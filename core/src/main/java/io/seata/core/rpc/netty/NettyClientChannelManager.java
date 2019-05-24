package io.seata.core.rpc.netty;

import io.netty.channel.Channel;
import io.seata.common.exception.FrameworkErrorCode;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.NetUtil;
import io.seata.discovery.registry.RegistryFactory;
import org.apache.commons.pool.impl.GenericKeyedObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * Netty client pool manager.
 *
 * @author jimin.jm @alibaba-inc.com
 * @author zhaojun
 */
public class NettyClientChannelManager {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(NettyClientChannelManager.class);
    
    private final ConcurrentMap<String, Object> channelLocks = new ConcurrentHashMap<>();
    
    private final ConcurrentMap<String, NettyPoolKey> poolKeyMap = new ConcurrentHashMap<>();
    
    private final ConcurrentMap<String, Channel> channels = new ConcurrentHashMap<>();
    
    private final GenericKeyedObjectPool<NettyPoolKey, Channel> nettyClientKeyPool;
    
    private Function<String, NettyPoolKey> poolKeyFunction;
    
    public NettyClientChannelManager(final NettyPoolableFactory keyPoolableFactory, final Function<String, NettyPoolKey> poolKeyFunction,
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
    
    public ConcurrentMap<String, Channel> getChannels() {
        return channels;
    }
    
    public Channel acquireChannel(String serverAddress) {
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
        synchronized (channelLocks.get(serverAddress)) {
            return doConnect(serverAddress);
        }
    }
    
    public void releaseChannel(Channel channel, String serverAddress) {
        if (null == channel || null == serverAddress) { return; }
        try {
            synchronized (channelLocks.get(serverAddress)) {
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
    
    public void reconnect(String transactionServiceGroup) {
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
                acquireChannel(serverAddress);
            } catch (Exception e) {
                LOGGER.error(FrameworkErrorCode.NetConnect.getErrCode(),
                    "can not connect to " + serverAddress + " cause:" + e.getMessage(), e);
            }
        }
    }
    
    private Channel doConnect(String serverAddress) {
        Channel channelToServer = channels.get(serverAddress);
        if (channelToServer != null && channelToServer.isActive()) {
            return channelToServer;
        }
        Channel channelFromPool;
        try {
            poolKeyMap.put(serverAddress, poolKeyFunction.apply(serverAddress));
            channelFromPool = nettyClientKeyPool.borrowObject(poolKeyMap.get(serverAddress));
        } catch (Exception exx) {
            LOGGER.error(FrameworkErrorCode.RegisterRM.getErrCode(), "register RM failed.", exx);
            throw new FrameworkException("can not register RM,err:" + exx.getMessage());
        }
        return channelFromPool;
    }
    
    private List<String> getAvailServerList(String transactionServiceGroup) throws Exception {
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
    
    public void invalidateObject(final String serverAddress, final Channel channel) throws Exception {
        nettyClientKeyPool.invalidateObject(poolKeyMap.get(serverAddress), channel);
    }
    
    public void registerChannel(final String serverAddress, final Channel channel) {
        if (null != channels.get(serverAddress) && channels.get(serverAddress).isActive()) {
            return;
        }
        channels.put(serverAddress, channel);
    }
}
