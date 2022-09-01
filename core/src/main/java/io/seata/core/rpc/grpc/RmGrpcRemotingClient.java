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

import java.util.Map;
import java.util.Set;
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
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationCache;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.rpc.RmRemotingClient;
import io.seata.core.rpc.RpcChannelPoolKey;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.processor.MessageMeta;
import io.seata.core.rpc.processor.RpcMessageHandleContext;
import io.seata.core.rpc.processor.client.ClientHeartbeatProcessor;
import io.seata.core.rpc.processor.client.ClientOnResponseProcessor;
import io.seata.core.rpc.processor.client.RmBranchCommitProcessor;
import io.seata.core.rpc.processor.client.RmBranchRollbackProcessor;
import io.seata.core.rpc.processor.client.RmUndoLogProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.Constants.DBKEYS_SPLIT_CHAR;

/**
 * @author goodboycoder
 */
public class RmGrpcRemotingClient extends AbstractGrpcRemotingClient implements RmRemotingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmGrpcRemotingClient.class);
    private ResourceManager resourceManager;
    private static volatile RmGrpcRemotingClient instance;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 20000;
    private String applicationId;
    private String transactionServiceGroup;

    public RmGrpcRemotingClient(GrpcClientConfig clientConfig, ThreadPoolExecutor messageExecutor) {
        super(clientConfig, messageExecutor, RpcChannelPoolKey.TransactionRole.RMROLE);

        this.enableClientBatchSendRequest = clientConfig.isEnableRmClientBatchSendRequest();
        ConfigurationCache.addConfigListener(ConfigurationKeys.GRPC_ENABLE_RM_CLIENT_BATCH_SEND_REQUEST, event -> {
            String dataId = event.getDataId();
            String newValue = event.getNewValue();
            if (ConfigurationKeys.GRPC_ENABLE_RM_CLIENT_BATCH_SEND_REQUEST.equals(dataId) && StringUtils.isNotBlank(newValue)) {
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

            // Found one or more resources that were registered before initialization
            if (resourceManager != null
                    && !resourceManager.getManagedResources().isEmpty()
                    && StringUtils.isNotBlank(transactionServiceGroup)) {
                getClientChannelManager().reconnect(transactionServiceGroup);
            }
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        initialized.getAndSet(false);
        instance = null;
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @return the instance
     */
    public static RmGrpcRemotingClient getInstance(String applicationId, String transactionServiceGroup) {
        RmGrpcRemotingClient rmNettyRemotingClient = getInstance();
        rmNettyRemotingClient.setApplicationId(applicationId);
        rmNettyRemotingClient.setTransactionServiceGroup(transactionServiceGroup);
        return rmNettyRemotingClient;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static RmGrpcRemotingClient getInstance() {
        if (instance == null) {
            synchronized (RmGrpcRemotingClient.class) {
                if (instance == null) {
                    GrpcClientConfig grpcClientConfig = new GrpcClientConfig();
                    final ThreadPoolExecutor messageExecutor = new ThreadPoolExecutor(
                            grpcClientConfig.getClientWorkerThreads(), grpcClientConfig.getClientWorkerThreads(),
                            KEEP_ALIVE_TIME, TimeUnit.SECONDS, new LinkedBlockingQueue<>(MAX_QUEUE_SIZE),
                            new NamedThreadFactory(grpcClientConfig.getRmDispatchThreadPrefix(),
                                    grpcClientConfig.getClientWorkerThreads()), new ThreadPoolExecutor.CallerRunsPolicy());
                    instance = new RmGrpcRemotingClient(grpcClientConfig, messageExecutor);
                }
            }
        }
        return instance;
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
     * Sets resource manager.
     *
     * @param resourceManager the resource manager
     */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    @Override
    protected Function<String, RpcChannelPoolKey> getPoolKeyFunction() {
        return serverAddress -> {
            String resourceIds = getMergedResourceKeys();
            if (resourceIds != null && LOGGER.isInfoEnabled()) {
                LOGGER.info("RM will register :{}", resourceIds);
            }
            RegisterRMRequest message = new RegisterRMRequest(applicationId, transactionServiceGroup);
            message.setResourceIds(resourceIds);
            return new RpcChannelPoolKey(RpcChannelPoolKey.TransactionRole.RMROLE, serverAddress, message);
        };
    }

    @Override
    protected String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    @Override
    protected boolean isEnableClientBatchSendRequest() {
        return false;
    }

    @Override
    protected long getRpcRequestTimeout() {
        return GrpcClientConfig.getRpcRmRequestTimeout();
    }

    @Override
    public void onRegisterMsgSuccess(String serverAddress, SeataChannel channel, Object response, AbstractMessage requestMessage) {
        RegisterRMRequest registerRMRequest = (RegisterRMRequest) requestMessage;
        RegisterRMResponse registerRMResponse = (RegisterRMResponse) response;
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register RM success. client version:{}, server version:{},channel:{}", registerRMRequest.getVersion(), registerRMResponse.getVersion(), channel);
        }
        getClientChannelManager().registerChannel(serverAddress, channel);
        String dbKey = getMergedResourceKeys();
        if (registerRMRequest.getResourceIds() != null) {
            if (!registerRMRequest.getResourceIds().equals(dbKey)) {
                sendRegisterMessage(serverAddress, channel, dbKey);
            }
        }
    }

    @Override
    public void onRegisterMsgFail(String serverAddress, SeataChannel channel, Object response, AbstractMessage requestMessage) {
        RegisterRMRequest registerRMRequest = (RegisterRMRequest) requestMessage;
        RegisterRMResponse registerRMResponse = (RegisterRMResponse) response;
        String errMsg = String.format(
                "register RM failed. client version: %s,server version: %s, errorMsg: %s, " + "channel: %s", registerRMRequest.getVersion(), registerRMResponse.getVersion(), registerRMResponse.getMsg(), channel);
        throw new FrameworkException(errMsg);
    }

    @Override
    public StreamObserver<GrpcRemoting.BiStreamMessage> bindBiStream(SeataChannel channel) {
        ConcurrentHashMap<Integer, MessageFuture> futures = getFutures();
        return io.seata.core.rpc.grpc.generated.ResourceManagerServiceGrpc.newStub((Channel) channel.originChannel()).registerRM(new StreamObserver<GrpcRemoting.BiStreamMessage>() {
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
                LOGGER.error("RM Request stream on error: {}. Client channel was cancelled and the stream will be closed", throwable.toString());
                channel.close();
            }

            @Override
            public void onCompleted() {
                LOGGER.info("RM Request stream on completed. The stream will be closed after server message to be sent completely");
                //it will wait for the server message to be sent completely
                channel.close();
            }
        });
    }

    private void registerProcessor() {
        // 1.registry rm client handle branch commit processor
        RmBranchCommitProcessor rmBranchCommitProcessor = new RmBranchCommitProcessor(getTransactionMessageHandler(), this);
        super.registerProcessor(MessageType.TYPE_BRANCH_COMMIT, rmBranchCommitProcessor, messageExecutor);
        // 2.registry rm client handle branch rollback processor
        RmBranchRollbackProcessor rmBranchRollbackProcessor = new RmBranchRollbackProcessor(getTransactionMessageHandler(), this);
        super.registerProcessor(MessageType.TYPE_BRANCH_ROLLBACK, rmBranchRollbackProcessor, messageExecutor);
        // 3.registry rm handler undo log processor
        RmUndoLogProcessor rmUndoLogProcessor = new RmUndoLogProcessor(getTransactionMessageHandler());
        super.registerProcessor(MessageType.TYPE_RM_DELETE_UNDOLOG, rmUndoLogProcessor, messageExecutor);
        // 4.registry TC response processor
        ClientOnResponseProcessor onResponseProcessor =
                new ClientOnResponseProcessor(mergeMsgMap, super.getFutures(), getTransactionMessageHandler());
        super.registerProcessor(MessageType.TYPE_SEATA_MERGE_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_BRANCH_REGISTER_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_BRANCH_STATUS_REPORT_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_GLOBAL_LOCK_QUERY_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_REG_RM_RESULT, onResponseProcessor, null);
        super.registerProcessor(MessageType.TYPE_BATCH_RESULT_MSG, onResponseProcessor, null);
        // 5.registry heartbeat message processor
        ClientHeartbeatProcessor clientHeartbeatProcessor = new ClientHeartbeatProcessor();
        super.registerProcessor(MessageType.TYPE_HEARTBEAT_MSG, clientHeartbeatProcessor, null);
    }

    /**
     * Register new db key.
     *
     * @param resourceGroupId the resource group id
     * @param resourceId      the db key
     */
    @Override
    public void registerResource(String resourceGroupId, String resourceId) {

        // Resource registration cannot be performed until the RM client is initialized
        if (StringUtils.isBlank(transactionServiceGroup)) {
            return;
        }

        if (getClientChannelManager().getChannels().isEmpty()) {
            getClientChannelManager().reconnect(transactionServiceGroup);
            return;
        }
        synchronized (getClientChannelManager().getChannels()) {
            for (Map.Entry<String, SeataChannel> entry : getClientChannelManager().getChannels().entrySet()) {
                String serverAddress = entry.getKey();
                SeataChannel rmChannel = entry.getValue();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("will register resourceId:{}", resourceId);
                }
                sendRegisterMessage(serverAddress, rmChannel, resourceId);
            }
        }
    }

    public void sendRegisterMessage(String serverAddress, SeataChannel channel, String resourceId) {
        RegisterRMRequest message = new RegisterRMRequest(applicationId, transactionServiceGroup);
        message.setResourceIds(resourceId);
        try {
            super.sendAsyncRequest(channel, message);
        } catch (FrameworkException e) {
            if (e.getErrcode() == FrameworkErrorCode.ChannelIsNotWritable && serverAddress != null) {
                getClientChannelManager().releaseChannel(channel, serverAddress);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("remove not writable channel:{}", channel);
                }
            } else {
                LOGGER.error("register resource failed, channel:{},resourceId:{}", channel, resourceId, e);
            }
        }
    }

    public String getMergedResourceKeys() {
        Map<String, Resource> managedResources = resourceManager.getManagedResources();
        Set<String> resourceIds = managedResources.keySet();
        if (!resourceIds.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String resourceId : resourceIds) {
                if (first) {
                    first = false;
                } else {
                    sb.append(DBKEYS_SPLIT_CHAR);
                }
                sb.append(resourceId);
            }
            return sb.toString();
        }
        return null;
    }
}
