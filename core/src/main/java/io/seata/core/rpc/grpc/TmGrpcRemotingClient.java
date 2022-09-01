/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc.grpc;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.seata.common.ConfigurationKeys;
import io.seata.common.exception.FrameworkException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.RejectedPolicies;
import io.seata.common.util.NetUtil;
import io.seata.config.ConfigurationCache;
import io.seata.core.auth.AuthSigner;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.rpc.RpcChannelPoolKey;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.processor.MessageMeta;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.core.rpc.processor.client.ClientHeartbeatProcessor;
import io.seata.core.rpc.processor.client.ClientOnResponseProcessor;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.ConfigurationKeys.EXTRA_DATA_KV_CHAR;
import static io.seata.common.ConfigurationKeys.EXTRA_DATA_SPLIT_CHAR;
import static io.seata.common.ConfigurationKeys.SEATA_ACCESS_KEY;
import static io.seata.common.ConfigurationKeys.SEATA_SECRET_KEY;

/**
 * @author goodboycoder
 */
public class TmGrpcRemotingClient extends AbstractGrpcRemotingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TmGrpcRemotingClient.class);
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    private static volatile TmGrpcRemotingClient instance;

    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 2000;
    private String applicationId;
    private String transactionServiceGroup;

    private final AuthSigner signer;
    private String accessKey;
    private String secretKey;

    public TmGrpcRemotingClient(GrpcClientConfig clientConfig, ThreadPoolExecutor messageExecutor) {
        super(clientConfig, messageExecutor, RpcChannelPoolKey.TransactionRole.TMROLE);
        this.signer = EnhancedServiceLoader.load(AuthSigner.class);

        this.enableClientBatchSendRequest = clientConfig.isEnableTmClientBatchSendRequest();
        ConfigurationCache.addConfigListener(ConfigurationKeys.GRPC_ENABLE_TM_CLIENT_BATCH_SEND_REQUEST, event -> {
            String dataId = event.getDataId();
            String newValue = event.getNewValue();
            if (ConfigurationKeys.GRPC_ENABLE_TM_CLIENT_BATCH_SEND_REQUEST.equals(dataId) && StringUtils.isNotBlank(newValue)) {
                enableClientBatchSendRequest = Boolean.parseBoolean(newValue);
            }
        });
    }

    @Override
    public void init() {
        // registry processor
        registerProcessor();
        if (initialized.compareAndSet(false, true)) {
            super.init();
            if (io.seata.common.util.StringUtils.isNotBlank(transactionServiceGroup)) {
                getClientChannelManager().reconnect(transactionServiceGroup);
            }
        }
    }

    @Override
    protected Function<String, RpcChannelPoolKey> getPoolKeyFunction() {
        return severAddress -> {
            RegisterTMRequest message = new RegisterTMRequest(applicationId, transactionServiceGroup, getExtraData());
            return new RpcChannelPoolKey(RpcChannelPoolKey.TransactionRole.TMROLE, severAddress, message);
        };
    }

    private String getExtraData() {
        String ip = NetUtil.getLocalIp();
        String timestamp = String.valueOf(System.currentTimeMillis());
        String digestSource;
        if (StringUtils.isEmpty(ip)) {
            digestSource = transactionServiceGroup + ",127.0.0.1," + timestamp;
        } else {
            digestSource = transactionServiceGroup + "," + ip + "," + timestamp;
        }
        String digest = signer.sign(digestSource, secretKey);
        StringBuilder sb = new StringBuilder();
        sb.append(RegisterTMRequest.UDATA_AK).append(EXTRA_DATA_KV_CHAR).append(accessKey).append(EXTRA_DATA_SPLIT_CHAR);
        sb.append(RegisterTMRequest.UDATA_DIGEST).append(EXTRA_DATA_KV_CHAR).append(digest).append(EXTRA_DATA_SPLIT_CHAR);
        sb.append(RegisterTMRequest.UDATA_TIMESTAMP).append(EXTRA_DATA_KV_CHAR).append(timestamp).append(EXTRA_DATA_SPLIT_CHAR);
        sb.append(RegisterTMRequest.UDATA_AUTH_VERSION).append(EXTRA_DATA_KV_CHAR).append(signer.getSignVersion()).append(EXTRA_DATA_SPLIT_CHAR);
        return sb.toString();
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @return the instance
     */
    public static TmGrpcRemotingClient getInstance(String applicationId, String transactionServiceGroup) {
        return getInstance(applicationId, transactionServiceGroup, null, null);
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @param accessKey               the access key
     * @param secretKey               the secret key
     * @return the instance
     */
    public static TmGrpcRemotingClient getInstance(String applicationId, String transactionServiceGroup, String accessKey, String secretKey) {
        TmGrpcRemotingClient tmRpcClient = getInstance();
        tmRpcClient.setApplicationId(applicationId);
        tmRpcClient.setTransactionServiceGroup(transactionServiceGroup);
        tmRpcClient.setAccessKey(accessKey);
        tmRpcClient.setSecretKey(secretKey);
        return tmRpcClient;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static TmGrpcRemotingClient getInstance() {
        if (instance == null) {
            synchronized (TmGrpcRemotingClient.class) {
                if (instance == null) {
                    GrpcClientConfig grpcClientConfig = new GrpcClientConfig();
                    final ThreadPoolExecutor messageExecutor = new ThreadPoolExecutor(
                            grpcClientConfig.getClientWorkerThreads(), grpcClientConfig.getClientWorkerThreads(),
                            KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                            new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                            new NamedThreadFactory(grpcClientConfig.getTmDispatchThreadPrefix(),
                                    grpcClientConfig.getClientWorkerThreads()),
                            RejectedPolicies.runsOldestTaskPolicy());
                    instance = new TmGrpcRemotingClient(grpcClientConfig, messageExecutor);
                }
            }
        }
        return instance;
    }

    private void registerProcessor() {
        // 1.registry TC response processor
        ClientOnResponseProcessor onResponseProcessor =
                new ClientOnResponseProcessor(mergeMsgMap, super.getFutures(), getTransactionMessageHandler());
        super.registerProcessor(MessageType.TYPE_SEATA_MERGE_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_BEGIN_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_COMMIT_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_REPORT_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_ROLLBACK_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_STATUS_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_REG_CLT_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_BATCH_RESULT_MSG, onResponseProcessor, null);
        // 2.registry heartbeat message processor
        ClientHeartbeatProcessor clientHeartbeatProcessor = new ClientHeartbeatProcessor();
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, clientHeartbeatProcessor, null);
    }

    /**
     * Sets application id.
     *
     * @param applicationId the application id
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Sets transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    /**
     * Sets access key.
     *
     * @param accessKey the access key
     */
    protected void setAccessKey(String accessKey) {
        if (null != accessKey) {
            this.accessKey = accessKey;
            return;
        }
        this.accessKey = System.getProperty(SEATA_ACCESS_KEY);
    }

    /**
     * Sets secret key.
     *
     * @param secretKey the secret key
     */
    protected void setSecretKey(String secretKey) {
        if (null != secretKey) {
            this.secretKey = secretKey;
            return;
        }
        this.secretKey = System.getProperty(SEATA_SECRET_KEY);
    }

    @Override
    protected String getTransactionServiceGroup() {
        return this.transactionServiceGroup;
    }

    @Override
    protected boolean isEnableClientBatchSendRequest() {
        return false;
    }

    @Override
    protected long getRpcRequestTimeout() {
        return GrpcClientConfig.getRpcTmRequestTimeout();
    }

    @Override
    public void onRegisterMsgSuccess(String serverAddress, SeataChannel channel, Object response, AbstractMessage requestMessage) {
        RegisterTMRequest registerTMRequest = (RegisterTMRequest) requestMessage;
        RegisterTMResponse registerTMResponse = (RegisterTMResponse) response;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register TM success. client version:{}, server version:{},channel:{}", registerTMRequest.getVersion(), registerTMResponse.getVersion(), channel);
        }
        getClientChannelManager().registerChannel(serverAddress, channel);
    }

    @Override
    public void onRegisterMsgFail(String serverAddress, SeataChannel channel, Object response, AbstractMessage requestMessage) {
        RegisterTMRequest registerTMRequest = (RegisterTMRequest) requestMessage;
        RegisterTMResponse registerTMResponse = (RegisterTMResponse) response;
        String errMsg = String.format(
                "register TM failed. client version: %s,server version: %s, errorMsg: %s, " + "channel: %s", registerTMRequest.getVersion(), registerTMResponse.getVersion(), registerTMResponse.getMsg(), channel);
        throw new FrameworkException(errMsg);
    }

    @Override
    public StreamObserver<GrpcRemoting.BiStreamMessage> bindBiStream(SeataChannel channel) {
        ConcurrentHashMap<Integer, MessageFuture> futures = getFutures();
        return io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.newStub((Channel) channel.originChannel()).registerTM(new StreamObserver<GrpcRemoting.BiStreamMessage>() {
            @Override
            public void onNext(GrpcRemoting.BiStreamMessage message) {
                int messageId = message.getID();
                GrpcRemoting.BiStreamMessageType messageType = message.getMessageType();
                Any body = message.getMessage();
                if (GrpcRemoting.BiStreamMessageType.TYPERegisterTMResponse == messageType) {
                    io.seata.serializer.protobuf.generated.RegisterTMResponseProto registerTMResponseProto;
                    try {
                        registerTMResponseProto = body.unpack(io.seata.serializer.protobuf.generated.RegisterTMResponseProto.class);
                    } catch (InvalidProtocolBufferException e) {
                        throw new RuntimeException(e);
                    }
                    MessageFuture messageFuture = futures.get(messageId);
                    if (null != messageFuture) {
                        messageFuture.setResultMessage(ProtoTypeConvertHelper.convertToModel(registerTMResponseProto));
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
                    RpcMessageHandleContext handleContext = new RpcMessageHandleContext(channel);
                    MessageMeta messageMeta = new MessageMeta();
                    messageMeta.setMessageId(messageId);
                    handleContext.setMessageMeta(messageMeta);
                    handleContext.setMessageReply(response -> sendAsyncResponse(NetUtil.toStringAddress(channel.remoteAddress()),
                            handleContext.getMessageMeta(), response));
                    processMessage(handleContext, modelRequest);
                }
            }

            @Override
            public void onError(Throwable throwable) {
                LOGGER.warn("TM Request stream error: {}. Client channel was cancelled and the stream will be closed", throwable.getMessage());
                channel.close();
            }

            @Override
            public void onCompleted() {
                LOGGER.info("TM Request stream onCompleted. The stream will be closed after server message to be sent completely");
                //it will wait for the server message to be sent completely
                channel.close();
            }
        });
    }
}
