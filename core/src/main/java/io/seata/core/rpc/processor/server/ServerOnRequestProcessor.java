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
package io.seata.core.rpc.processor.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.seata.common.ConfigurationKeys;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.BatchResultMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.Version;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.netty.NettyServerConfig;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * process RM/TM client request message.
 * <p>
 * message type:
 * RM:
 * 1) {@link MergedWarpMessage}
 * 2) {@link BranchRegisterRequest}
 * 3) {@link BranchReportRequest}
 * 4) {@link GlobalLockQueryRequest}
 * TM:
 * 1) {@link MergedWarpMessage}
 * 2) {@link GlobalBeginRequest}
 * 3) {@link GlobalCommitRequest}
 * 4) {@link GlobalReportRequest}
 * 5) {@link GlobalRollbackRequest}
 * 6) {@link GlobalStatusRequest}
 *
 * @author zhangchenghui.dev@gmail.com
 * @since 1.3.0
 */
public class ServerOnRequestProcessor implements RemotingProcessor, Disposable {

    private static final Logger LOGGER = LoggerFactory.getLogger(ServerOnRequestProcessor.class);

    private final RemotingServer remotingServer;

    private final TransactionMessageHandler transactionMessageHandler;

    private ExecutorService batchResponseExecutorService;

    private final ConcurrentMap<Channel, BlockingQueue<QueueItem>> basketMap = new ConcurrentHashMap<>();
    protected final Object batchResponseLock = new Object();
    private volatile boolean isResponding = false;
    private static final int MAX_BATCH_RESPONSE_MILLS = 1;
    private static final int MAX_BATCH_RESPONSE_THREAD = 1;
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final String BATCH_RESPONSE_THREAD_PREFIX = "rpcBatchResponse";
    private static final boolean PARALLEL_REQUEST_HANDLE =
        ConfigurationFactory.getInstance().getBoolean(ConfigurationKeys.ENABLE_PARALLEL_REQUEST_HANDLE_KEY, true);

    public ServerOnRequestProcessor(RemotingServer remotingServer, TransactionMessageHandler transactionMessageHandler) {
        this.remotingServer = remotingServer;
        this.transactionMessageHandler = transactionMessageHandler;
        if (NettyServerConfig.isEnableTcServerBatchSendResponse()) {
            batchResponseExecutorService = new ThreadPoolExecutor(MAX_BATCH_RESPONSE_THREAD,
                MAX_BATCH_RESPONSE_THREAD,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                new NamedThreadFactory(BATCH_RESPONSE_THREAD_PREFIX, MAX_BATCH_RESPONSE_THREAD));
            batchResponseExecutorService.submit(new BatchResponseRunnable());
        }
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        if (ChannelManager.isRegistered(ctx.channel())) {
            onRequestMessage(ctx, rpcMessage);
        } else {
            try {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("closeChannelHandlerContext channel:" + ctx.channel());
                }
                ctx.disconnect();
                ctx.close();
            } catch (Exception exx) {
                LOGGER.error(exx.getMessage());
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(String.format("close a unhandled connection! [%s]", ctx.channel().toString()));
            }
        }
    }

    @Override
    public void destroy() {
        if (batchResponseExecutorService != null) {
            batchResponseExecutorService.shutdown();
        }
    }

    private void onRequestMessage(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        Object message = rpcMessage.getBody();
        RpcContext rpcContext = ChannelManager.getContextFromIdentified(ctx.channel());
        if (!(message instanceof AbstractMessage)) {
            LOGGER.error("unrecognized message:{}", message);
            return;
        }
        // the batch send request message
        if (message instanceof MergedWarpMessage) {
            if (NettyServerConfig.isEnableTcServerBatchSendResponse() && StringUtils.isNotBlank(rpcContext.getVersion())
                && Version.isAboveOrEqualVersion150(rpcContext.getVersion())) {
                List<AbstractMessage> msgs = ((MergedWarpMessage)message).msgs;
                List<Integer> msgIds = ((MergedWarpMessage)message).msgIds;
                for (int i = 0; i < msgs.size(); i++) {
                    AbstractMessage msg = msgs.get(i);
                    int msgId = msgIds.get(i);
                    if (PARALLEL_REQUEST_HANDLE) {
                        CompletableFuture.runAsync(
                            () -> handleRequestsByMergedWarpMessageBy150(msg, msgId, rpcMessage, ctx, rpcContext));
                    } else {
                        handleRequestsByMergedWarpMessageBy150(msg, msgId, rpcMessage, ctx, rpcContext);
                    }
                }
            } else {
                List<AbstractResultMessage> results = new ArrayList<>();
                List<CompletableFuture<AbstractResultMessage>> completableFutures = null;
                for (int i = 0; i < ((MergedWarpMessage)message).msgs.size(); i++) {
                    if (PARALLEL_REQUEST_HANDLE) {
                        if (completableFutures == null) {
                            completableFutures = new ArrayList<>();
                        }
                        int finalI = i;
                        completableFutures.add(CompletableFuture.supplyAsync(() -> handleRequestsByMergedWarpMessage(
                            ((MergedWarpMessage)message).msgs.get(finalI), rpcContext)));
                    } else {
                        results.add(i,
                            handleRequestsByMergedWarpMessage(((MergedWarpMessage)message).msgs.get(i), rpcContext));
                    }
                }
                if (CollectionUtils.isNotEmpty(completableFutures)) {
                    try {
                        for (CompletableFuture<AbstractResultMessage> completableFuture : completableFutures) {
                            results.add(completableFuture.get());
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        LOGGER.error("handle request error: {}", e.getMessage(), e);
                    }
                }
                MergeResultMessage resultMessage = new MergeResultMessage();
                resultMessage.setMsgs(results.toArray(new AbstractResultMessage[0]));
                remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resultMessage);
            }
        } else {
            // the single send request message
            final AbstractMessage msg = (AbstractMessage) message;
            if (LOGGER.isInfoEnabled()) {
                String receiveMsgLog = String.format("receive msg[single]: %s, clientIp: %s, vgroup: %s", message,
                    NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
                BatchLogHandler.INSTANCE.writeLog(receiveMsgLog);
            }
            AbstractResultMessage result = transactionMessageHandler.onRequest(msg, rpcContext);
            remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), result);
            if (LOGGER.isInfoEnabled()) {
                String resultMsgLog = String.format("result msg[single]: %s, clientIp: %s, vgroup: %s", result,
                    NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
                BatchLogHandler.INSTANCE.writeLog(resultMsgLog);
            }
        }
    }

    private void notifyBatchRespondingThread() {
        if (!isResponding) {
            synchronized (batchResponseLock) {
                batchResponseLock.notifyAll();
            }
        }
    }

    private BlockingQueue<QueueItem> computeIfAbsentMsgQueue(Channel channel) {
        return CollectionUtils.computeIfAbsent(basketMap, channel, key -> new LinkedBlockingQueue<>());
    }

    private void offerMsg(BlockingQueue<QueueItem> msgQueue, RpcMessage rpcMessage,
                          AbstractResultMessage resultMessage, int msgId, Channel channel) {
        if (!msgQueue.offer(new QueueItem(resultMessage, msgId, rpcMessage))) {
            LOGGER.error("put message into basketMap offer failed, channel:{},rpcMessage:{},resultMessage:{}",
                channel, rpcMessage, resultMessage);
        }
    }

    /**
     * batch response runnable
     *
     * @since 1.5.0
     */
    private class BatchResponseRunnable implements Runnable {
        @Override
        public void run() {
            while (true) {
                synchronized (batchResponseLock) {
                    try {
                        batchResponseLock.wait(MAX_BATCH_RESPONSE_MILLS);
                    } catch (InterruptedException e) {
                        LOGGER.error("BatchResponseRunnable Interrupted error", e);
                    }
                }
                isResponding = true;
                basketMap.forEach((channel, msgQueue) -> {
                    if (msgQueue.isEmpty()) {
                        return;
                    }
                    // Because the [serialization,compressor,rpcMessageId,headMap] of the response
                    // needs to be the same as the [serialization,compressor,rpcMessageId,headMap] of the request.
                    // Assemble by grouping according to the [serialization,compressor,rpcMessageId,headMap] dimensions.
                    Map<ClientRequestRpcInfo, BatchResultMessage> batchResultMessageMap = new HashMap<>();
                    while (!msgQueue.isEmpty()) {
                        QueueItem item = msgQueue.poll();
                        BatchResultMessage batchResultMessage = CollectionUtils.computeIfAbsent(batchResultMessageMap,
                            new ClientRequestRpcInfo(item.getRpcMessage()),
                            key -> new BatchResultMessage());
                        batchResultMessage.getResultMessages().add(item.getResultMessage());
                        batchResultMessage.getMsgIds().add(item.getMsgId());
                    }
                    batchResultMessageMap.forEach((clientRequestRpcInfo, batchResultMessage) ->
                        remotingServer.sendAsyncResponse(buildRpcMessage(clientRequestRpcInfo),
                            channel, batchResultMessage));
                });
                isResponding = false;
            }
        }
    }

    /**
     * handle rpc request message
     * @param rpcContext rpcContext
     */
    private AbstractResultMessage handleRequestsByMergedWarpMessage(AbstractMessage subMessage, RpcContext rpcContext) {
        if (LOGGER.isInfoEnabled()) {
            String receiveMsgLog = String.format("receive msg[merged]: %s, clientIp: %s, vgroup: %s", subMessage,
                NetUtil.toIpAddress(rpcContext.getChannel().remoteAddress()), rpcContext.getTransactionServiceGroup());
            BatchLogHandler.INSTANCE.writeLog(receiveMsgLog);
        }
        AbstractResultMessage resultMessage = transactionMessageHandler.onRequest(subMessage, rpcContext);
        if (LOGGER.isInfoEnabled()) {
            String resultMsgLog = String.format("result msg[merged]: %s, clientIp: %s, vgroup: %s", resultMessage,
                NetUtil.toIpAddress(rpcContext.getChannel().remoteAddress()), rpcContext.getTransactionServiceGroup());
            BatchLogHandler.INSTANCE.writeLog(resultMsgLog);
        }
        return resultMessage;
    }

    /**
     * handle rpc request message
     * @param msg msg
     * @param msgId msgId
     * @param rpcMessage rpcMessage
     * @param ctx ctx
     * @param rpcContext rpcContext
     */
    private void handleRequestsByMergedWarpMessageBy150(AbstractMessage msg, int msgId, RpcMessage rpcMessage,
        ChannelHandlerContext ctx, RpcContext rpcContext) {
        if (LOGGER.isInfoEnabled()) {
            String receiveMsgLog = String.format("receive msg[merged]: %s, clientIp: %s, vgroup: %s", msg,
                NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
            BatchLogHandler.INSTANCE.writeLog(receiveMsgLog);
        }
        AbstractResultMessage resultMessage = transactionMessageHandler.onRequest(msg, rpcContext);
        BlockingQueue<QueueItem> msgQueue = computeIfAbsentMsgQueue(ctx.channel());
        offerMsg(msgQueue, rpcMessage, resultMessage, msgId, ctx.channel());
        notifyBatchRespondingThread();
        if (LOGGER.isInfoEnabled()) {
            String resultMsgLog = String.format("result msg[merged]: %s, clientIp: %s, vgroup: %s", resultMessage,
                NetUtil.toIpAddress(ctx.channel().remoteAddress()), rpcContext.getTransactionServiceGroup());
            BatchLogHandler.INSTANCE.writeLog(resultMsgLog);
        }
    }

    /**
     * build RpcMessage
     *
     * @param clientRequestRpcInfo For saving client request rpc info
     * @return rpcMessage
     */
    private RpcMessage buildRpcMessage(ClientRequestRpcInfo clientRequestRpcInfo) {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(clientRequestRpcInfo.getRpcMessageId());
        rpcMessage.setCodec(clientRequestRpcInfo.getCodec());
        rpcMessage.setCompressor(clientRequestRpcInfo.getCompressor());
        rpcMessage.setHeadMap(clientRequestRpcInfo.getHeadMap());
        return rpcMessage;
    }

    /**
     * For saving client request rpc info
     * <p>
     * Because the [serialization,compressor,rpcMessageId,headMap] of the response
     * needs to be the same as the [serialization,compressor,rpcMessageId,headMap] of the request.
     * Assemble by grouping according to the [serialization,compressor,rpcMessageId,headMap] dimensions.
     */
    private static class ClientRequestRpcInfo {

        /**
         * the Outer layer rpcMessage id
         */
        private int rpcMessageId;

        /**
         * the Outer layer rpcMessage client send request message codec
         */
        private byte codec;

        /**
         * the Outer layer rpcMessage client send request message compressor
         */
        private byte compressor;

        /**
         * the Outer layer rpcMessage headMap
         */
        private Map<String, String> headMap;

        public ClientRequestRpcInfo(RpcMessage rpcMessage) {
            this.rpcMessageId = rpcMessage.getId();
            this.codec = rpcMessage.getCodec();
            this.compressor = rpcMessage.getCompressor();
            this.headMap = rpcMessage.getHeadMap();
        }

        public int getRpcMessageId() {
            return rpcMessageId;
        }

        public void setRpcMessageId(int rpcMessageId) {
            this.rpcMessageId = rpcMessageId;
        }

        public byte getCodec() {
            return codec;
        }

        public void setCodec(byte codec) {
            this.codec = codec;
        }

        public byte getCompressor() {
            return compressor;
        }

        public void setCompressor(byte compressor) {
            this.compressor = compressor;
        }

        public Map<String, String> getHeadMap() {
            return headMap;
        }

        public void setHeadMap(Map<String, String> headMap) {
            this.headMap = headMap;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ClientRequestRpcInfo that = (ClientRequestRpcInfo) o;
            return rpcMessageId == that.rpcMessageId && codec == that.codec
                && compressor == that.compressor && headMap.equals(that.headMap);
        }

        @Override
        public int hashCode() {
            return Objects.hash(rpcMessageId, codec, compressor, headMap);
        }
    }

    /**
     * the queue item
     *
     * @see ServerOnRequestProcessor#basketMap
     */
    private static class QueueItem {

        /**
         * the result message
         */
        private AbstractResultMessage resultMessage;

        /**
         * result message id
         */
        private Integer msgId;

        /**
         * the Outer layer rpcMessage
         */
        private RpcMessage rpcMessage;

        public QueueItem(AbstractResultMessage resultMessage, int msgId, RpcMessage rpcMessage) {
            this.resultMessage = resultMessage;
            this.msgId = msgId;
            this.rpcMessage = rpcMessage;
        }

        public AbstractResultMessage getResultMessage() {
            return resultMessage;
        }

        public void setResultMessage(AbstractResultMessage resultMessage) {
            this.resultMessage = resultMessage;
        }

        public Integer getMsgId() {
            return msgId;
        }

        public void setMsgId(Integer msgId) {
            this.msgId = msgId;
        }

        public RpcMessage getRpcMessage() {
            return rpcMessage;
        }

        public void setRpcMessage(RpcMessage rpcMessage) {
            this.rpcMessage = rpcMessage;
        }
    }

}
