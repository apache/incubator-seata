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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.discovery.registry.FileRegistryServiceImpl;
import org.apache.seata.discovery.registry.RegistryFactory;
import org.apache.seata.discovery.registry.RegistryService;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Netty client pool manager.
 *
 */
class NettyClientChannelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientChannelManager.class);

    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();

    private final GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;

    private Function<String, NettyPoolKey> poolKeyFunction;

    NettyClientChannelManager(final NettyPoolableFactory keyPoolableFactory, final Function<String, NettyPoolKey> poolKeyFunction,
                                     final NettyClientConfig clientConfig) {
        nettyClientKeyPool = new GenericKeyedObjectPool<>(keyPoolableFactory);
        nettyClientKeyPool.setConfig(getNettyPoolConfig(clientConfig));
        this.poolKeyFunction = poolKeyFunction;
    }

    private GenericKeyedObjectPool.Config getNettyPoolConfig(final NettyClientConfig clientConfig) {
        GenericKeyedObjectPool.Config poolConfig = new GenericKeyedObjectPool.Config();
        poolConfig.maxActive = clientConfig.getMaxPoolActive();
        poolConfig.minIdle = clientConfig.getMinPoolIdle();
        poolConfig.maxWait = clientConfig.getMaxAcquireConnMills();
        poolConfig.testOnBorrow = clientConfig.isPoolTestBorrow();
        poolConfig.testOnReturn = clientConfig.isPoolTestReturn();
        poolConfig.lifo = clientConfig.isPoolLifo();
        return poolConfig;
    }

    /**
     * Get all channels registered on current Rpc Client.
     *
     * @return channels
     */
    ConcurrentMap<String, Channel> getChannels() {
        return channels;
    }

    /**
     * Acquire netty client channel connected to remote server.
     *
     * @param serverAddress server address
     * @return netty channel
     */
    Channel acquireChannel(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (channelToServer != null) {
                return channelToServer;
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("will connect to {}", serverAddress);
        }
        Object lockObj = CollectionUtils.computeIfAbsent(channelLocks, serverAddress, key -> new Object());
        synchronized (lockObj) {
            return doConnect(serverAddress);
        }
    }

    /**
     * Release channel to pool if necessary.
     *
     * @param channel channel
     * @param serverAddress server address
     */
    void releaseChannel(Channel channel, String serverAddress) {
        if (channel == null || serverAddress == null) { return; }
        try {
            synchronized (channelLocks.get(serverAddress)) {
                Channel ch = channels.get(serverAddress);
                if (ch == null) {
                    nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
                    return;
                }
                if (ch.compareTo(channel) == 0) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("return to pool, rm channel:{}", channel);
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

    /**
     * Destroy channel.
     *
     * @param serverAddress server address
     * @param channel channel
     */
    void destroyChannel(String serverAddress, Channel channel) {
        if (channel == null) { return; }
        try {
            if (channel.equals(channels.get(serverAddress))) {
                channels.remove(serverAddress);
            }
            nettyClientKeyPool.returnObject(poolKeyMap.get(serverAddress), channel);
        } catch (Exception exx) {
            LOGGER.error("return channel to rmPool error:{}", exx.getMessage());
        }
    }

    /**
     * Reconnect to remote server of current transaction service group.
     *
     * @param transactionServiceGroup transaction service group
     */
    void reconnect(String transactionServiceGroup) {
        doReconnect(transactionServiceGroup, false);
    }

    /**
     * Init reconnect to remote server of current transaction service group.
     * @param transactionServiceGroup
     * @param failFast
     */
    void initReconnect(String transactionServiceGroup, boolean failFast) {
        doReconnect(transactionServiceGroup, failFast);
    }

    /**
     * reconnect to remote server of current transaction service group.
     * @param transactionServiceGroup
     * @param failFast
     */
    void doReconnect(String transactionServiceGroup, boolean failFast) {
        List<String> availList;
        try {
            availList = getAvailServerList(transactionServiceGroup);
        } catch (Exception e) {
            LOGGER.error("Failed to get available servers: {}", e.getMessage(), e);
            throwFailFastException(failFast, "Failed to get available servers");
            return;
        }
        if (CollectionUtils.isEmpty(availList)) {
            RegistryService registryService = RegistryFactory.getInstance();
            String clusterName = registryService.getServiceGroup(transactionServiceGroup);

            if (StringUtils.isBlank(clusterName)) {
                LOGGER.error("can not get cluster name in registry config '{}{}', please make sure registry config correct",
                        ConfigurationKeys.SERVICE_GROUP_MAPPING_PREFIX,
                        transactionServiceGroup);
                throwFailFastException(failFast, "can not get cluster name in registry config.");
                return;
            }

            if (!(registryService instanceof FileRegistryServiceImpl)) {
                LOGGER.error("no available service found in cluster '{}', please make sure registry config correct and keep your seata server running", clusterName);
            }
            throwFailFastException(failFast, "no available service found in cluster.");
            return;
        }
        try {
            doReconnect(availList, transactionServiceGroup);
        } catch (Exception e) {
            if (failFast) {
                throw e;
            }
            LOGGER.error("connect server failed. {}", e.getMessage(), e);
        }
    }

    /**
     * Reconnect to remote server of current transaction service group.
     *
     * @param availList avail list
     * @param transactionServiceGroup transaction service group
     */
    void doReconnect(List<String> availList, String transactionServiceGroup) {
        Set<String> channelAddress = new HashSet<>(availList.size());
        Map<String, Exception> failedMap = new HashMap<>();
        try {
            for (String serverAddress : availList) {
                try {
                    acquireChannel(serverAddress);
                    channelAddress.add(serverAddress);
                } catch (Exception e) {
                    failedMap.put(serverAddress, e);
                }
            }
            if (failedMap.size() > 0) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.error("{} can not connect to {} cause:{}", FrameworkErrorCode.NetConnect.getErrCode(),
                            failedMap.keySet(),
                            failedMap.values().stream().map(Throwable::getMessage).collect(Collectors.toSet()));
                } else if (LOGGER.isDebugEnabled()) {
                    failedMap.forEach((key, value) -> {
                        LOGGER.error("{} can not connect to {} cause:{} trace information:",
                                FrameworkErrorCode.NetConnect.getErrCode(), key, value.getMessage(), value);
                    });
                }
                String invalidAddress = StringUtils.join(failedMap.keySet().iterator(), ", ");
                throw new FrameworkException("can not connect to [" + invalidAddress + "]");
            }
        } finally {
            if (CollectionUtils.isNotEmpty(channelAddress)) {
                List<InetSocketAddress> aliveAddress = new ArrayList<>(channelAddress.size());
                for (String address : channelAddress) {
                    String[] array = NetUtil.splitIPPortStr(address);
                    aliveAddress.add(new InetSocketAddress(array[0], Integer.parseInt(array[1])));
                }
                RegistryFactory.getInstance().refreshAliveLookup(transactionServiceGroup, aliveAddress);
            } else {
                RegistryFactory.getInstance().refreshAliveLookup(transactionServiceGroup, Collections.emptyList());
            }
        }
    }

    void invalidateObject(final String serverAddress, final Channel channel) throws Exception {
        nettyClientKeyPool.invalidateObject(poolKeyMap.get(serverAddress), channel);
    }

    void registerChannel(final String serverAddress, final Channel channel) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return;
        }
        channels.put(serverAddress, channel);
    }

    private Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        Channel channelFromPool;
        try {
            NettyPoolKey currentPoolKey = poolKeyFunction.apply(serverAddress);
            poolKeyMap.put(serverAddress, currentPoolKey);
            channelFromPool = nettyClientKeyPool.borrowObject(currentPoolKey);
            channels.put(serverAddress, channelFromPool);
        } catch (Exception exx) {
            LOGGER.error("{} register RM failed.", FrameworkErrorCode.RegisterRM.getErrCode(), exx);
            throw new FrameworkException("can not register RM,err:" + exx.getMessage());
        }
        return channelFromPool;
    }

    private List<String> getAvailServerList(String transactionServiceGroup) throws Exception {
        List<InetSocketAddress> availInetSocketAddressList = RegistryFactory.getInstance()
                .lookup(transactionServiceGroup);
        if (CollectionUtils.isEmpty(availInetSocketAddressList)) {
            return Collections.emptyList();
        }

        return availInetSocketAddressList.stream()
                .map(NetUtil::toStringAddress)
                .collect(Collectors.toList());
    }

    private Channel getExistAliveChannel(Channel rmChannel, String serverAddress) {
        if (rmChannel.isActive()) {
            return rmChannel;
        } else {
            int i = 0;
            for (; i < NettyClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(NettyClientConfig.getCheckAliveInterval());
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
                rmChannel = channels.get(serverAddress);
                if (rmChannel != null && rmChannel.isActive()) {
                    return rmChannel;
                }
            }
            if (i == NettyClientConfig.getMaxCheckAliveRetry()) {
                LOGGER.warn("channel {} is not active after long wait, close it.", rmChannel);
                releaseChannel(rmChannel, serverAddress);
                return null;
            }
        }
        return null;
    }

    private void throwFailFastException(boolean failFast, String message) {
        if (failFast) {
            throw new FrameworkException(message);
        }
    }

}

