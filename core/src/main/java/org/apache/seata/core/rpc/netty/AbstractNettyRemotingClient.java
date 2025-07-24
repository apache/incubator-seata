/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelException;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelId;
import io.netty.channel.ChannelPromise;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.MergeMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.AbstractGlobalEndRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.rpc.RemotingClient;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.processor.Pair;
import org.apache.seata.core.rpc.processor.RemotingProcessor;
import org.apache.seata.discovery.loadbalance.LoadBalanceFactory;
import org.apache.seata.discovery.registry.RegistryFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import static org.apache.seata.common.exception.FrameworkErrorCode.NoAvailableService;

/**
 * The netty remoting client.
 */
public abstract class AbstractNettyRemotingClient extends AbstractNettyRemoting implements RemotingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractNettyRemotingClient.class);
    private static final String MSG_ID_PREFIX = "msgId:";
    private static final String FUTURES_PREFIX = "futures:";
    private static final String SINGLE_LOG_POSTFIX = ";";
    private static final int MAX_MERGE_SEND_MILLS = 1;
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    private static final int MAX_MERGE_SEND_THREAD = 1;
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final long SCHEDULE_DELAY_MILLS = 60 * 1000L;
    private static final long SCHEDULE_INTERVAL_MILLS = 10 * 1000L;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageSend";

    private final CopyOnWriteArrayList<ChannelEventListener> channelEventListeners = new CopyOnWriteArrayList<>();

    protected final Object mergeLock = new Object();

    /**
     * When sending message type is {@link MergeMessage}, will be stored to mergeMsgMap.
     */
    protected final Map<Integer, MergeMessage> mergeMsgMap = new ConcurrentHashMap<>();

    protected final Map<Integer, Integer> childToParentMap = new ConcurrentHashMap<>();

    /**
     * When batch sending is enabled, the message will be stored to basketMap
     * Send via asynchronous thread {@link AbstractNettyRemotingClient.MergedSendRunnable}
     * {@link AbstractNettyRemotingClient#isEnableClientBatchSendRequest()}
     */
    protected final ConcurrentHashMap<String /*serverAddress*/, BlockingQueue<RpcMessage>> basketMap =
            new ConcurrentHashMap<>();

    private final NettyClientBootstrap clientBootstrap;
    private final NettyClientChannelManager clientChannelManager;
    private final NettyPoolKey.TransactionRole transactionRole;
    private ExecutorService mergeSendExecutorService;
    private TransactionMessageHandler transactionMessageHandler;
    protected volatile boolean enableClientBatchSendRequest;

    @Override
    public void init() {
        timerExecutor.scheduleAtFixedRate(
                () -> {
                    try {
                        clientChannelManager.reconnect(getTransactionServiceGroup());
                    } catch (Exception ex) {
                        LOGGER.warn("reconnect server failed. {}", ex.getMessage());
                    }
                },
                SCHEDULE_DELAY_MILLS,
                SCHEDULE_INTERVAL_MILLS,
                TimeUnit.MILLISECONDS);
        if (this.isEnableClientBatchSendRequest()) {
            mergeSendExecutorService = new ThreadPoolExecutor(
                    MAX_MERGE_SEND_THREAD,
                    MAX_MERGE_SEND_THREAD,
                    KEEP_ALIVE_TIME,
                    TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(),
                    new NamedThreadFactory(getThreadPrefix(), MAX_MERGE_SEND_THREAD));
            mergeSendExecutorService.submit(new MergedSendRunnable());
        }
        super.init();
        clientBootstrap.start();
    }

    public AbstractNettyRemotingClient(
            NettyClientConfig nettyClientConfig,
            ThreadPoolExecutor messageExecutor,
            NettyPoolKey.TransactionRole transactionRole) {
        super(messageExecutor);
        this.transactionRole = transactionRole;
        clientBootstrap = new NettyClientBootstrap(nettyClientConfig, transactionRole);
        clientBootstrap.setChannelHandlers(new ClientHandler(), new ChannelEventHandler(this));
        clientChannelManager = new NettyClientChannelManager(
                new NettyPoolableFactory(this, clientBootstrap), getPoolKeyFunction(), nettyClientConfig);
    }

    @Override
    public Object sendSyncRequest(Object msg) throws TimeoutException {
        String serverAddress = loadBalance(getTransactionServiceGroup(), msg);
        long timeoutMillis = this.getRpcRequestTimeout();
        RpcMessage rpcMessage = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC);

        // send batch message
        // put message into basketMap, @see MergedSendRunnable
        if (this.isEnableClientBatchSendRequest()) {

            // send batch message is sync request, needs to create messageFuture and put it in futures.
            MessageFuture messageFuture = new MessageFuture();
            messageFuture.setRequestMessage(rpcMessage);
            messageFuture.setTimeout(timeoutMillis);
            futures.put(rpcMessage.getId(), messageFuture);

            // put message into basketMap
            BlockingQueue<RpcMessage> basket =
                    CollectionUtils.computeIfAbsent(basketMap, serverAddress, key -> new LinkedBlockingQueue<>());
            if (!basket.offer(rpcMessage)) {
                LOGGER.error(
                        "put message into basketMap offer failed, serverAddress:{},rpcMessage:{}",
                        serverAddress,
                        rpcMessage);
                return null;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("offer message: {}", rpcMessage.getBody());
            }
            if (!isSending) {
                synchronized (mergeLock) {
                    mergeLock.notifyAll();
                }
            }

            try {
                Object response = messageFuture.get(timeoutMillis, TimeUnit.MILLISECONDS);
                return response;
            } catch (Exception exx) {
                LOGGER.error(
                        "wait response error:{},ip:{},request:{}",
                        exx.getMessage(),
                        serverAddress,
                        rpcMessage.getBody());
                if (exx instanceof TimeoutException) {
                    throw (TimeoutException) exx;
                } else {
                    throw new RuntimeException(exx);
                }
            }
        } else {
            Channel channel = clientChannelManager.acquireChannel(serverAddress);
            return super.sendSync(channel, rpcMessage, timeoutMillis);
        }
    }

    @Override
    public Object sendSyncRequest(Channel channel, Object msg) throws TimeoutException {
        if (channel == null) {
            LOGGER.warn("sendSyncRequest nothing, caused by null channel.");
            return null;
        }
        RpcMessage rpcMessage = buildRequestMessage(msg, ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        return super.sendSync(channel, rpcMessage, this.getRpcRequestTimeout());
    }

    @Override
    public void sendAsyncRequest(Channel channel, Object msg) {
        if (channel == null) {
            LOGGER.warn("sendAsyncRequest nothing, caused by null channel.");
            throw new FrameworkException(
                    new Throwable("throw"), "frameworkException", FrameworkErrorCode.ChannelIsNotWritable);
        }
        RpcMessage rpcMessage = buildRequestMessage(
                msg,
                msg instanceof HeartbeatMessage
                        ? ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST
                        : ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        Object body = rpcMessage.getBody();
        if (body instanceof MergeMessage) {
            Integer parentId = rpcMessage.getId();
            mergeMsgMap.put(parentId, (MergeMessage) rpcMessage.getBody());
            if (body instanceof MergedWarpMessage) {
                for (Integer msgId : ((MergedWarpMessage) rpcMessage.getBody()).msgIds) {
                    childToParentMap.put(msgId, parentId);
                }
            }
        }
        super.sendAsync(channel, rpcMessage);
    }

    @Override
    public void sendAsyncResponse(String serverAddress, RpcMessage rpcMessage, Object msg) {
        RpcMessage rpcMsg = buildResponseMessage(rpcMessage, msg, ProtocolConstants.MSGTYPE_RESPONSE);
        Channel channel = clientChannelManager.acquireChannel(serverAddress);
        super.sendAsync(channel, rpcMsg);
    }

    @Override
    public void registerProcessor(int requestCode, RemotingProcessor processor, ExecutorService executor) {
        Pair<RemotingProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(requestCode, pair);
    }

    @Override
    public void destroyChannel(String serverAddress, Channel channel) {
        clientChannelManager.destroyChannel(serverAddress, channel);
    }

    @Override
    public void destroy() {
        clientBootstrap.shutdown();
        if (mergeSendExecutorService != null) {
            mergeSendExecutorService.shutdown();
        }
        super.destroy();
    }

    public void setTransactionMessageHandler(TransactionMessageHandler transactionMessageHandler) {
        this.transactionMessageHandler = transactionMessageHandler;
    }

    public TransactionMessageHandler getTransactionMessageHandler() {
        return transactionMessageHandler;
    }

    public NettyClientChannelManager getClientChannelManager() {
        return clientChannelManager;
    }

    protected String loadBalance(String transactionServiceGroup, Object msg) {
        InetSocketAddress address = null;
        try {
            @SuppressWarnings("unchecked")
            List<InetSocketAddress> inetSocketAddressList =
                    RegistryFactory.getInstance().aliveLookup(transactionServiceGroup);
            address = this.doSelect(inetSocketAddressList, msg);
        } catch (Exception ex) {
            LOGGER.error("Select the address failed: {}", ex.getMessage());
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
        return StringUtils.isBlank(xid)
                ? String.valueOf(ThreadLocalRandom.current().nextLong(Long.MAX_VALUE))
                : xid;
    }

    private String getThreadPrefix() {
        return AbstractNettyRemotingClient.MERGE_THREAD_PREFIX + THREAD_PREFIX_SPLIT_CHAR + transactionRole.name();
    }

    /**
     * Get pool key function.
     *
     * @return lambda function
     */
    protected abstract Function<String, NettyPoolKey> getPoolKeyFunction();

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

    /**
     * Registers a channel event listener to receive channel events.
     * If the listener is already registered, it will not be added again.
     *
     * @param channelEventListener the channel event listener to register
     */
    @Override
    public void registerChannelEventListener(ChannelEventListener channelEventListener) {
        if (channelEventListener != null) {
            channelEventListeners.addIfAbsent(channelEventListener);
            LOGGER.info(
                    "register channel event listener: {}",
                    channelEventListener.getClass().getName());
        }
    }

    /**
     * Unregisters a previously registered channel event listener.
     *
     * @param channelEventListener the channel event listener to unregister
     */
    @Override
    public void unregisterChannelEventListener(ChannelEventListener channelEventListener) {
        if (channelEventListener != null) {
            channelEventListeners.remove(channelEventListener);
            LOGGER.info(
                    "unregister channel event listener: {}",
                    channelEventListener.getClass().getName());
        }
    }

    /**
     * Handles channel active events from Netty.
     * Fires a CONNECTED event to all registered listeners.
     *
     * @param channel the channel that became active
     */
    public void onChannelActive(Channel channel) {
        fireChannelEvent(channel, ChannelEventType.CONNECTED);
    }

    /**
     * Handles channel inactive events from Netty.
     * Fires a DISCONNECTED event to all registered listeners and cleans up resources.
     *
     * @param channel the channel that became inactive
     */
    public void onChannelInactive(Channel channel) {
        fireChannelEvent(channel, ChannelEventType.DISCONNECTED);
        cleanupResourcesForChannel(channel);
    }

    /**
     * Handles channel exception events from Netty.
     * Fires an EXCEPTION event to all registered listeners and cleans up resources.
     *
     * @param channel the channel where the exception occurred
     * @param cause   the throwable that represents the exception
     */
    public void onChannelException(Channel channel, Throwable cause) {
        fireChannelEvent(channel, ChannelEventType.EXCEPTION, cause);
        cleanupResourcesForChannel(channel);
    }

    /**
     * Handles channel idle events from Netty.
     * Fires an IDLE event to all registered listeners.
     *
     * @param channel the channel that became idle
     */
    public void onChannelIdle(Channel channel) {
        fireChannelEvent(channel, ChannelEventType.IDLE);
    }

    /**
     * Cleans up resources associated with a channel that has been disconnected.
     * This includes collecting message IDs for the channel and cleaning up their futures.
     *
     * @param channel the channel for which resources should be cleaned up
     */
    protected void cleanupResourcesForChannel(Channel channel) {
        if (channel == null) {
            return;
        }
        ChannelException cause =
                new ChannelException(String.format("Channel disconnected: %s", channel.remoteAddress()));

        Set<Integer> messageIds = collectMessageIdsForChannel(channel.id());
        cleanupFuturesForMessageIds(messageIds, cause);

        LOGGER.info(
                "Cleaned up {} pending requests for disconnected channel: {}",
                messageIds.size(),
                channel.remoteAddress());
    }

    /**
     * Collects message IDs associated with a specific channel.
     * This is used during channel cleanup to identify pending requests.
     *
     * @param channelId the ID of the channel
     * @return a set of message IDs associated with the channel
     */
    private Set<Integer> collectMessageIdsForChannel(ChannelId channelId) {
        Set<Integer> messageIds = new HashSet<>();

        String serverAddress = null;
        for (Map.Entry<String, Channel> entry :
                clientChannelManager.getChannels().entrySet()) {
            Channel channel = entry.getValue();
            if (channelId.equals(channel.id())) {
                serverAddress = entry.getKey();
                break;
            }
        }

        if (serverAddress == null) {
            return messageIds;
        }

        Iterator<Map.Entry<Integer, MergeMessage>> it = mergeMsgMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, MergeMessage> entry = it.next();
            MergeMessage mergeMessage = entry.getValue();

            if (mergeMessage instanceof MergedWarpMessage) {
                MergedWarpMessage warpMessage = (MergedWarpMessage) mergeMessage;

                BlockingQueue<RpcMessage> basket = basketMap.get(serverAddress);
                if (basket != null && !basket.isEmpty()) {
                    messageIds.addAll(warpMessage.msgIds);
                    it.remove();
                }
            }
        }

        return messageIds;
    }

    /**
     * Cleans up futures for a set of message IDs.
     * This completes futures with an exception to notify waiting threads.
     *
     * @param messageIds the set of message IDs whose futures should be cleaned up
     * @param cause      the exception to set as the result for each future
     */
    private void cleanupFuturesForMessageIds(Set<Integer> messageIds, Exception cause) {
        for (Integer messageId : messageIds) {
            Integer parentId = childToParentMap.remove(messageId);
            if (parentId != null) {
                mergeMsgMap.remove(parentId);
            }

            MessageFuture future = futures.remove(messageId);
            if (future != null) {
                future.setResultMessage(cause);
            }
        }
    }

    /**
     * Fires a channel event without an associated cause.
     * This is an overloaded version that calls {@link #fireChannelEvent(Channel, ChannelEventType, Throwable)}
     * with a null cause.
     *
     * @param channel   the channel associated with the event
     * @param eventType the type of event that occurred
     */
    protected void fireChannelEvent(Channel channel, ChannelEventType eventType) {
        fireChannelEvent(channel, eventType, null);
    }

    /**
     * Fires a channel event to all registered listeners.
     * This method dispatches the event to the appropriate method on each listener
     * based on the event type.
     *
     * @param channel   the channel associated with the event
     * @param eventType the type of event that occurred
     * @param cause     the cause of the event (maybe null for certain event types)
     */
    protected void fireChannelEvent(Channel channel, ChannelEventType eventType, Throwable cause) {
        for (ChannelEventListener listener : channelEventListeners) {
            try {
                switch (eventType) {
                    case CONNECTED:
                        listener.onChannelConnected(channel);
                        break;
                    case DISCONNECTED:
                        listener.onChannelDisconnected(channel);
                        break;
                    case EXCEPTION:
                        listener.onChannelException(channel, cause);
                        break;
                    case IDLE:
                        listener.onChannelIdle(channel);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
                LOGGER.warn("Error while firing channel {} event", eventType, e);
            }
        }
    }

    /**
     * The type Merged send runnable.
     */
    private class MergedSendRunnable implements Runnable {

        @Override
        public void run() {
            while (true) {
                synchronized (mergeLock) {
                    try {
                        mergeLock.wait(MAX_MERGE_SEND_MILLS);
                    } catch (InterruptedException e) {
                    }
                }
                isSending = true;
                basketMap.forEach((address, basket) -> {
                    if (basket.isEmpty()) {
                        return;
                    }

                    MergedWarpMessage mergeMessage = new MergedWarpMessage();
                    while (!basket.isEmpty()) {
                        RpcMessage msg = basket.poll();
                        mergeMessage.msgs.add((AbstractMessage) msg.getBody());
                        mergeMessage.msgIds.add(msg.getId());
                    }
                    if (mergeMessage.msgIds.size() > 1) {
                        printMergeMessageLog(mergeMessage);
                    }
                    Channel sendChannel = null;
                    try {
                        // send batch message is sync request, but there is no need to get the return value.
                        // Since the messageFuture has been created before the message is placed in basketMap,
                        // the return value will be obtained in ClientOnResponseProcessor.
                        sendChannel = clientChannelManager.acquireChannel(address);
                        AbstractNettyRemotingClient.this.sendAsyncRequest(sendChannel, mergeMessage);
                    } catch (FrameworkException e) {
                        if (e.getErrcode() == FrameworkErrorCode.ChannelIsNotWritable && sendChannel != null) {
                            destroyChannel(address, sendChannel);
                        }
                        // fast fail
                        for (Integer msgId : mergeMessage.msgIds) {
                            MessageFuture messageFuture = futures.remove(msgId);
                            Integer parentId = childToParentMap.remove(msgId);
                            if (parentId != null) {
                                mergeMsgMap.remove(parentId);
                            }
                            if (messageFuture != null) {
                                messageFuture.setResultMessage(
                                        new RuntimeException(String.format("%s is unreachable", address), e));
                            }
                        }
                        LOGGER.error("client merge call failed: {}", e.getMessage(), e);
                    }
                });
                isSending = false;
            }
        }

        private void printMergeMessageLog(MergedWarpMessage mergeMessage) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("merge msg size:{}", mergeMessage.msgIds.size());
                for (AbstractMessage cm : mergeMessage.msgs) {
                    LOGGER.debug(cm.toString());
                }
                StringBuilder sb = new StringBuilder();
                for (long l : mergeMessage.msgIds) {
                    sb.append(MSG_ID_PREFIX).append(l).append(SINGLE_LOG_POSTFIX);
                }
                sb.append("\n");
                for (long l : futures.keySet()) {
                    sb.append(FUTURES_PREFIX).append(l).append(SINGLE_LOG_POSTFIX);
                }
                LOGGER.debug(sb.toString());
            }
        }
    }

    /**
     * The type ClientHandler.
     */
    @Sharable
    class ClientHandler extends ChannelDuplexHandler {

        @Override
        public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
            if (msg instanceof RpcMessage) {
                processMessage(ctx, (RpcMessage) msg);
            } else {
                LOGGER.error("rpcMessage type error");
            }
        }

        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) {
            synchronized (lock) {
                if (ctx.channel().isWritable()) {
                    lock.notifyAll();
                }
            }
            ctx.fireChannelWritabilityChanged();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            if (messageExecutor.isShutdown()) {
                return;
            }
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("channel inactive: {}", ctx.channel());
            }
            timerExecutor.execute(() -> {
                try {
                    clientChannelManager.releaseChannel(ctx.channel(), getAddressFromChannel(ctx.channel()));
                } catch (Throwable throwable) {
                    LOGGER.error("release channel error: {}", throwable.getMessage(), throwable);
                }
            });
            super.channelInactive(ctx);
        }

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
                if (idleStateEvent.state() == IdleState.READER_IDLE) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("channel {} read idle.", ctx.channel());
                    }
                    try {
                        String serverAddress =
                                NetUtil.toStringAddress(ctx.channel().remoteAddress());
                        clientChannelManager.invalidateObject(serverAddress, ctx.channel());
                    } catch (Exception exx) {
                        LOGGER.error(exx.getMessage());
                    } finally {
                        try {
                            timerExecutor.execute(() -> {
                                try {
                                    clientChannelManager.releaseChannel(
                                            ctx.channel(), getAddressFromChannel(ctx.channel()));
                                } catch (Throwable throwable) {
                                    LOGGER.error("release channel error: {}", throwable.getMessage(), throwable);
                                }
                            });
                        } catch (Exception e) {
                            LOGGER.error("failed to schedule releaseChannel: {}", e.getMessage(), e);
                        }
                    }
                }
                if (idleStateEvent == IdleStateEvent.WRITER_IDLE_STATE_EVENT) {
                    try {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("will send ping msg,channel {}", ctx.channel());
                        }
                        AbstractNettyRemotingClient.this.sendAsyncRequest(ctx.channel(), HeartbeatMessage.PING);
                    } catch (Throwable throwable) {
                        LOGGER.error("send request error: {}", throwable.getMessage(), throwable);
                    }
                }
            }
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            LOGGER.error(
                    FrameworkErrorCode.ExceptionCaught.getErrCode(),
                    NetUtil.toStringAddress(ctx.channel().remoteAddress()) + "connect exception. " + cause.getMessage(),
                    cause);
            timerExecutor.execute(() -> {
                try {
                    clientChannelManager.releaseChannel(ctx.channel(), getAddressFromChannel(ctx.channel()));
                } catch (Throwable throwable) {
                    LOGGER.error("release channel error: {}", throwable.getMessage(), throwable);
                }
            });
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("remove exception rm channel:{}", ctx.channel());
            }
            super.exceptionCaught(ctx, cause);
        }

        @Override
        public void close(ChannelHandlerContext ctx, ChannelPromise future) throws Exception {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info(ctx + " will closed");
            }
            super.close(ctx, future);
        }
    }
}
