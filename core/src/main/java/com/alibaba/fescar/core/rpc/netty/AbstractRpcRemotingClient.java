/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.core.rpc.netty;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.alibaba.fescar.common.exception.FrameworkErrorCode;
import com.alibaba.fescar.common.exception.FrameworkException;
import com.alibaba.fescar.common.thread.NamedThreadFactory;
import com.alibaba.fescar.common.util.NetUtil;
import com.alibaba.fescar.core.protocol.AbstractMessage;
import com.alibaba.fescar.core.protocol.HeartbeatMessage;
import com.alibaba.fescar.core.protocol.MergeResultMessage;
import com.alibaba.fescar.core.protocol.MergedWarpMessage;
import com.alibaba.fescar.core.protocol.MessageFuture;
import com.alibaba.fescar.core.protocol.RpcMessage;
import com.alibaba.fescar.core.rpc.ClientMessageListener;
import com.alibaba.fescar.core.rpc.ClientMessageSender;
import com.alibaba.fescar.core.rpc.RemotingService;
import com.alibaba.fescar.core.rpc.netty.NettyPoolKey.TransactionRole;
import com.alibaba.fescar.core.service.ServiceManager;
import com.alibaba.fescar.core.service.ServiceManagerStaticConfigImpl;
import com.alibaba.fescar.discovery.registry.RegistryFactory;
import com.alibaba.nacos.client.naming.utils.CollectionUtils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.EpollChannelOption;
import io.netty.channel.epoll.EpollMode;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.AbstractChannelPoolMap;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.pool.FixedChannelPool.AcquireTimeoutAction;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.PlatformDependent;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.apache.commons.pool.impl.GenericKeyedObjectPool.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Rpc remoting client.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /9/12
 */
public abstract class AbstractRpcRemotingClient extends AbstractRpcRemoting
    implements RemotingService, RegisterMsgListener, ClientMessageSender {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractRpcRemotingClient.class);
    private final NettyClientConfig nettyClientConfig;
    private final Bootstrap bootstrap = new Bootstrap();
    private final EventLoopGroup eventLoopGroupWorker;
    private EventExecutorGroup defaultEventExecutorGroup;
    private AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool> clientChannelPool;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final String MSG_ID_PREFIX = "msgId:";
    private static final String FUTURES_PREFIX = "futures:";
    private static final String SINGLE_LOG_POSTFIX = ";";
    private static final int MAX_MERGE_SEND_MILLS = 1;
    private static final String THREAD_PREFIX_SPLIT_CHAR = "_";

    /**
     * The Service manager.
     */
    protected ServiceManager serviceManager;

    /**
     * The Netty client key pool.
     */
    protected GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;
    /**
     * The Client message listener.
     */
    protected ClientMessageListener clientMessageListener;

    /**
     * Instantiates a new Rpc remoting client.
     *
     * @param nettyClientConfig the netty client config
     */
    public AbstractRpcRemotingClient(final NettyClientConfig nettyClientConfig) {
        this(nettyClientConfig, null, null);
    }

    /**
     * Instantiates a new Rpc remoting client.
     *
     * @param nettyClientConfig  the netty client config
     * @param eventExecutorGroup the event executor group
     * @param messageExecutor    the message executor
     */
    public AbstractRpcRemotingClient(NettyClientConfig nettyClientConfig, final EventExecutorGroup eventExecutorGroup,
                                     final ThreadPoolExecutor messageExecutor) {
        super(messageExecutor);
        if (null == nettyClientConfig) {
            nettyClientConfig = new NettyClientConfig();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("use default netty client config.");
            }
        }
        this.nettyClientConfig = nettyClientConfig;
        int selectorThreadSizeThreadSize = this.nettyClientConfig.getClientSelectorThreadSize();
        this.eventLoopGroupWorker = new NioEventLoopGroup(selectorThreadSizeThreadSize,
            new NamedThreadFactory(getThreadPrefix(this.nettyClientConfig.getClientSelectorThreadPrefix()),
                selectorThreadSizeThreadSize));
        this.defaultEventExecutorGroup = eventExecutorGroup;
    }

    @Override
    public void init() {
        NettyPoolableFactory keyPoolableFactory = new NettyPoolableFactory(this);
        nettyClientKeyPool = new GenericKeyedObjectPool(keyPoolableFactory);
        nettyClientKeyPool.setConfig(getNettyPoolConfig());
        serviceManager = new ServiceManagerStaticConfigImpl();
        super.init();
    }

    @Override
    public void start() {

        if (this.defaultEventExecutorGroup == null) {
            this.defaultEventExecutorGroup = new DefaultEventExecutorGroup(nettyClientConfig.getClientWorkerThreads(),
                new NamedThreadFactory(getThreadPrefix(nettyClientConfig.getClientWorkerThreadPrefix()),
                    nettyClientConfig.getClientWorkerThreads()));
        }
        this.bootstrap.group(this.eventLoopGroupWorker).channel(
            nettyClientConfig.getClientChannelClazz()).option(
            ChannelOption.TCP_NODELAY, true).option(ChannelOption.SO_KEEPALIVE, true).option(
            ChannelOption.CONNECT_TIMEOUT_MILLIS, nettyClientConfig.getConnectTimeoutMillis()).option(
            ChannelOption.SO_SNDBUF, nettyClientConfig.getClientSocketSndBufSize()).option(ChannelOption.SO_RCVBUF,
            nettyClientConfig.getClientSocketRcvBufSize());

        if (nettyClientConfig.enableNative()) {
            if (PlatformDependent.isOsx()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("client run on macOS");
                }
            } else {
                bootstrap.option(EpollChannelOption.EPOLL_MODE, EpollMode.EDGE_TRIGGERED)
                    .option(EpollChannelOption.TCP_QUICKACK, true);
            }
        }
        if (nettyClientConfig.isUseConnPool()) {
            clientChannelPool = new AbstractChannelPoolMap<InetSocketAddress, FixedChannelPool>() {
                @Override
                protected FixedChannelPool newPool(InetSocketAddress key) {
                    FixedChannelPool fixedClientChannelPool = new FixedChannelPool(
                        bootstrap.remoteAddress(key),
                        new DefaultChannelPoolHandler() {
                            @Override
                            public void channelCreated(Channel ch) throws Exception {
                                super.channelCreated(ch);
                                final ChannelPipeline pipeline = ch.pipeline();
                                pipeline.addLast(defaultEventExecutorGroup,
                                    new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                        nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                        nettyClientConfig.getChannelMaxAllIdleSeconds()));
                                pipeline.addLast(defaultEventExecutorGroup, new RpcClientHandler());
                            }
                        },
                        ChannelHealthChecker.ACTIVE,
                        AcquireTimeoutAction.FAIL,
                        nettyClientConfig.getMaxAcquireConnMills(),
                        nettyClientConfig.getPerHostMaxConn(),
                        nettyClientConfig.getPendingConnSize(),
                        false
                    );
                    return fixedClientChannelPool;

                }
            };
        } else {
            bootstrap.handler(
                new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                            new IdleStateHandler(nettyClientConfig.getChannelMaxReadIdleSeconds(),
                                nettyClientConfig.getChannelMaxWriteIdleSeconds(),
                                nettyClientConfig.getChannelMaxAllIdleSeconds()))
                            .addLast(new MessageCodecHandler());
                        if (null != channelHandlers) {
                            addChannelPipelineLast(ch, channelHandlers);
                        }
                    }
                });
        }
        if (initialized.compareAndSet(false, true) && LOGGER.isInfoEnabled()) {
            LOGGER.info("AbstractRpcRemotingClient has started");
        }
    }

    /**
     * Gets new channel.
     *
     * @param address the address
     * @return the new channel
     */
    protected Channel getNewChannel(InetSocketAddress address) {
        Channel channel = null;
        ChannelFuture f = this.bootstrap.connect(address);
        try {
            f.await(this.nettyClientConfig.getConnectTimeoutMillis(), TimeUnit.MILLISECONDS);
            if (f.isCancelled()) {
                throw new FrameworkException(f.cause(), "connect cancelled, can not connect to fescar-server.");
            } else if (!f.isSuccess()) {
                throw new FrameworkException(f.cause(), "connect failed, can not connect to fescar-server.");
            } else {
                channel = f.channel();
            }
        } catch (Exception e) {
            throw new FrameworkException(e, "can not connect to fescar-server.");
        }
        return channel;
    }

    @Override
    public void shutdown() {
        try {
            if (null != clientChannelPool) {
                clientChannelPool.close();
            }
            this.eventLoopGroupWorker.shutdownGracefully();
            if (this.defaultEventExecutorGroup != null) {
                this.defaultEventExecutorGroup.shutdownGracefully();
            }
            super.destroy();
        } catch (Exception exx) {
            LOGGER.error("shutdown error:" + exx.getMessage());
        }
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage)msg;
            if (rpcMessage.getBody() == HeartbeatMessage.PONG) {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("received PONG from " + ctx.channel().remoteAddress());
                }
                return;
            }
        }

        if (((RpcMessage)msg).getBody() instanceof MergeResultMessage) {
            MergeResultMessage results = (MergeResultMessage)((RpcMessage)msg).getBody();
            MergedWarpMessage mergeMessage = (MergedWarpMessage)mergeMsgMap.remove(((RpcMessage)msg).getId());
            int num = mergeMessage.msgs.size();
            for (int i = 0; i < num; i++) {
                long msgId = mergeMessage.msgIds.get(i);
                MessageFuture future = futures.remove(msgId);
                if (future == null) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("msg:" + msgId + " is not found in futures.");
                    }
                } else {
                    future.setResultMessage(results.getMsgs()[i]);
                }
            }
            return;
        }
        super.channelRead(ctx, msg);
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
    protected abstract TransactionRole getTransactionRole();

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
                    } catch (InterruptedException e) {}
                }
                isSending = true;
                for (String address : basketMap.keySet()) {
                    BlockingQueue<RpcMessage> basket = basketMap.get(address);
                    if (basket.isEmpty()) { continue; }

                    MergedWarpMessage mergeMessage = new MergedWarpMessage();
                    while (!basket.isEmpty()) {
                        RpcMessage msg = basket.poll();
                        mergeMessage.msgs.add((AbstractMessage)msg.getBody());
                        mergeMessage.msgIds.add(msg.getId());
                    }
                    if (mergeMessage.msgIds.size() > 1) {
                        printMergeMessageLog(mergeMessage);
                    }
                    Channel sendChannel = connect(address);
                    try {
                        sendRequest(sendChannel, mergeMessage);
                    } catch (FrameworkException e) {
                        if (e.getErrcode() == FrameworkErrorCode.ChannelIsNotWritable
                            && address != null) {
                            destroyChannel(address, sendChannel);
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
                for (AbstractMessage cm : mergeMessage.msgs) { LOGGER.debug(cm.toString()); }
                StringBuffer sb = new StringBuffer();
                for (long l : mergeMessage.msgIds) { sb.append(MSG_ID_PREFIX).append(l).append(SINGLE_LOG_POSTFIX); }
                sb.append("\n");
                for (long l : futures.keySet()) { sb.append(FUTURES_PREFIX).append(l).append(SINGLE_LOG_POSTFIX); }
                LOGGER.debug(sb.toString());
            }
        }
    }
}
