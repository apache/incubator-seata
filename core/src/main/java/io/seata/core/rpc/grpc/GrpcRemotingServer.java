package io.seata.core.rpc.grpc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.util.MutableHandlerRegistry;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingBootstrap;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.SeataChannelServerManager;
import io.seata.core.rpc.ShutdownHook;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.grpc.interceptor.ServerChannelInterceptor;
import io.seata.core.rpc.grpc.service.HealthCheckService;
import io.seata.core.rpc.grpc.service.ResourceManagerService;
import io.seata.core.rpc.grpc.service.TransactionManagerService;
import io.seata.core.rpc.processor.Pair;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.core.rpc.processor.server.BranchRegisterProcessor;
import io.seata.core.rpc.processor.server.BranchReportProcessor;
import io.seata.core.rpc.processor.server.GlobalBeginProcessor;
import io.seata.core.rpc.processor.server.GlobalCommitProcessor;
import io.seata.core.rpc.processor.server.GlobalLockQueryProcessor;
import io.seata.core.rpc.processor.server.GlobalReportProcessor;
import io.seata.core.rpc.processor.server.GlobalRollbackProcessor;
import io.seata.core.rpc.processor.server.GlobalStatusProcessor;
import io.seata.core.rpc.processor.server.MergedWarpMessageProcessor;
import io.seata.core.rpc.processor.server.RegRmProcessor;
import io.seata.core.rpc.processor.server.RegTmProcessor;
import io.seata.core.rpc.processor.server.ServerHeartbeatProcessor;
import io.seata.core.rpc.processor.server.ServerOnResponseProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcRemotingServer extends AbstractGrpcRemoting implements RemotingServer, Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRemotingServer.class);

    private final RemotingBootstrap grpcServerBootstrap;
    private final MutableHandlerRegistry mutableHandlerRegistry;
    private final List<ServerInterceptor> serverInterceptors = new CopyOnWriteArrayList<>();
    private TransactionMessageHandler transactionMessageHandler;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private final ThreadPoolExecutor branchResultMessageExecutor = new ThreadPoolExecutor(GrpcServerConfig.getMinBranchResultPoolSize(),
            GrpcServerConfig.getMaxBranchResultPoolSize(), GrpcServerConfig.getKeepAliveTime(), TimeUnit.SECONDS,
            new LinkedBlockingQueue<>(GrpcServerConfig.getMaxTaskQueueSize()),
            new NamedThreadFactory("GrpcBranchResultHandlerThread", GrpcServerConfig.getMaxBranchResultPoolSize()), new ThreadPoolExecutor.CallerRunsPolicy());

    public GrpcRemotingServer(ThreadPoolExecutor messageExecutor) {
        super(messageExecutor);
        this.mutableHandlerRegistry = new MutableHandlerRegistry();
        this.grpcServerBootstrap = new GrpcServerBootstrap(this.mutableHandlerRegistry, new GrpcServerConfig());

        SeataChannelServerManager.register(RpcType.GRPC, new GrpcServerChannelManager());
    }

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            super.init();
            //register server interceptor(must before the service register)
            registerDefaultInterceptor();
            //register default service
            registerDefaultService();
            //register default remoting processor
            registerProcessor();
            grpcServerBootstrap.start();
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        grpcServerBootstrap.shutdown();
        branchResultMessageExecutor.shutdown();
    }

    public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {
        Pair<RemotingProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(messageType, pair);
    }


    private void registerDefaultInterceptor() {
        this.serverInterceptors.add(new ServerChannelInterceptor());
    }

    private void registerDefaultService() {
        mutableHandlerRegistry.addService(ServerInterceptors.intercept(new TransactionManagerService(this), serverInterceptors));
        mutableHandlerRegistry.addService(ServerInterceptors.intercept(new ResourceManagerService(this), serverInterceptors));
        mutableHandlerRegistry.addService(ServerInterceptors.intercept(new HealthCheckService(this), serverInterceptors));
    }

    private void registerProcessor() {
        // 1. registry on request message processor
        registerProcessor(MessageType.TYPE_BRANCH_REGISTER, new BranchRegisterProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT, new BranchReportProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_BEGIN, new GlobalBeginProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_COMMIT, new GlobalCommitProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY, new GlobalLockQueryProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_REPORT, new GlobalReportProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK, new GlobalRollbackProcessor(getHandler()), messageExecutor);
        registerProcessor(MessageType.TYPE_GLOBAL_STATUS, new GlobalStatusProcessor(getHandler()), messageExecutor);
        MergedWarpMessageProcessor mergedWarpMessageProcessor =
                new MergedWarpMessageProcessor(this, getHandler());
        ShutdownHook.getInstance().addDisposable(mergedWarpMessageProcessor);
        registerProcessor(MessageType.TYPE_SEATA_MERGE, mergedWarpMessageProcessor, messageExecutor);

        // 2. registry on response message processor
        ServerOnResponseProcessor onResponseProcessor =
                new ServerOnResponseProcessor(getHandler(), getFutures());
        registerProcessor(MessageType.TYPE_BRANCH_COMMIT_RESULT, onResponseProcessor, branchResultMessageExecutor);
        registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK_RESULT, onResponseProcessor, branchResultMessageExecutor);

        // 3. registry rm message processor
        RegRmProcessor regRmProcessor = new RegRmProcessor(this);
        registerProcessor(MessageType.TYPE_REG_RM, regRmProcessor, messageExecutor);

        // 4. registry tm message processor
        RegTmProcessor regTmProcessor = new RegTmProcessor(this);
        registerProcessor(MessageType.TYPE_REG_CLT, regTmProcessor, null);

        // 5. registry heartbeat message processor
        ServerHeartbeatProcessor heartbeatMessageProcessor = new ServerHeartbeatProcessor(this);
        registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, heartbeatMessageProcessor, null);
    }

    /**
     * Sets transactionMessageHandler.
     *
     * @param transactionMessageHandler the transactionMessageHandler
     */
    public void setHandler(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    public TransactionMessageHandler getHandler() {
        return transactionMessageHandler;
    }

    @Override
    public Object sendSyncRequest(String resourceId, String clientId, Object msg) throws TimeoutException {
        SeataChannel channel = SeataChannelServerManager.getServerManager(RpcType.GRPC).getChannel(resourceId, clientId);
        if (channel == null || RpcType.GRPC != channel.getType()) {
            throw new RuntimeException("rm client is not connected. dbkey:" + resourceId + ",clientId:" + clientId);
        }
        return super.sendSync(channel, buildRpcMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC), GrpcServerConfig.getRpcRequestTimeout());
    }

    @Override
    public Object sendSyncRequest(SeataChannel channel, Object msg) throws TimeoutException {
        if (channel == null || RpcType.GRPC != channel.getType()) {
            throw new RuntimeException("client is not connected");
        }
        return super.sendSync(channel, buildRpcMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC), GrpcServerConfig.getRpcRequestTimeout());
    }

    @Override
    public void sendAsyncRequest(SeataChannel channel, Object msg) {
        if (channel == null || RpcType.GRPC != channel.getType()) {
            throw new RuntimeException("client is not connected");
        }
        GrpcSeataChannel grpcSeataChannel = (GrpcSeataChannel) channel;
        super.sendAsync(grpcSeataChannel, buildRpcMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY));
    }

    @Override
    public void sendAsyncResponse(RpcMessage rpcMessage, SeataChannel channel, Object msg) {
        //do nothing
    }

    @Override
    protected Object doSyncSend(SeataChannel channel, RpcMessage rpcMessage, long timeoutMillis) throws Exception {
        Object messageBody = rpcMessage.getBody();

        Message protoRequest = ProtoTypeConvertHelper.convertToProto(messageBody);
        io.seata.core.rpc.grpc.generated.GrpcRemoting.BiStreamMessageType biStreamMessageType = BiStreamMessageTypeHelper.getBiStreamMessageTypeByClass(protoRequest.getClass());
        if (null == biStreamMessageType) {
            LOGGER.warn("not supported message type: {}", messageBody.getClass());
            throw new IllegalArgumentException("not supported message type" + messageBody.getClass());
        }

        io.seata.core.rpc.grpc.generated.GrpcRemoting.BiStreamMessage biStreamMessage = io.seata.core.rpc.grpc.generated.GrpcRemoting.BiStreamMessage.newBuilder()
                .setID(rpcMessage.getId())
                .setMessageType(biStreamMessageType)
                .setMessage(Any.pack(protoRequest))
                .build();

        MessageFuture messageFuture = new MessageFuture();
        messageFuture.setRequestMessage(rpcMessage);
        messageFuture.setTimeout(timeoutMillis);
        futures.put(rpcMessage.getId(), messageFuture);
        try {
            channel.sendMsg(biStreamMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Object response = messageFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        if (response instanceof Message) {
            response = ProtoTypeConvertHelper.convertToModel((Message) response);
        }
        return response;
    }

    @Override
    protected void doAsyncSend(SeataChannel channel, RpcMessage rpcMessage) {
        Object messageBody = rpcMessage.getBody();

        Message protoRequest = ProtoTypeConvertHelper.convertToProto(messageBody);
        //Try bidirectional streaming
        io.seata.core.rpc.grpc.generated.GrpcRemoting.BiStreamMessageType biStreamMessageType = BiStreamMessageTypeHelper.getBiStreamMessageTypeByClass(protoRequest.getClass());
        if (null == biStreamMessageType) {
            LOGGER.warn("not supported message type: {}", messageBody.getClass());
            throw new IllegalArgumentException("not supported message type" + messageBody.getClass());
        }
        io.seata.core.rpc.grpc.generated.GrpcRemoting.BiStreamMessage biStreamMessage = io.seata.core.rpc.grpc.generated.GrpcRemoting.BiStreamMessage.newBuilder()
                .setID(rpcMessage.getId())
                .setMessageType(biStreamMessageType)
                .setMessage(Any.pack(protoRequest))
                .build();
        try {
            channel.sendMsg(biStreamMessage);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private RpcMessage buildRpcMessage(Object msg, byte messageType) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(getNextMessageId());
        rpcMessage.setMessageType(messageType);
        rpcMessage.setBody(msg);
        return rpcMessage;
    }
}
