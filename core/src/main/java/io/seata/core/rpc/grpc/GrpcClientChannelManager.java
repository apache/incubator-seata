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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.seata.common.Constants;
import io.seata.common.DefaultValues;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.common.util.StringUtils;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.rpc.RpcChannelPoolKey;
import io.seata.core.rpc.SeataChannel;
import io.seata.discovery.registry.FileRegistryServiceImpl;
import io.seata.discovery.registry.RegistryFactory;
import io.seata.discovery.registry.RegistryService;
import org.apache.commons.pool.KeyedPoolableObjectFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcClientChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClientChannelManager.class);
    private final ConcurrentMap<String, SeataChannel> channels = new ConcurrentHashMap<>();

    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<>();

    private Function<String, RpcChannelPoolKey> poolKeyFunction;
    private final ConcurrentMap<String, RpcChannelPoolKey> poolKeyMap = new ConcurrentHashMap<>();
    private final GenericKeyedObjectPool<RpcChannelPoolKey, SeataChannel> grpcClientKeyPool;

    public GrpcClientChannelManager(KeyedPoolableObjectFactory<RpcChannelPoolKey, SeataChannel> poolableObjectFactory,
                                    Function<String, RpcChannelPoolKey> poolKeyFunction, final GrpcClientConfig clientConfig) {
        grpcClientKeyPool = new GenericKeyedObjectPool<>(poolableObjectFactory);
        grpcClientKeyPool.setConfig(getGrpcPoolConfig(clientConfig));
        this.poolKeyFunction = poolKeyFunction;
    }

    private GenericKeyedObjectPool.Config getGrpcPoolConfig(final GrpcClientConfig clientConfig) {
        GenericKeyedObjectPool.Config poolConfig = new GenericKeyedObjectPool.Config();
        poolConfig.maxActive = clientConfig.getMaxPoolActive();
        poolConfig.minIdle = clientConfig.getMinPoolIdle();
        poolConfig.maxWait = clientConfig.getMaxAcquireConnMills();
        poolConfig.testOnBorrow = clientConfig.isPoolTestBorrow();
        poolConfig.testOnReturn = clientConfig.isPoolTestReturn();
        poolConfig.lifo = clientConfig.isPoolLifo();
        return poolConfig;
    }

    void registerChannel(final String serverAddress, final SeataChannel channel) {
        SeataChannel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return;
        }
        channels.put(serverAddress, channel);
    }

    public SeataChannel acquireChannel(String serverAddress) {
        SeataChannel channelToServer = channels.get(serverAddress);
        if (channelToServer != null) {
            channelToServer = getExistAliveChannel(channelToServer, serverAddress);
            if (channelToServer != null) {
                return channelToServer;
            }
        }
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("[GRPC]will connect to {}", serverAddress);
        }
        Object lockObj = CollectionUtils.computeIfAbsent(channelLocks, serverAddress, key -> new Object());
        synchronized (lockObj) {
            return doConnect(serverAddress);
        }
    }

    private SeataChannel doConnect(String serverAddress) {
        SeataChannel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        SeataChannel channel;
        try {
            RpcChannelPoolKey currentPoolKey = poolKeyFunction.apply(serverAddress);
            if (currentPoolKey.getMessage() instanceof RegisterTMRequest) {
                poolKeyMap.put(serverAddress, currentPoolKey);
            } else {
                RpcChannelPoolKey previousPoolKey = poolKeyMap.putIfAbsent(serverAddress, currentPoolKey);
                if (previousPoolKey != null && previousPoolKey.getMessage() instanceof RegisterRMRequest) {
                    RegisterRMRequest registerRMRequest = (RegisterRMRequest) currentPoolKey.getMessage();
                    ((RegisterRMRequest) previousPoolKey.getMessage()).setResourceIds(registerRMRequest.getResourceIds());
                }
            }

            channel = grpcClientKeyPool.borrowObject(currentPoolKey);
            channels.put(serverAddress, channel);
        } catch (Exception exx) {
            LOGGER.error("{} register RM failed.", FrameworkErrorCode.RegisterRM.getErrCode(), exx);
            throw new FrameworkException("can not register RM,err:" + exx.getMessage());
        }
        return channel;
    }

    private SeataChannel getExistAliveChannel(SeataChannel rmChannel, String serverAddress) {
        if (rmChannel.isActive()) {
            return rmChannel;
        } else {
            int i = 0;
            for (; i < GrpcClientConfig.getMaxCheckAliveRetry(); i++) {
                try {
                    Thread.sleep(GrpcClientConfig.getCheckAliveInterval());
                } catch (InterruptedException exx) {
                    LOGGER.error(exx.getMessage());
                }
                rmChannel = channels.get(serverAddress);
                if (rmChannel != null && rmChannel.isActive()) {
                    return rmChannel;
                }
            }
            if (i == GrpcClientConfig.getMaxCheckAliveRetry()) {
                LOGGER.warn("channel {} is not active after long wait, close it.", rmChannel);
                releaseChannel(rmChannel, serverAddress);
                return null;
            }
        }
        return null;
    }

    private List<String> getAvailServerList(String transactionServiceGroup) throws Exception {
        List<InetSocketAddress> availInetSocketAddressList = RegistryFactory.getInstance()
                .lookup(transactionServiceGroup);
        if (CollectionUtils.isEmpty(availInetSocketAddressList)) {
            return Collections.emptyList();
        }

        return availInetSocketAddressList.stream()
                .map(address -> NetUtil.toStringAddress(new InetSocketAddress(address.getAddress().getHostAddress(), address.getPort() + DefaultValues.GRPC_SERVICE_PORT_OFFSET)))
                .collect(Collectors.toList());
    }

    /**
     * Release channel to pool if necessary.
     *
     * @param channel       channel
     * @param serverAddress server address
     */
    void releaseChannel(SeataChannel channel, String serverAddress) {
        if (channel == null || serverAddress == null) {
            return;
        }
        try {
            synchronized (channelLocks.get(serverAddress)) {
                SeataChannel ch = channels.get(serverAddress);
                if (ch == null) {
                    return;
                }
                if (ch.equals(channel)) {
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("return to pool, rm channel:{}", channel);
                    }
                    destroyChannel(serverAddress, channel);
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
     * @param channel       channel
     */
    void destroyChannel(String serverAddress, SeataChannel channel) {
        if (channel == null) {
            return;
        }
        try {
            if (channel.equals(channels.get(serverAddress))) {
                channels.remove(serverAddress);
            }
        } catch (Exception exx) {
            LOGGER.error("return channel to rmPool error:{}", exx.getMessage());
        }
    }

    /**
     * Get all channels registered on current Rpc Client.
     *
     * @return channels
     */
    ConcurrentMap<String, SeataChannel> getChannels() {
        return channels;
    }

    /**
     * Reconnect to remote server of current transaction service group.
     *
     * @param transactionServiceGroup transaction service group
     */
    void reconnect(String transactionServiceGroup) {
        List<String> availList;
        try {
            availList = getAvailServerList(transactionServiceGroup);
        } catch (Exception e) {
            LOGGER.error("Failed to get available servers: {}", e.getMessage(), e);
            return;
        }
        if (CollectionUtils.isEmpty(availList)) {
            RegistryService registryService = RegistryFactory.getInstance();
            String clusterName = registryService.getServiceGroup(transactionServiceGroup);

            if (StringUtils.isBlank(clusterName)) {
                LOGGER.error("can not get cluster name in registry config '{}{}', please make sure registry config correct",
                        ConfigurationKeys.SERVICE_GROUP_MAPPING_PREFIX,
                        transactionServiceGroup);
                return;
            }

            if (!(registryService instanceof FileRegistryServiceImpl)) {
                LOGGER.error("no available service found in cluster '{}', please make sure registry config correct and keep your seata server running", clusterName);
            }
            return;
        }
        Set<String> channelAddress = new HashSet<>(availList.size());
        try {
            for (String serverAddress : availList) {
                try {
                    acquireChannel(serverAddress);
                    channelAddress.add(serverAddress);
                } catch (Exception e) {
                    LOGGER.error("{} can not connect to {} cause:{}", FrameworkErrorCode.NetConnect.getErrCode(),
                            serverAddress, e.getMessage(), e);
                }
            }
        } finally {
            if (CollectionUtils.isNotEmpty(channelAddress)) {
                List<InetSocketAddress> aliveAddress = new ArrayList<>(channelAddress.size());
                for (String address : channelAddress) {
                    String[] array = address.split(Constants.CLIENT_ID_SPLIT_CHAR);
                    aliveAddress.add(new InetSocketAddress(array[0], Integer.parseInt(array[1])));
                }
                RegistryFactory.getInstance().refreshAliveLookup(transactionServiceGroup, aliveAddress);
            } else {
                RegistryFactory.getInstance().refreshAliveLookup(transactionServiceGroup, Collections.emptyList());
            }
        }
    }
}
