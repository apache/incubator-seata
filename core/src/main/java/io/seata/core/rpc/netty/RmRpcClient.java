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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.concurrent.EventExecutorGroup;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.common.util.NetUtil;
import io.seata.core.model.Resource;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterRMResponse;
import io.seata.core.rpc.netty.NettyPoolKey.TransactionRole;
import org.apache.commons.pool.impl.GenericKeyedObjectPool.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.common.Constants.DBKEYS_SPLIT_CHAR;

/**
 * The type Rm rpc client.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2018 /10/10
 */
@Sharable
public final class RmRpcClient extends AbstractRpcRemotingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(RmRpcClient.class);

    private ResourceManager resourceManager;

    private String applicationId;

    private String transactionServiceGroup;

    private static volatile RmRpcClient instance;
    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<String, Object>();
    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap = new ConcurrentHashMap<String, NettyPoolKey>();
    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();
    private String customerKeys;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final int MAX_MERGE_SEND_THREAD = 1;
    private static final long KEEP_ALIVE_TIME = Integer.MAX_VALUE;
    private static final int MAX_QUEUE_SIZE = 20000;
    private static final int SCHEDULE_INTERVAL_MILLS = 5;
    private static final String MERGE_THREAD_PREFIX = "rpcMergeMessageSend";
    private final NettyClientConfig rmClientConfig;

    private RmRpcClient(NettyClientConfig nettyClientConfig) {
        super(nettyClientConfig);
        this.rmClientConfig = nettyClientConfig;
    }

    private RmRpcClient(NettyClientConfig nettyClientConfig, EventExecutorGroup eventExecutorGroup,
                        ThreadPoolExecutor messageExecutor) {
        super(nettyClientConfig, eventExecutorGroup, messageExecutor);
        this.rmClientConfig = nettyClientConfig;
    }

    /**
     * Sets resource manager.
     *
     * @param resourceManager the resource manager
     */
    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }

    /**
     * Gets instance.
     *
     * @param applicationId           the application id
     * @param transactionServiceGroup the transaction service group
     * @return the instance
     */
    public static RmRpcClient getInstance(String applicationId, String transactionServiceGroup) {
        RmRpcClient rmRpcClient = getInstance();
        rmRpcClient.setApplicationId(applicationId);
        rmRpcClient.setTransactionServiceGroup(transactionServiceGroup);
        return rmRpcClient;
    }

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static RmRpcClient getInstance() {
        if (null == instance) {
            synchronized (RmRpcClient.class) {
                if (null == instance) {
                    NettyClientConfig nettyClientConfig = new NettyClientConfig();
                    final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                        nettyClientConfig.getClientWorkerThreads(), nettyClientConfig.getClientWorkerThreads(),
                        KEEP_ALIVE_TIME, TimeUnit.SECONDS,
                        new LinkedBlockingQueue(MAX_QUEUE_SIZE),
                        new NamedThreadFactory(nettyClientConfig.getRmDispatchThreadPrefix(),
                            nettyClientConfig.getClientWorkerThreads()),
                        new ThreadPoolExecutor.CallerRunsPolicy());
                    instance = new RmRpcClient(nettyClientConfig, null, threadPoolExecutor);
                }
            }
        }
        return instance;
    }

    @Override
    public void init() {
        if (initialized.compareAndSet(false, true)) {
            super.init();
            timerExecutor.scheduleAtFixedRate(new Runnable() {
                @Override
                public void run() {
                    reconnect(transactionServiceGroup);
                }
            }, SCHEDULE_INTERVAL_MILLS, SCHEDULE_INTERVAL_MILLS, TimeUnit.SECONDS);
            ExecutorService mergeSendExecutorService = new ThreadPoolExecutor(MAX_MERGE_SEND_THREAD,
                MAX_MERGE_SEND_THREAD,
                KEEP_ALIVE_TIME, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<Runnable>(),
                new NamedThreadFactory(getThreadPrefix(MERGE_THREAD_PREFIX), MAX_MERGE_SEND_THREAD));
            mergeSendExecutorService.submit(new MergedSendRunnable());
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

    @Override
    protected Config getNettyPoolConfig() {
        Config poolConfig = new Config();
        poolConfig.maxActive = rmClientConfig.getMaxPoolActive();
        poolConfig.minIdle = rmClientConfig.getMinPoolIdle();
        poolConfig.maxWait = rmClientConfig.getMaxAcquireConnMills();
        poolConfig.testOnBorrow = rmClientConfig.isPoolTestBorrow();
        poolConfig.testOnReturn = rmClientConfig.isPoolTestReturn();
        poolConfig.lifo = rmClientConfig.isPoolLifo();
        return poolConfig;
    }

    @Override
    protected NettyPoolKey.TransactionRole getTransactionRole() {
        return TransactionRole.RMROLE;
    }

    @Override
    public Object sendMsgWithResponse(Object msg, long timeout) throws TimeoutException {
        String validAddress = loadBalance(transactionServiceGroup);
        Channel acquireChannel = connect(validAddress);
        return super.sendAsyncRequestWithResponse(validAddress, acquireChannel, msg, timeout);
    }

    @Override
    public Object sendMsgWithResponse(String serverAddress, Object msg, long timeout) throws TimeoutException {
        return super.sendAsyncRequestWithResponse(serverAddress, connect(serverAddress), msg, timeout);
    }

    @Override
    public Object sendMsgWithResponse(Object msg) throws TimeoutException {
        return sendMsgWithResponse(msg, NettyClientConfig.getRpcRequestTimeout());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent)evt;
            if (idleStateEvent == IdleStateEvent.READER_IDLE_STATE_EVENT) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("RmRpcClient channel" + ctx.channel() + " idle.");
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
                    sendRequest(ctx.channel(), HeartbeatMessage.PING);
                } catch (Throwable throwable) {
                    LOGGER.error("", "send request error", throwable);
                }
            }
        }
    }

    @Override
    protected void releaseChannel(Channel channel, String serverAddress) {
        if (null == channel || null == serverAddress) { return; }
        Object connectLock = channelLocks.get(serverAddress);
        try {
            synchronized (connectLock) {
                Channel ch = channels.get(serverAddress);
                if (null == ch) {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                    return;
                }
                if (ch.compareTo(channel) == 0) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("return to pool, rm channel:" + channel);
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
        if (channelToServer != null) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (null != channelToServer) {
                return channelToServer;
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will connect to " + serverAddress);
        }
        channelLocks.putIfAbsent(serverAddress, new Object());
        Object connectLock = channelLocks.get(serverAddress);
        synchronized (connectLock) {
            Channel channel = doConnect(serverAddress);
            return channel;
        }
    }

    /**
     * Connect channel.
     *
     * @param serverAddress the server address
     * @return the channel
     */
    private Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        Channel channelFromPool = null;
        try {
            String resourceIds = customerKeys == null ? getMergedResourceKeys(resourceManager) : customerKeys;
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("RM will register :" + resourceIds);
            }
            RegisterRMRequest message = null;
            if (null == poolKeyMap.get(serverAddress)) {
                message = new RegisterRMRequest(applicationId, transactionServiceGroup);
                message.setResourceIds(resourceIds);
                poolKeyMap.putIfAbsent(serverAddress,
                    new NettyPoolKey(getTransactionRole(), serverAddress, message));
            } else {
                message = (RegisterRMRequest)poolKeyMap.get(serverAddress).getMessage();
                message.setResourceIds(resourceIds);
            }
            channelFromPool = nettyClientKeyPool.borrowObject(poolKeyMap.get(serverAddress));
        } catch (Exception exx) {
            LOGGER.error(FrameworkErrorCode.RegisterRM.getErrCode(), "register RM failed.", exx);
            throw new FrameworkException("can not register RM,err:" + exx.getMessage());
        }
        return channelFromPool;
    }

    private Channel getExistAliveChannel(Channel rmChannel, String serverAddress) {
        if (rmChannel.isActive()) {
            return rmChannel;
        } else {
            int i = 0;
            for (; i < NettyClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(NettyClientConfig.getCheckAliveInternal());
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
                rmChannel = channels.get(serverAddress);
                if (null == rmChannel || rmChannel.isActive()) {
                    return rmChannel;
                }
            }
            if (i == NettyClientConfig.getMaxCheckAliveRetry()) {
                LOGGER.warn("channel " + rmChannel + " is not active after long wait, close it.");
                releaseChannel(rmChannel, serverAddress);
                return null;
            }
        }
        return null;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        LOGGER.error(FrameworkErrorCode.ExceptionCaught.getErrCode(),
            NetUtil.toStringAddress(ctx.channel().remoteAddress()) + "connect exception. " + cause.getMessage(),
            cause);
        releaseChannel(ctx.channel(), getAddressFromChannel(ctx.channel()));
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("remove exception rm channel:" + ctx.channel());
        }
        super.exceptionCaught(ctx, cause);
    }

    private void sendRegisterMessage(String serverAddress, Channel channel, String dbKey) {
        RegisterRMRequest message = new RegisterRMRequest(applicationId,
            transactionServiceGroup);
        message.setResourceIds(dbKey);
        try {
            super.sendAsyncRequestWithoutResponse(null, channel, message);
        } catch (FrameworkException e) {
            if (e.getErrcode() == FrameworkErrorCode.ChannelIsNotWritable
                && serverAddress != null) {
                releaseChannel(channel, serverAddress);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("remove channel:" + channel);
                }
            } else {
                LOGGER.error("", "register failed", e);
            }
        } catch (TimeoutException e) {
            LOGGER.error(e.getMessage());
        }
    }

    /**
     * Register new db key.
     *
     * @param resourceGroupId the resource group id
     * @param resourceId      the db key
     */
    public void registerResource(String resourceGroupId, String resourceId) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("register to RM resourceId:" + resourceId);
        }
        if (channels.isEmpty()) {
            reconnect(transactionServiceGroup);
            return;
        }
        synchronized (channels) {
            for (Map.Entry<String, Channel> entry : channels.entrySet()) {
                String serverAddress = entry.getKey();
                Channel rmChannel = entry.getValue();
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("register resource, resourceId:" + resourceId);
                }
                sendRegisterMessage(serverAddress, rmChannel, resourceId);
            }
        }
    }

    @Override
    public void sendResponse(long msgId, String serverAddress, Object msg) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("RmRpcClient sendResponse " + msg);
        }
        super.sendResponse(msgId, connect(serverAddress), msg);
    }

    /**
     * Gets customer keys.
     *
     * @return the customer keys
     */
    public String getCustomerKeys() {
        return customerKeys;
    }

    /**
     * Sets customer keys.
     *
     * @param customerKeys the customer keys
     */
    public void setCustomerKeys(String customerKeys) {
        this.customerKeys = customerKeys;
    }

    @Override
    public void onRegisterMsgSuccess(String serverAddress, Channel channel, Object response,
                                     AbstractMessage requestMessage) {

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "register RM success. server version:" + ((RegisterRMResponse)response).getVersion()
                    + ",channel:" + channel);
        }
        if (customerKeys == null) {
            synchronized (channels) {
                channels.put(serverAddress, channel);
            }
            String dbKey = getMergedResourceKeys(resourceManager);
            RegisterRMRequest message = (RegisterRMRequest)requestMessage;
            if (message.getResourceIds() != null) {
                if (!message.getResourceIds().equals(dbKey)) {
                    sendRegisterMessage(serverAddress, channel, dbKey);
                }
            }
        }
    }

    /**
     * Gets merged resource keys.
     *
     * @param resourceManager the resource manager
     * @return the merged resource keys
     */
    public String getMergedResourceKeys(ResourceManager resourceManager) {
        //TODO
        Map<String, Resource> managedResources = resourceManager.getManagedResources();
        Set<String> resourceIds = managedResources.keySet();
        if (!resourceIds.isEmpty()) {
            StringBuffer sb = new StringBuffer();
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

    @Override
    public void onRegisterMsgFail(String serverAddress, Channel channel, Object response,
                                  AbstractMessage requestMessage) {

        if (response instanceof RegisterRMResponse && LOGGER.isInfoEnabled()) {
            LOGGER.info(
                "register RM failed. server version:" + ((RegisterRMResponse)response).getVersion());
        }
        throw new FrameworkException("register RM failed.");
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
            LOGGER.error("return channel to rmPool error:" + exx.getMessage());
        }
    }

}
