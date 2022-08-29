package io.seata.core.rpc;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.Constants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.protocol.IncompatibleVersionException;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;

/**
 * @author goodboycoder
 */
public class SeataChannelServerManager {
    private static final Map<RpcType, ServerChannelManager> SERVER_CHANNEL_MANAGER_MAP = new HashMap<>();

    /**
     * resourceId -> clientId -> rpcType
     */
    private static final Map<String, Map<String, RpcType>> RM_RPC_TYPE_MAP = new ConcurrentHashMap<>();

    public static void register(RpcType rpcType, ServerChannelManager manager) {
        SERVER_CHANNEL_MANAGER_MAP.put(rpcType, manager);
    }

    public static ServerChannelManager getServerManager(RpcType rpcType) {
        if (!SERVER_CHANNEL_MANAGER_MAP.containsKey(rpcType)) {
            throw new ShouldNeverHappenException("no server channel manager for rpcType:" + rpcType.name);
        }
        return SERVER_CHANNEL_MANAGER_MAP.get(rpcType);
    }

    public static Map<String, SeataChannel> getAllRmChannels() {
        Map<String, SeataChannel> resultMap = new HashMap<>();
        for (Map.Entry<RpcType, ServerChannelManager> managerEntry : SERVER_CHANNEL_MANAGER_MAP.entrySet()) {
            ServerChannelManager channelManager = managerEntry.getValue();
            resultMap.putAll(channelManager.getRmChannels());
        }
        return resultMap;
    }

    public static void registerRMChannel(RegisterRMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        if (StringUtils.isNotBlank(request.getResourceIds())) {
            Set<String> resourceIds = new HashSet<>(Arrays.asList(request.getResourceIds().split(Constants.DBKEYS_SPLIT_CHAR)));
            String clientId = request.getApplicationId() + Constants.CLIENT_ID_SPLIT_CHAR + SeataChannelUtil.getAddressFromChannel(channel);
            for (String resourceId : resourceIds) {
                Map<String, RpcType> clientIdMap = CollectionUtils.computeIfAbsent(RM_RPC_TYPE_MAP, resourceId, key -> new ConcurrentHashMap<>());
                clientIdMap.put(clientId, channel.getType());
            }
        }
        getServerManager(channel.getType()).registerRMChannel(request, channel);
    }

    public static void registerTMChannel(RegisterTMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        getServerManager(channel.getType()).registerTMChannel(request, channel);
    }

    public static RpcType getRpcType(String resourceId, String clientId) {
        RpcType rpcType;
        Map<String, RpcType> resourceIdMap = RM_RPC_TYPE_MAP.get(resourceId);
        if (null == resourceIdMap || null == (rpcType = resourceIdMap.get(clientId))) {
            throw new RuntimeException("unknown resourceId/clientId, the corresponding channel may not be registered. resourceId[" + resourceId + "] clientId[" + clientId + "]" );
        }
        return rpcType;
    }

    public static boolean isRegistered(SeataChannel channel) {
        return getServerManager(channel.getType()).isRegistered(channel);
    }

    public static RpcContext getContextFromIdentified(SeataChannel channel) {
        return getServerManager(channel.getType()).getContextFromIdentified(channel);
    }

    public static void releaseRpcContext(SeataChannel channel) {
        getServerManager(channel.getType()).releaseRpcContext(channel);
    }
}
