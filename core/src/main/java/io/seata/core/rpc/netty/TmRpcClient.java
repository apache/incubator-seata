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
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.thread.RejectedPolicies;
import io.seata.common.util.NetUtil;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.RegisterTMResponse;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.commons.pool.impl.GenericKeyedObjectPool.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * The type Rpc client.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/23
 */
@Sharable
public final class TmRpcClient extends AbstractRpcRemotingClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(TmRpcClient.class);
    private static volatile TmRpcClient instance;
    private static final int MAX_MERGE_SEND_THREAD = 1;
    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<String, Object>();
    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<String, Channel>();
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 2000;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageSend";
    private static final int SCHEDULE_INTERVAL_MILLS = 5;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private String applicationId;
    private String transactionServiceGroup;
    private final NettyClientConfig tmClientConfig;
    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap
        = new ConcurrentHashMap<String, NettyPoolKey>();
    /**
     * The constant enableDegrade.
     */
    public static boolean enableDegrade = false;

    private TmRpcClient(NettyClientConfig nettyClientConfig,
                        EventExecutorGroup eventExecutorGroup,
                        ThreadPoolExecutor messageExecutor) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor);
        this.tmClientConfig = nettyClientConfig;
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @return the instance
     */
    public static TmRpcClient getInstance(String applicationId, String transactionServiceGroup) {
        TmRpcClient tmRpcClient = getInstance();
        tmRpcClient.setApplicationId(applicationId);
        tmRpcClient.setTransactionServiceGroup(transactionServiceGroup);
        return tmRpcClient;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static TmRpcClient getInstance() {
        if (null == instance) {
            synchronized (TmRpcClient.class) {
                if (null == instance) {
                    NettyClientConfig nettyClientConfig = new NettyClientConfig();
                    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                        nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
                        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                        new LinkedBlockingQueue(MAX_QUEUE_SIZE),
                        new NamedThreadFactory(nettyClientConfig.getTmDispatchThreadPrefix(),
                            nettyClientConfig.getClientWorkerThreads()),
                        RejectedPolicies.runsOldestTaskPolicy());
                    instance = new TmRpcClient(nettyClientConfig, null, threadPoolExecutor);
                }
            }
        }
        return instance;
    }

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            init(SCHEDULE_INTERVAL_MILLS, SCHEDULE_INTERVAL_MILLS);
        }
    }

    private void initVars() {
        enableDegrade = CONFIG.getBoolean(ConfigurationKeys.SERVICE_PREFIX + ConfigurationKeys.ENABLE_DEGRADE_POSTFIX);
        super.init();
    }

    /**
     * Init.
     *
     * @param healthCheckDelay  the health check delay
     * @param healthCheckPeriod the health check period
     */
    public void init(long healthCheckDelay, long healthCheckPeriod) {
        initVars();
        ExecutorService mergeSendExecutorService = new ThreadPoolExecutor(MAX_MERGE_SEND_THREAD, MAX_MERGE_SEND_THREAD,
            KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(),
            new NamedThreadFactory(getThreadPrefix(MERGE_THREAD_PREFIX), MAX_MERGE_SEND_THREAD));
        mergeSendExecutorService.submit(new MergedSendRunnable());
        timerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    reconnect(transactionServiceGroup);
                } catch (Exception ignore) {
                    LOGGER.error(ignore.getMessage());
                }
            }
        }, healthCheckDelay, healthCheckPeriod, TimeUnit.SECONDS);
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

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
            if (idleStateEvent.state() == IdleState.READER_IDLE) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("channel" + ctx.channel() + " read idle.");
                }
                try {
                    String serverAddress = NetUtil.toStringAddress(ctx.channel().remoteAddress());
                    nettyClientKeyPool.invalidateObject(poolKeyMap.get(serverAddress), ctx.channel());
                } catch (Exception exx) {
                    LOGGER.error(exx.getMessage());
                } finally {
                    releaseChannel(ctx.channel(), getAddressFromContext(ctx));
                }
            }
            if (idleStateEvent == IdleStateEvent.WRITER_IDLE_STATE_EVENT) {
                try {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("will send ping msg,channel" + ctx.channel());
                    }
                    sendRequest(ctx.channel(), HeartbeatMessage.PING);
                } catch (Throwable throwable) {
                    LOGGER.error("", "send request error", throwable);
                }
            }
        }

    }

    @Override
    protected void releaseChannel(Channel channel, String serverAddress) {
        if (null == channel || null == serverAddress) {
            return;
        }
        try {
            Object connectLock = channelLocks.get(serverAddress);
            synchronized (connectLock) {
                Channel ch = channels.get(serverAddress);
                if (null == ch) {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                    return;
                }
                if (ch.compareTo(channel) == 0) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("return to pool, tm channel:" + channel);
                    }
                    destroyChannel(serverAddress, channel);
                } else {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                }
            }
        } catch (Exception exx) {
            LOGGER.error(exx.getMessage());
        }
    }

    @Override
    protected Channel connect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (null != channelToServer) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (null != channelToServer) {
                return channelToServer;
            }
        }
        channelLocks.putIfAbsent(serverAddress, new Object());
        Object connectLock = channelLocks.get(serverAddress);
        synchronized (connectLock) {
            channelToServer = doConnect(serverAddress);
            channels.put(serverAddress, channelToServer);
            return channelToServer;
        }
    }

    private Channel getExistAliveChannel(Channel channel, String serverAddress) {
        if (channel.isActive()) {
            return channel;
        } else {
            int i = 0;
            for (; i < NettyClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(NettyClientConfig.getCheckAliveInternal());
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
                channel = channels.get(serverAddress);
                if (null == channel || channel.isActive()) {
                    return channel;
                }
            }
            if (i == NettyClientConfig.getMaxCheckAliveRetry()) {
                LOGGER.warn("channel " + channel + " is not active after long wait, close it.");
                releaseChannel(channel, serverAddress);
                return null;
            }
        }
        return null;
    }

    @Override
    protected Config getNettyPoolConfig() {
        Config poolConfig = new Config();
        poolConfig.maxActive = tmClientConfig.getMaxPoolActive();
        poolConfig.minIdle = tmClientConfig.getMinPoolIdle();
        poolConfig.maxWait = tmClientConfig.getMaxAcquireConnMills();
        poolConfig.testOnBorrow = tmClientConfig.isPoolTestBorrow();
        poolConfig.testOnReturn = tmClientConfig.isPoolTestReturn();
        poolConfig.lifo = tmClientConfig.isPoolLifo();
        return poolConfig;
    }

    @Override
    protected NettyPoolKey.TransactionRole getTransactionRole() {
        return NettyPoolKey.TransactionRole.TMROLE;
    }

    /**
     * Connect channel.
     *
     * @param serverAddress the server address
     * @return the channel
     */
    private Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (null != channelToServer && channelToServer.isActive()) {
            return channelToServer;
        }
        try {
            RegisterTMRequest
                registerTransactionManagerRequest = new RegisterTMRequest(
                applicationId, transactionServiceGroup);
            poolKeyMap.putIfAbsent(serverAddress,
                new NettyPoolKey(getTransactionRole(), serverAddress, registerTransactionManagerRequest));
            channelToServer = nettyClientKeyPool.borrowObject(poolKeyMap.get(serverAddress));
        } catch (Exception exx) {
            LOGGER.error("get channel from pool error.", exx);
            throw new FrameworkException("can not register TM,err:" + exx.getMessage());
        }
        return channelToServer;
    }

    /**
     * Gets client app name.
     *
     * @return the client app name
     */
    public String getApplicationId() {
        return applicationId;
    }

    /**
     * Sets client app name.
     *
     * @param applicationId the client app name
     */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
     * Gets transaction service group.
     *
     * @return the transaction service group
     */
    public String getTransactionServiceGroup() {
        return transactionServiceGroup;
    }

    /**
     * Sets transaction service group.
     *
     * @param transactionServiceGroup the transaction service group
     */
    public void setTransactionServiceGroup(String transactionServiceGroup) {
        this.transactionServiceGroup = transactionServiceGroup;
    }

    @Override
    public void sendResponse(long msgId, String serverAddress, Object msg) {
        super.sendResponse(msgId, connect(serverAddress), msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(FrameworkErrorCode.ExceptionCaught.getErrCode(),
            NetUtil.toStringAddress(ctx.channel().remoteAddress()) + "connect exception. " + cause.getMessage(), cause);
        releaseChannel(ctx.channel(), getAddressFromChannel(ctx.channel()));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("remove exception rm channel:" + ctx.channel());
        }
        super.exceptionCaught(ctx, cause);
    }

    @Override
    public void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response,
                                     AbstractMessage requestMessage) {
        if (null != channels.get(serverAddress) && channels.get(serverAddress).isActive()) {
            return;
        }
        channels.put(serverAddress, channel);
    }

    @Override
    public void onRegisterMsgFail(String serverAddress, Channel channel, Object response,
                                  AbstractMessage requestMessage) {
        if (response instanceof RegisterTMResponse && LOGGER.isInfoEnabled()) {
            LOGGER.info("register client failed, server version:"
                + ((RegisterTMResponse)response).getVersion());
        }
        throw new FrameworkException("register client app failed.");
    }

    @Override
    public void destroyChannel(String serverAddress, Channel channel) {
        if (null == channel) { return; }
        try {
            if (channel.equals(channels.get(serverAddress))) {
                channels.remove(serverAddress);
            }
            nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
        } catch (Exception exx) {
            LOGGER.error("return channel to rpcPool error:" + exx.getMessage());
        }
    }

}
