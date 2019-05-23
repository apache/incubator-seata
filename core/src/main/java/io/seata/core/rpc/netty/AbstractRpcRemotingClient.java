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
package io.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.MergeResultMessage;
import io.seata.core.protocol.MergedWarpMessage;
import io.seata.core.protocol.MessageFuture;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.rpc.ClientMessageListener;
import io.seata.core.rpc.ClientMessageSender;
import io.seata.discovery.loadbalance.LoadBalanceFactory;
import io.seata.discovery.registry.RegistryFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.seata.common.exception.FrameworkErrorCode.NoAvailableService;

/**
 * The type Rpc remoting client.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/12
 */
public abstract class AbstractRpcRemotingClient extends AbstractRpcRemoting
    implements RegisterMsgListener, ClientMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRpcRemotingClient.class);
    private static final String MSG_ID_PREFIX = "msgId:";
    private static final String FUTURES_PREFIX = "futures:";
    private static final String SINGLE_LOG_POSTFIX = ";";
    private static final int MAX_MERGE_SEND_MILLS = 1;
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";
    
    private static final int MAX_MERGE_SEND_THREAD = 1;
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 20000;
    private static final int SCHEDULE_INTERVAL_MILLS = 5;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageSend";
    
    private final RpcClient clientRemotingService;
    
    protected String applicationId;
    
    protected String transactionServiceGroup;
    
    /**
     * The Netty client key pool.
     */
    protected GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;
    /**
     * The Client message listener.
     */
    protected ClientMessageListener clientMessageListener;
    
    public AbstractRpcRemotingClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup,
                                     ThreadPoolExecutor messageExecutor, NettyPoolKey.TransactionRole transactionRole) {
        super(messageExecutor);
        clientRemotingService = new RpcClient(nettyClientConfig, eventExecutorGroup, this, transactionRole);
    }

    @Override
    public void init() {
        clientRemotingService.start();
        NettyPoolableFactory keyPoolableFactory = new NettyPoolableFactory(this, clientRemotingService);
        nettyClientKeyPool = new GenericKeyedObjectPool<>(keyPoolableFactory);
        nettyClientKeyPool.setConfig(getNettyPoolConfig());
        setChannelHandlers(this);
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                reconnect(transactionServiceGroup);
            }
        }, SCHEDULE_INTERVAL_MILLS, SCHEDULE_INTERVAL_MILLS, TimeUnit.SECONDS);
        ExecutorService mergeSendExecutorService = new ThreadPoolExecutor(MAX_MERGE_SEND_THREAD,
            MAX_MERGE_SEND_THREAD,
            KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            new NamedThreadFactory(getThreadPrefix(MERGE_THREAD_PREFIX), MAX_MERGE_SEND_THREAD));
        mergeSendExecutorService.submit(new MergedSendRunnable());
        super.init();
    }
    
    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof RpcMessage)) {
            return;
        }
        RpcMessage rpcMessage = (RpcMessage) msg;
        if (rpcMessage.getBody() == HeartbeatMessage.PONG) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("received PONG from {}", ctx.channel().remoteAddress());
            }
            return;
        }
        if (rpcMessage.getBody() instanceof MergeResultMessage) {
            MergeResultMessage results = (MergeResultMessage) rpcMessage.getBody();
            MergedWarpMessage mergeMessage = (MergedWarpMessage) mergeMsgMap.remove(rpcMessage.getId());
            for (int i = 0; i < mergeMessage.msgs.size(); i++) {
                long msgId = mergeMessage.msgIds.get(i);
                MessageFuture future = futures.remove(msgId);
                if (future == null) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("msg: {} is not found in futures.", msgId);
                    }
                } else {
                    future.setResultMessage(results.getMsgs()[i]);
                }
            }
            return;
        }
        super.channelRead(ctx, msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        if (messageExecutor.isShutdown()) {
            return;
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("channel inactive: {}", ctx.channel());
        }
        releaseChannel(ctx.channel(), NetUtil.toStringAddress(ctx.channel().remoteAddress()));
        super.channelInactive(ctx);
    }
    
    @Override
    public Object sendMsgWithResponse(Object msg, long timeout) throws TimeoutException {
        String validAddress = loadBalance(transactionServiceGroup);
        Channel acquireChannel = connect(validAddress);
        Object result = super.sendAsyncRequestWithResponse(validAddress, acquireChannel, msg, timeout);
        if (result instanceof GlobalBeginResponse
            && ((GlobalBeginResponse) result).getResultCode() == ResultCode.Failed) {
            LOGGER.error("begin response error,release channel:" + acquireChannel);
            releaseChannel(acquireChannel, validAddress);
        }
        return result;
    }
    
    @Override
    public Object sendMsgWithResponse(Object msg) throws TimeoutException {
        return sendMsgWithResponse(msg, NettyClientConfig.getRpcRequestTimeout());
    }
    
    @Override
    public Object sendMsgWithResponse(String serverAddress, Object msg, long timeout)
        throws TimeoutException {
        return sendAsyncRequestWithResponse(serverAddress, connect(serverAddress), msg, timeout);
    }

    /**
     * Gets client message listener.
     *
     * @return the client message listener
     */
    public ClientMessageListener getClientMessageListener() {
        return clientMessageListener;
    }

    /**
     * Sets client message listener.
     *
     * @param clientMessageListener the client message listener
     */
    public void setClientMessageListener(ClientMessageListener clientMessageListener) {
        this.clientMessageListener = clientMessageListener;
    }

    @Override
    public void dispatch(long msgId, ChannelHandlerContext ctx, Object msg) {
        if (clientMessageListener != null) {
            String remoteAddress = NetUtil.toStringAddress(ctx.channel().remoteAddress());
            clientMessageListener.onMessage(msgId, remoteAddress, msg, this);
        }
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

    protected void reconnect(String transactionServiceGroup) {
        List<String> availList = null;
        try {
            availList = getAvailServerList(transactionServiceGroup);
        } catch (Exception exx) {
            LOGGER.error("Failed to get available servers: {}" + exx.getMessage());
        }
        if (CollectionUtils.isEmpty(availList)) {
            LOGGER.error("no available server to connect.");
            return;
        }
        for (String serverAddress : availList) {
            try {
                connect(serverAddress);
            } catch (Exception e) {
                LOGGER.error(FrameworkErrorCode.NetConnect.getErrCode(),
                    "can not connect to " + serverAddress + " cause:" + e.getMessage(), e);
            }
        }
    }

    /**
     * Gets avail server list.
     *
     * @param transactionServiceGroup the transaction service group
     * @return the avail server list
     * @throws Exception the exception
     */
    protected List<String> getAvailServerList(String transactionServiceGroup) throws Exception {
        List<String> availList = new ArrayList<>();
        List<InetSocketAddress> availInetSocketAddressList = RegistryFactory.getInstance().lookup(
            transactionServiceGroup);
        if (!CollectionUtils.isEmpty(availInetSocketAddressList)) {
            for (InetSocketAddress address : availInetSocketAddressList) {
                availList.add(NetUtil.toStringAddress(address));
            }
        }
        return availList;
    }

    protected String loadBalance(String transactionServiceGroup) {
        InetSocketAddress address = null;
        try {
            List<InetSocketAddress> inetSocketAddressList = RegistryFactory.getInstance().lookup(
                transactionServiceGroup);
            address = LoadBalanceFactory.getInstance().select(inetSocketAddressList);
        } catch (Exception ignore) {
            LOGGER.error(ignore.getMessage());
        }
        if (address == null) {
            throw new FrameworkException(NoAvailableService);
        }
        return NetUtil.toStringAddress(address);
    }

    /**
     * Gets thread prefix.
     *
     * @param threadPrefix the thread prefix
     * @return the thread prefix
     */
    protected String getThreadPrefix(String threadPrefix) {
        return threadPrefix + THREAD_PREFIX_SPLIT_CHAR + getTransactionRole().name();
    }

    /**
     * Connect channel.
     *
     * @param serverAddress the server address
     * @return the channel
     */
    protected abstract Channel connect(String serverAddress);

    /**
     * Release channel.
     *
     * @param channel       the channel
     * @param serverAddress the server address
     */
    protected abstract void releaseChannel(Channel channel, String serverAddress);

    /**
     * Gets netty pool config.
     *
     * @return the netty pool config
     */
    protected abstract Config getNettyPoolConfig();

    /**
     * Gets transaction role.
     *
     * @return the transaction role
     */
    protected abstract NettyPoolKey.TransactionRole getTransactionRole();

    /**
     * The type Merged send runnable.
     */
    public class MergedSendRunnable implements Runnable {

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
                for (String address : basketMap.keySet()) {
                    BlockingQueue<RpcMessage> basket = basketMap.get(address);
                    if (basket.isEmpty()) {
                        continue;
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
                        sendChannel = connect(address);
                        sendRequest(sendChannel, mergeMessage);
                    } catch (FrameworkException e) {
                        if (e.getErrcode() == FrameworkErrorCode.ChannelIsNotWritable && sendChannel != null) {
                            destroyChannel(address, sendChannel);
                        }
                        // fast fail
                        for (Long msgId : mergeMessage.msgIds) {
                            MessageFuture messageFuture = futures.remove(msgId);
                            if (messageFuture != null) {
                                messageFuture.setResultMessage(null);
                            }
                        }
                        LOGGER.error("", "client merge call failed", e);
                    }
                }
                isSending = false;
            }
        }

        private void printMergeMessageLog(MergedWarpMessage mergeMessage) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("merge msg size:" + mergeMessage.msgIds.size());
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
}
