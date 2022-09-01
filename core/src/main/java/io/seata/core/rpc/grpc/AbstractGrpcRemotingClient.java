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

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.Any;
import com.google.protobuf.Message;
import io.seata.common.exception.FrameworkException;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.MessageType;
import io.seata.core.protocol.MessageTypeAware;
import io.seata.core.protocol.ProtocolConstants;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.AbstractGlobalEndRequest;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.rpc.RemotingClient;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;
import io.seata.core.rpc.grpc.generated.ResourceManagerServiceGrpc;
import io.seata.core.rpc.RpcChannelPoolKey;
import io.seata.core.rpc.processor.MessageMeta;
import io.seata.core.rpc.processor.Pair;
import io.seata.core.rpc.processor.RemotingProcessor;
import io.seata.discovery.loadbalance.LoadBalanceFactory;
import io.seata.discovery.registry.RegistryFactory;
import io.seata.serializer.protobuf.generated.BranchRegisterResponseProto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.exception.FrameworkErrorCode.NoAvailableService;

/**
 * @author goodboycoder
 */
public abstract class AbstractGrpcRemotingClient extends AbstractGrpcRemoting implements RemotingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGrpcRemotingClient.class);

    private final RpcChannelPoolKey.TransactionRole transactionRole;
    private final GrpcClientChannelManager clientChannelManager;
    private TransactionMessageHandler transactionMessageHandler;

    private static final long SCHEDULE_DELAY_MILLS = 60 * 1000L;
    private static final long SCHEDULE_INTERVAL_MILLS = 10 * 1000L;

    /**
     * merge message send setting
     */
    private static final String MSG_ID_PREFIX = "msgId:";
    private static final String FUTURES_PREFIX = "futures:";
    private static final String SINGLE_LOG_POSTFIX = ";";
    private static final int MAX_MERGE_SEND_MILLS = 1;
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    private static final int MAX_MERGE_SEND_THREAD = 1;
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageSend";

    protected final Object mergeLock = new Object();
    protected volatile boolean enableClientBatchSendRequest;
    private ExecutorService mergeSendExecutorService;
    /**
     * When sending message type is {@link MergeMessage}, will be stored to mergeMsgMap.
     */
    protected final Map<Integer, MergeMessage> mergeMsgMap = new ConcurrentHashMap<>();

    /**
     * When batch sending is enabled, the message will be stored to basketMap
     * Send via asynchronous thread
     */
    protected final ConcurrentHashMap<String/*serverAddress*/, BlockingQueue<RpcMessage>> basketMap = new ConcurrentHashMap<>();

    private static final Map<Short, GrpcStubFunction> STUB_FUNCTION_MAP = new ConcurrentHashMap<>();

    public AbstractGrpcRemotingClient(GrpcClientConfig clientConfig, ThreadPoolExecutor messageExecutor, RpcChannelPoolKey.TransactionRole transactionRole) {
        super(messageExecutor);
        this.transactionRole = transactionRole;
        this.clientChannelManager = new GrpcClientChannelManager(new GrpcPoolableFactory(this), getPoolKeyFunction(), clientConfig);
    }

    @Override
    public void init() {
        super.init();
        registerDefaultClientStub();
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                clientChannelManager.reconnect(getTransactionServiceGroup());
            }
        }, SCHEDULE_DELAY_MILLS, SCHEDULE_INTERVAL_MILLS, TimeUnit.MILLISECONDS);
    }

    @Override
    public void destroy() {
        if (mergeSendExecutorService != null) {
            mergeSendExecutorService.shutdown();
        }
        clientChannelManager.closeAllChannel();
        super.destroy();
    }

    public TransactionMessageHandler getTransactionMessageHandler() {
        return transactionMessageHandler;
    }

    public void setTransactionMessageHandler(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    public RpcChannelPoolKey.TransactionRole getTransactionRole() {
        return transactionRole;
    }

    public GrpcClientChannelManager getClientChannelManager() {
        return clientChannelManager;
    }


    @Override
    public Object sendSyncRequest(Object msg) throws TimeoutException {
        String serverAddress = loadBalance(getTransactionServiceGroup(), msg);
        long timeoutMillis = this.getRpcRequestTimeout();
        if (!(msg instanceof MessageTypeAware)) {
            LOGGER.warn("[GRPC]not supported request to send:{}", msg);
            return null;
        }

        if (this.isEnableClientBatchSendRequest()) {
            throw new NotSupportYetException("batch message send is not yet supported now");
        } else {
            SeataChannel channel = clientChannelManager.acquireChannel(serverAddress);
            return super.sendSync(channel, buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC), timeoutMillis);
        }
    }

    /**
     * client send sync request.
     *
     * @param channel client channel
     * @param msg     transaction message {@link io.seata.core.protocol}
     * @return server result message
     * @throws TimeoutException TimeoutException
     */
    public Object sendSyncRequest(SeataChannel channel, Object msg) throws TimeoutException {
        if (channel == null) {
            LOGGER.warn("[GRPC]sendSyncRequest nothing, caused by null channel.");
            return null;
        }
        RpcMessage rpcMessage = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        return super.sendSync(channel, rpcMessage, this.getRpcRequestTimeout());
    }

    /**
     * client send async request.
     *
     * @param channel client channel
     * @param msg     transaction message {@link io.seata.core.protocol}
     */
    public void sendAsyncRequest(SeataChannel channel, Object msg) {
        if (channel == null) {
            LOGGER.warn("sendAsyncRequest nothing, caused by null channel.");
            return;
        }
        RpcMessage rpcMessage = buildRequestMessage(msg, msg instanceof HeartbeatMessage
                ? ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
                : ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        super.sendAsync(channel, rpcMessage);
    }

    /**
     * client send async response.
     *
     * @param serverAddress server address
     * @param requestMeta   rpc message requestMeta from server request
     * @param msg           transaction message {@link io.seata.core.protocol}
     */
    public void sendAsyncResponse(String serverAddress, MessageMeta requestMeta, Object msg) {
        //build Rpc message
        RpcMessage message = buildResponseMessage(requestMeta, msg, ProtocolConstants.MSGTYPE_RESPONSE);
        SeataChannel channel = clientChannelManager.acquireChannel(serverAddress);
        try {
            super.sendAsync(channel, message);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    protected Object doSyncSend(SeataChannel channel, RpcMessage rpcMessage, long timeoutMillis) throws Exception {
        Object messageBody = rpcMessage.getBody();
        MessageTypeAware messageTypeAware = (MessageTypeAware) messageBody;

        GrpcStubFunction<Message, Message> stubFunction = STUB_FUNCTION_MAP.get(messageTypeAware.getTypeCode());
        Message protoRequest = ProtoTypeConvertHelper.convertToProto(messageBody);
        if (null == stubFunction) {
            //Try bidirectional streaming
            GrpcRemoting.BiStreamMessageType biStreamMessageType = BiStreamMessageTypeHelper.getBiStreamMessageTypeByClass(protoRequest.getClass());
            if (null == biStreamMessageType) {
                LOGGER.error("[GRPC]can not find useful stub for request type:{}", messageTypeAware.getTypeCode());
                throw new IllegalArgumentException("can not find a useful stub for the request type" + messageTypeAware.getTypeCode());
            }
            GrpcRemoting.BiStreamMessage biStreamMessage = GrpcRemoting.BiStreamMessage.newBuilder()
                    .setID(rpcMessage.getId())
                    .setMessageType(biStreamMessageType)
                    .setMessage(Any.pack(protoRequest))
                    .build();
            try {
                channel.sendMsg(biStreamMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            MessageFuture messageFuture = new MessageFuture();
            messageFuture.setRequestMessage(rpcMessage);
            messageFuture.setTimeout(timeoutMillis);
            getFutures().put(rpcMessage.getId(), messageFuture);
            return messageFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
        } else {
            ListenableFuture<Message> future = stubFunction.apply(channel, protoRequest);
            return ProtoTypeConvertHelper.convertToModel(future.get(timeoutMillis, TimeUnit.MILLISECONDS));
        }
    }

    @Override
    protected void doAsyncSend(SeataChannel channel, RpcMessage rpcMessage) {
        Object messageBody = rpcMessage.getBody();
        MessageTypeAware messageTypeAware = (MessageTypeAware) messageBody;

        GrpcStubFunction<Message, Message> stubFunction = STUB_FUNCTION_MAP.get(messageTypeAware.getTypeCode());
        Message protoRequest = ProtoTypeConvertHelper.convertToProto(messageBody);
        if (null == stubFunction) {
            //Try bidirectional streaming
            GrpcRemoting.BiStreamMessageType biStreamMessageType = BiStreamMessageTypeHelper.getBiStreamMessageTypeByClass(protoRequest.getClass());
            if (null == biStreamMessageType) {
                LOGGER.error("[GRPC]can not find useful stub for request type:{}", messageTypeAware.getTypeCode());
                throw new IllegalArgumentException("can not find a useful stub for the request type" + messageTypeAware.getTypeCode());
            }
            GrpcRemoting.BiStreamMessage biStreamMessage = GrpcRemoting.BiStreamMessage.newBuilder()
                    .setID(rpcMessage.getId())
                    .setMessageType(biStreamMessageType)
                    .setMessage(Any.pack(protoRequest))
                    .build();
            try {
                channel.sendMsg(biStreamMessage);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else {
            stubFunction.apply(channel, protoRequest);
        }
    }

    @Override
    public void registerProcessor(int requestCode, RemotingProcessor processor, ExecutorService executor) {
        Pair<RemotingProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(requestCode, pair);
    }

    protected RpcMessage buildRequestMessage(Object msg, byte messageType) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(getNextMessageId());
        rpcMessage.setMessageType(messageType);
        rpcMessage.setBody(msg);
        return rpcMessage;
    }

    protected RpcMessage buildResponseMessage(MessageMeta meta, Object msg, byte messageType) {
        RpcMessage rpcMsg = new RpcMessage();
        rpcMsg.setId(meta.getMessageId());
        rpcMsg.setMessageType(messageType);
        rpcMsg.setBody(msg);
        return rpcMsg;
    }

    protected void registerDefaultClientStub() {
        //TransactionManagerService
        STUB_FUNCTION_MAP.put(MessageType.TYPE_GLOBAL_BEGIN, (GrpcStubFunction<io.seata.serializer.protobuf.generated.GlobalBeginRequestProto, io.seata.serializer.protobuf.generated.GlobalBeginResponseProto>) (channel, req) -> {
            io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.TransactionManagerServiceFutureStub futureStub = io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.globalBegin(req);
        });
        STUB_FUNCTION_MAP.put(MessageType.TYPE_GLOBAL_COMMIT, (GrpcStubFunction<io.seata.serializer.protobuf.generated.GlobalCommitRequestProto, io.seata.serializer.protobuf.generated.GlobalCommitResponseProto>) (channel, req) -> {
            io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.TransactionManagerServiceFutureStub futureStub = io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.globalCommit(req);
        });
        STUB_FUNCTION_MAP.put(MessageType.TYPE_GLOBAL_ROLLBACK, (GrpcStubFunction<io.seata.serializer.protobuf.generated.GlobalRollbackRequestProto, io.seata.serializer.protobuf.generated.GlobalRollbackResponseProto>) (channel, req) -> {
            io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.TransactionManagerServiceFutureStub futureStub = io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.globalRollback(req);
        });
        STUB_FUNCTION_MAP.put(MessageType.TYPE_GLOBAL_REPORT, (GrpcStubFunction<io.seata.serializer.protobuf.generated.GlobalReportRequestProto, io.seata.serializer.protobuf.generated.GlobalReportResponseProto>) (channel, req) -> {
            io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.TransactionManagerServiceFutureStub futureStub = io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.globalReport(req);
        });
        STUB_FUNCTION_MAP.put(MessageType.TYPE_GLOBAL_STATUS, (GrpcStubFunction<io.seata.serializer.protobuf.generated.GlobalStatusRequestProto, io.seata.serializer.protobuf.generated.GlobalStatusResponseProto>) (channel, req) -> {
            io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.TransactionManagerServiceFutureStub futureStub = io.seata.core.rpc.grpc.generated.TransactionManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.getGlobalStatus(req);
        });

        //ResourceManagerService
        STUB_FUNCTION_MAP.put(MessageType.TYPE_BRANCH_REGISTER, (GrpcStubFunction<io.seata.serializer.protobuf.generated.BranchRegisterRequestProto, BranchRegisterResponseProto>) (channel, req) -> {
            ResourceManagerServiceGrpc.ResourceManagerServiceFutureStub futureStub = ResourceManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.branchRegister(req);
        });
        STUB_FUNCTION_MAP.put(MessageType.TYPE_BRANCH_STATUS_REPORT, (GrpcStubFunction<io.seata.serializer.protobuf.generated.BranchReportRequestProto, io.seata.serializer.protobuf.generated.BranchReportResponseProto>) (channel, req) -> {
            ResourceManagerServiceGrpc.ResourceManagerServiceFutureStub futureStub = ResourceManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.branchReport(req);
        });
        STUB_FUNCTION_MAP.put(MessageType.TYPE_GLOBAL_LOCK_QUERY, (GrpcStubFunction<io.seata.serializer.protobuf.generated.GlobalLockQueryRequestProto, io.seata.serializer.protobuf.generated.GlobalLockQueryResponseProto>) (channel, req) -> {
            ResourceManagerServiceGrpc.ResourceManagerServiceFutureStub futureStub = ResourceManagerServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.lockQuery(req);
        });

        //HealthCheckService
        STUB_FUNCTION_MAP.put(MessageType.TYPE_HEARTBEAT_MSG, (GrpcStubFunction<io.seata.serializer.protobuf.generated.HeartbeatMessageProto, io.seata.serializer.protobuf.generated.HeartbeatMessageProto>) (channel, req) -> {
            io.seata.core.rpc.grpc.generated.HealthCheckServiceGrpc.HealthCheckServiceFutureStub futureStub = io.seata.core.rpc.grpc.generated.HealthCheckServiceGrpc.newFutureStub((io.grpc.Channel) channel.originChannel());
            return futureStub.heartbeatChecker(req);
        });
    }

    protected String loadBalance(String transactionServiceGroup, Object msg) {
        InetSocketAddress address = null;
        try {
            @SuppressWarnings("unchecked")
            List<InetSocketAddress> inetSocketAddressList = RegistryFactory.getInstance().aliveLookup(transactionServiceGroup);
            address = this.doSelect(inetSocketAddressList, msg);
        } catch (Exception ex) {
            LOGGER.error(ex.getMessage());
        }
        if (address == null) {
            throw new FrameworkException(NoAvailableService);
        }
        return NetUtil.toStringAddress(address);
    }

    protected InetSocketAddress doSelect(List<InetSocketAddress> list, Object msg) throws Exception {
        if (CollectionUtils.isNotEmpty(list)) {
            if (list.size() > 1) {
                return LoadBalanceFactory.getInstance().select(list, getXid(msg));
            } else {
                return list.get(0);
            }
        }
        return null;
    }

    protected String getXid(Object msg) {
        String xid = "";
        if (msg instanceof AbstractGlobalEndRequest) {
            xid = ((AbstractGlobalEndRequest) msg).getXid();
        } else if (msg instanceof GlobalBeginRequest) {
            xid = ((GlobalBeginRequest) msg).getTransactionName();
        } else if (msg instanceof BranchRegisterRequest) {
            xid = ((BranchRegisterRequest) msg).getXid();
        } else if (msg instanceof BranchReportRequest) {
            xid = ((BranchReportRequest) msg).getXid();
        } else {
            try {
                Field field = msg.getClass().getDeclaredField("xid");
                xid = String.valueOf(field.get(msg));
            } catch (Exception ignore) {
            }
        }
        return StringUtils.isBlank(xid) ? String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE)) : xid;
    }

    /**
     * Get pool key function.
     *
     * @return lambda function
     */
    protected abstract Function<String, RpcChannelPoolKey> getPoolKeyFunction();

    /**
     * Get transaction service group.
     *
     * @return transaction service group
     */
    protected abstract String getTransactionServiceGroup();

    /**
     * Whether to enable batch sending of requests, hand over to subclass implementation.
     *
     * @return true:enable, false:disable
     */
    protected abstract boolean isEnableClientBatchSendRequest();

    /**
     * get Rpc Request Timeout
     *
     * @return the Rpc Request Timeout
     */
    protected abstract long getRpcRequestTimeout();

    public abstract void onRegisterMsgSuccess(String serverAddress, SeataChannel channel, Object response,
                                              AbstractMessage requestMessage);

    public abstract void onRegisterMsgFail(String serverAddress, SeataChannel channel, Object response,
                                           AbstractMessage requestMessage);


}
