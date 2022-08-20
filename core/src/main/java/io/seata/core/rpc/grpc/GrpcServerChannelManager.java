package io.seata.core.rpc.grpc;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.seata.common.Constants;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.protocol.IncompatibleVersionException;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.Version;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.SeataChannelUtil;
import io.seata.core.rpc.netty.NettyPoolKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcServerChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerChannelManager.class);

    /**
     * connectionId -> rpcContext
     */
    private static final ConcurrentHashMap<SeataChannel, RpcContext> IDENTIFIED_CHANNELS = new ConcurrentHashMap<>();

    /**
     * resourceId -> applicationId -> ip -> port -> connectionId
     */
    private static final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String,
            ConcurrentMap<Integer, RpcContext>>>> RM_CHANNELS = new ConcurrentHashMap<>();

    /**
     * applicationId+ip -> port -> connectionId
     */
    private static final ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> TM_CHANNELS
            = new ConcurrentHashMap<>();

    /**
     * Register tm channel.
     *
     * @param request the request
     * @param channel the channel
     * @throws IncompatibleVersionException the incompatible version exception
     */
    public void registerTMChannel(RegisterTMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        Version.checkVersion(request.getVersion());
        RpcContext rpcContext = buildChannelHolder(NettyPoolKey.TransactionRole.TMROLE, request.getVersion(),
                request.getApplicationId(),
                request.getTransactionServiceGroup(),
                null, channel);
        rpcContext.holdInIdentifiedChannels(IDENTIFIED_CHANNELS);
        IDENTIFIED_CHANNELS.put(channel, rpcContext);

        String clientIdentified = rpcContext.getApplicationId() + Constants.CLIENT_ID_SPLIT_CHAR
                + SeataChannelUtil.getClientIpFromChannel(channel);
        ConcurrentMap<Integer, RpcContext> clientIdentifiedMap = CollectionUtils.computeIfAbsent(TM_CHANNELS,
                clientIdentified, key -> new ConcurrentHashMap<>());
        rpcContext.holdInClientChannels(clientIdentifiedMap);
    }

    /**
     * Register rm channel.
     *
     * @param request the resource manager request
     * @param channel                the channel
     * @throws IncompatibleVersionException the incompatible  version exception
     */
    public void registerRMChannel(RegisterRMRequest request, SeataChannel channel)
            throws IncompatibleVersionException {
        Version.checkVersion(request.getVersion());
        Set<String> dbKeySet = dbKeyToSet(request.getResourceIds());
        RpcContext rpcContext;
        if (!IDENTIFIED_CHANNELS.containsKey(channel)) {
            rpcContext = buildChannelHolder(NettyPoolKey.TransactionRole.RMROLE, request.getVersion(),
                    request.getApplicationId(), request.getTransactionServiceGroup(),
                    request.getResourceIds(), channel);
//            rpcContext.holdInIdentifiedChannels(IDENTIFIED_CHANNELS);
        } else {
            rpcContext = IDENTIFIED_CHANNELS.get(channel);
            rpcContext.addResources(dbKeySet);
        }

        if (CollectionUtils.isEmpty(dbKeySet)) { return; }
        for (String resourceId : dbKeySet) {
            String clientIp;
            ConcurrentMap<Integer, RpcContext> portMap = CollectionUtils.computeIfAbsent(RM_CHANNELS, resourceId, key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(request.getApplicationId(), key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(clientIp = SeataChannelUtil.getClientIpFromChannel(channel), key -> new ConcurrentHashMap<>());

            rpcContext.holdInResourceManagerChannels(resourceId, portMap);
//            updateChannelsResource(resourceId, clientIp, request.getApplicationId());
        }
    }

    public void unRegister(String connectionId) {

    }

    private RpcContext buildChannelHolder(NettyPoolKey.TransactionRole clientRole, String version, String applicationId,
                                                 String txServiceGroup, String dbkeys, SeataChannel channel) {
        RpcContext holder = new RpcContext();
        holder.setClientRole(clientRole);
        holder.setVersion(version);
        holder.setClientId(buildClientId(applicationId, channel));
        holder.setApplicationId(applicationId);
        holder.setTransactionServiceGroup(txServiceGroup);
        holder.addResources(dbKeyToSet(dbkeys));
//        holder.setChannel(channel);
        return holder;
    }

    private String buildClientId(String applicationId, SeataChannel channel) {
        return applicationId + Constants.CLIENT_ID_SPLIT_CHAR + SeataChannelUtil.getAddressFromChannel(channel);
    }

    private static Set<String> dbKeyToSet(String dbKey) {
        if (StringUtils.isNullOrEmpty(dbKey)) {
            return null;
        }
        return new HashSet<>(Arrays.asList(dbKey.split(Constants.DBKEYS_SPLIT_CHAR)));
    }
}
