package io.seata.core.rpc.grpc;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.grpc.generated.ResourceManagerServiceGrpc;
import io.seata.core.rpc.netty.NettyPoolKey;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcPoolableFactory implements KeyedPoolableObjectFactory<NettyPoolKey, SeataChannel> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcPoolableFactory.class);

    private final AbstractGrpcRemotingClient rpcRemotingClient;

    public GrpcPoolableFactory(AbstractGrpcRemotingClient rpcRemotingClient) {
        this.rpcRemotingClient = rpcRemotingClient;
    }

    @Override
    public SeataChannel makeObject(NettyPoolKey key) {
        InetSocketAddress address = NetUtil.toInetSocketAddress(key.getAddress());
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[GRPC]NettyPool create channel to " + key);
        }
        ManagedChannel managedChannel = ManagedChannelBuilder
                .forAddress(address.getHostName(), address.getPort())
                .directExecutor()
                .build();
        long start = System.currentTimeMillis();
        Object response;
        GrpcClientSeataChannel channelToServer = null;
        GrpcClientSeataChannel tmpChannel = new GrpcClientSeataChannel(managedChannel);
        if (key.getMessage() == null) {
            throw new FrameworkException("register msg is null, role:" + key.getTransactionRole().name());
        }

        if (NettyPoolKey.TransactionRole.RMROLE == key.getTransactionRole()) {
            ConcurrentHashMap<Integer, MessageFuture> futures = rpcRemotingClient.getFutures();
            StreamObserver<GrpcRemoting.BiStreamMessage> clientStreamObserver = ResourceManagerServiceGrpc.newStub(managedChannel).registerRM(new StreamObserver<GrpcRemoting.BiStreamMessage>() {
                @Override
                public void onNext(GrpcRemoting.BiStreamMessage message) {
                    int messageId = message.getID();
                    GrpcRemoting.BiStreamMessageType messageType = message.getMessageType();
                    Any body = message.getMessage();
                    if (GrpcRemoting.BiStreamMessageType.TYPERegisterRMResponse == messageType) {
                        io.seata.serializer.protobuf.generated.RegisterRMResponseProto registerRMResponseProto;
                        try {
                            registerRMResponseProto = body.unpack(io.seata.serializer.protobuf.generated.RegisterRMResponseProto.class);
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }
                        MessageFuture messageFuture = futures.get(messageId);
                        if (null != messageFuture) {
                            messageFuture.setResultMessage(ProtoTypeConvertHelper.convertToModel(registerRMResponseProto));
                        }
                    } else {
                        Message unpackMessage;
                        try {
                            unpackMessage = body.unpack(BiStreamMessageTypeHelper.getBiStreamMessageClassType(messageType));
                        } catch (InvalidProtocolBufferException e) {
                            throw new RuntimeException(e);
                        }
                        Object modelRequest = ProtoTypeConvertHelper.convertToModel(unpackMessage);
                        //handle request
                        RpcMessageHandleContext handleContext = new RpcMessageHandleContext(tmpChannel);
                        handleContext.setMessageReply(response -> rpcRemotingClient.sendAsyncResponse(NetUtil.toStringAddress(tmpChannel.remoteAddress()),
                                handleContext.getMessageMeta(), response));
                        rpcRemotingClient.processMessage(handleContext, modelRequest);
                    }
                }

                @Override
                public void onError(Throwable throwable) {
                    //TODO client streamObserver on error
                    LOGGER.warn("Request stream error: {}", throwable.toString());
                }

                @Override
                public void onCompleted() {
                    //TODO client streamObserver on completed
                    LOGGER.info("Request stream onCompleted");
                }
            });
            tmpChannel.setStreamObserver(clientStreamObserver);
        }

        try {
            response = rpcRemotingClient.sendSyncRequest(key.getMessage());
            if (!isRegisterSuccess(response, key.getTransactionRole())) {
                rpcRemotingClient.onRegisterMsgFail(key.getAddress(), tmpChannel, response, key.getMessage());
            } else {
                channelToServer = tmpChannel;
                rpcRemotingClient.onRegisterMsgSuccess(key.getAddress(), tmpChannel, response, key.getMessage());
            }
        } catch (Exception exx) {
            tmpChannel.close();
            throw new FrameworkException(
                    "register " + key.getTransactionRole().name() + " error, errMsg:" + exx.getMessage());
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register success, cost " + (System.currentTimeMillis() - start) + " ms, version:" + getVersion(
                    response, key.getTransactionRole()) + ",role:" + key.getTransactionRole().name() + ",channel:"
                    + channelToServer);
        }
        return channelToServer;
    }

    private boolean isRegisterSuccess(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (response == null) {
            return false;
        }
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            if (!(response instanceof RegisterTMResponse)) {
                return false;
            }
            RegisterTMResponse registerTMResponse = (RegisterTMResponse) response;
            return registerTMResponse.isIdentified();
        } else if (transactionRole.equals(NettyPoolKey.TransactionRole.RMROLE)) {
            if (!(response instanceof RegisterRMResponse)) {
                return false;
            }
            RegisterRMResponse registerRMResponse = (RegisterRMResponse) response;
            return registerRMResponse.isIdentified();
        }
        return false;
    }

    private String getVersion(Object response, NettyPoolKey.TransactionRole transactionRole) {
        if (transactionRole.equals(NettyPoolKey.TransactionRole.TMROLE)) {
            return ((RegisterTMResponse) response).getVersion();
        } else {
            return ((RegisterRMResponse) response).getVersion();
        }
    }

    @Override
    public void destroyObject(NettyPoolKey key, SeataChannel channel) {
        if (channel != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("will destroy channel:" + channel);
            }
            channel.disconnect();
            channel.close();
        }
    }

    @Override
    public boolean validateObject(NettyPoolKey key, SeataChannel obj) {
        if (obj != null && obj.isActive()) {
            return true;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel valid false,channel:" + obj);
        }
        return false;
    }

    @Override
    public void activateObject(NettyPoolKey key, SeataChannel obj) {

    }

    @Override
    public void passivateObject(NettyPoolKey key, SeataChannel obj) {

    }
}
