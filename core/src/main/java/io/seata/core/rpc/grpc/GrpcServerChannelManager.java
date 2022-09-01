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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import io.seata.common.Constants;
import io.seata.common.exception.FrameworkException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.StringUtils;
import io.seata.core.protocol.IncompatibleVersionException;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.protocol.Version;
import io.seata.core.rpc.RpcChannelPoolKey;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.SeataChannelUtil;
import io.seata.core.rpc.ServerChannelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcServerChannelManager implements ServerChannelManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerChannelManager.class);

    private final Map<String, SeataChannel> channelMap = new ConcurrentHashMap<>();

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

    @Override
    public void registerTMChannel(RegisterTMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        Version.checkVersion(request.getVersion());
        RpcContext rpcContext = buildChannelHolder(RpcChannelPoolKey.TransactionRole.TMROLE, request.getVersion(),
                request.getApplicationId(),
                request.getTransactionServiceGroup(),
                null, channel);
        rpcContext.holdInIdentifiedChannels(IDENTIFIED_CHANNELS);
        IDENTIFIED_CHANNELS.put(channel, rpcContext);
        channelMap.put(channel.getId(), channel);

        String clientIdentified = rpcContext.getApplicationId() + Constants.CLIENT_ID_SPLIT_CHAR
                + SeataChannelUtil.getClientIpFromChannel(channel);
        ConcurrentMap<Integer, RpcContext> clientIdentifiedMap = CollectionUtils.computeIfAbsent(TM_CHANNELS,
                clientIdentified, key -> new ConcurrentHashMap<>());
        rpcContext.holdInClientChannels(clientIdentifiedMap);
    }

    @Override
    public void registerRMChannel(RegisterRMRequest request, SeataChannel channel)
            throws IncompatibleVersionException {
        Version.checkVersion(request.getVersion());
        Set<String> dbKeySet = dbKeyToSet(request.getResourceIds());
        RpcContext rpcContext;
        if (!IDENTIFIED_CHANNELS.containsKey(channel)) {
            rpcContext = buildChannelHolder(RpcChannelPoolKey.TransactionRole.RMROLE, request.getVersion(),
                    request.getApplicationId(), request.getTransactionServiceGroup(),
                    request.getResourceIds(), channel);
            rpcContext.holdInIdentifiedChannels(IDENTIFIED_CHANNELS);
        } else {
            rpcContext = IDENTIFIED_CHANNELS.get(channel);
            rpcContext.addResources(dbKeySet);
        }
        channelMap.put(channel.getId(), channel);

        if (CollectionUtils.isEmpty(dbKeySet)) {
            return;
        }
        for (String resourceId : dbKeySet) {
            String clientIp;
            ConcurrentMap<Integer, RpcContext> portMap = CollectionUtils.computeIfAbsent(RM_CHANNELS, resourceId, key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(request.getApplicationId(), key -> new ConcurrentHashMap<>())
                    .computeIfAbsent(clientIp = SeataChannelUtil.getClientIpFromChannel(channel), key -> new ConcurrentHashMap<>());
            rpcContext.holdInResourceManagerChannels(resourceId, portMap);
            updateChannelsResource(resourceId, clientIp, request.getApplicationId());
        }
    }

    private static void updateChannelsResource(String resourceId, String clientIp, String applicationId) {
        ConcurrentMap<Integer, RpcContext> sourcePortMap = RM_CHANNELS.get(resourceId).get(applicationId).get(clientIp);
        for (ConcurrentMap.Entry<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
                RpcContext>>>> rmChannelEntry : RM_CHANNELS.entrySet()) {
            if (rmChannelEntry.getKey().equals(resourceId)) {
                continue;
            }
            ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
                    RpcContext>>> applicationIdMap = rmChannelEntry.getValue();
            if (!applicationIdMap.containsKey(applicationId)) {
                continue;
            }
            ConcurrentMap<String, ConcurrentMap<Integer,
                    RpcContext>> clientIpMap = applicationIdMap.get(applicationId);
            if (!clientIpMap.containsKey(clientIp)) {
                continue;
            }
            ConcurrentMap<Integer, RpcContext> portMap = clientIpMap.get(clientIp);
            for (ConcurrentMap.Entry<Integer, RpcContext> portMapEntry : portMap.entrySet()) {
                Integer port = portMapEntry.getKey();
                if (!sourcePortMap.containsKey(port)) {
                    RpcContext rpcContext = portMapEntry.getValue();
                    sourcePortMap.put(port, rpcContext);
                    rpcContext.holdInResourceManagerChannels(resourceId, port);
                }
            }
        }
    }

    @Override
    public SeataChannel getChannel(String resourceId, String clientId) {
        SeataChannel resultChannel = null;

        String[] clientIdInfo = readClientId(clientId);

        if (clientIdInfo == null || clientIdInfo.length != 3) {
            throw new FrameworkException("Invalid Client ID: " + clientId);
        }

        String targetApplicationId = clientIdInfo[0];
        String targetIP = clientIdInfo[1];
        int targetPort = Integer.parseInt(clientIdInfo[2]);

        ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
                RpcContext>>> applicationIdMap = RM_CHANNELS.get(resourceId);

        if (targetApplicationId == null || applicationIdMap == null || applicationIdMap.isEmpty()) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("No channel is available for resource[{}]", resourceId);
            }
            return null;
        }

        ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> ipMap = applicationIdMap.get(targetApplicationId);

        if (ipMap != null && !ipMap.isEmpty()) {
            // Firstly, try to find the original channel through which the branch was registered.
            ConcurrentMap<Integer, RpcContext> portMapOnTargetIP = ipMap.get(targetIP);
            if (portMapOnTargetIP != null && !portMapOnTargetIP.isEmpty()) {
                RpcContext exactRpcContext = portMapOnTargetIP.get(targetPort);
                if (exactRpcContext != null) {
                    SeataChannel channel = exactRpcContext.getChannel();
                    if (channel.isActive()) {
                        resultChannel = channel;
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Just got exactly the one {} for {}", channel, clientId);
                        }
                    } else {
                        if (portMapOnTargetIP.remove(targetPort, exactRpcContext)) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Removed inactive {}", channel);
                            }
                        }
                    }
                }

                // The original channel was broken, try another one.
                if (resultChannel == null) {
                    for (ConcurrentMap.Entry<Integer, RpcContext> portMapOnTargetIPEntry : portMapOnTargetIP
                            .entrySet()) {
                        SeataChannel channel = portMapOnTargetIPEntry.getValue().getChannel();

                        if (channel.isActive()) {
                            resultChannel = channel;
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info(
                                        "Choose {} on the same IP[{}] as alternative of {}", channel, targetIP, clientId);
                            }
                            break;
                        } else {
                            if (portMapOnTargetIP.remove(portMapOnTargetIPEntry.getKey(),
                                    portMapOnTargetIPEntry.getValue())) {
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("Removed inactive {}", channel);
                                }
                            }
                        }
                    }
                }
            }

            // No channel on the this app node, try another one.
            if (resultChannel == null) {
                for (ConcurrentMap.Entry<String, ConcurrentMap<Integer, RpcContext>> ipMapEntry : ipMap
                        .entrySet()) {
                    if (ipMapEntry.getKey().equals(targetIP)) {
                        continue;
                    }

                    ConcurrentMap<Integer, RpcContext> portMapOnOtherIP = ipMapEntry.getValue();
                    if (portMapOnOtherIP == null || portMapOnOtherIP.isEmpty()) {
                        continue;
                    }

                    for (ConcurrentMap.Entry<Integer, RpcContext> portMapOnOtherIPEntry : portMapOnOtherIP.entrySet()) {
                        SeataChannel channel = portMapOnOtherIPEntry.getValue().getChannel();

                        if (channel.isActive()) {
                            resultChannel = channel;
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Choose {} on the same application[{}] as alternative of {}", channel, targetApplicationId, clientId);
                            }
                            break;
                        } else {
                            if (portMapOnOtherIP.remove(portMapOnOtherIPEntry.getKey(),
                                    portMapOnOtherIPEntry.getValue())) {
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("Removed inactive {}", channel);
                                }
                            }
                        }
                    }
                    if (resultChannel != null) {
                        break;
                    }
                }
            }
        }

        if (resultChannel == null) {
            resultChannel = tryOtherApp(applicationIdMap, targetApplicationId);

            if (resultChannel == null) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("No channel is available for resource[{}] as alternative of {}", resourceId, clientId);
                }
            } else {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Choose {} on the same resource[{}] as alternative of {}", resultChannel, resourceId, clientId);
                }
            }
        }

        return resultChannel;

    }

    private static SeataChannel tryOtherApp(ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
            RpcContext>>> applicationIdMap, String myApplicationId) {
        SeataChannel chosenChannel = null;
        for (ConcurrentMap.Entry<String, ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>>> applicationIdMapEntry : applicationIdMap
                .entrySet()) {
            if (!StringUtils.isNullOrEmpty(myApplicationId) && applicationIdMapEntry.getKey().equals(myApplicationId)) {
                continue;
            }

            ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> targetIPMap = applicationIdMapEntry.getValue();
            if (targetIPMap == null || targetIPMap.isEmpty()) {
                continue;
            }

            for (ConcurrentMap.Entry<String, ConcurrentMap<Integer, RpcContext>> targetIPMapEntry : targetIPMap
                    .entrySet()) {
                ConcurrentMap<Integer, RpcContext> portMap = targetIPMapEntry.getValue();
                if (portMap == null || portMap.isEmpty()) {
                    continue;
                }

                for (ConcurrentMap.Entry<Integer, RpcContext> portMapEntry : portMap.entrySet()) {
                    SeataChannel channel = portMapEntry.getValue().getChannel();
                    if (channel.isActive()) {
                        chosenChannel = channel;
                        break;
                    } else {
                        if (portMap.remove(portMapEntry.getKey(), portMapEntry.getValue())) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("Removed inactive {}", channel);
                            }
                        }
                    }
                }
                if (chosenChannel != null) {
                    break;
                }
            }
            if (chosenChannel != null) {
                break;
            }
        }
        return chosenChannel;

    }

    @Override
    public Map<String, SeataChannel> getRmChannels() {
        if (RM_CHANNELS.isEmpty()) {
            return null;
        }
        Map<String, SeataChannel> channels = new HashMap<>(RM_CHANNELS.size());
        RM_CHANNELS.forEach((resourceId, value) -> {
            SeataChannel channel = tryOtherApp(value, null);
            if (channel == null) {
                return;
            }
            channels.put(resourceId, channel);
        });
        return channels;
    }

    @Override
    public boolean isRegistered(SeataChannel channel) {
        return IDENTIFIED_CHANNELS.containsKey(channel);
    }

    @Override
    public RpcContext getContextFromIdentified(SeataChannel channel) {
        return IDENTIFIED_CHANNELS.get(channel);
    }

    @Override
    public void releaseRpcContext(SeataChannel channel) {
        RpcContext rpcContext = getContextFromIdentified(channel);
        if (rpcContext != null) {
            rpcContext.release();
        }
    }

    public void unregister(String connectionId) {
        SeataChannel seataChannel = channelMap.get(connectionId);
        if (null != seataChannel) {
            releaseRpcContext(seataChannel);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("remove unused channel, connectionId:{} channel:{}", connectionId, seataChannel);
            }
        }
    }

    private static String[] readClientId(String clientId) {
        return clientId.split(Constants.CLIENT_ID_SPLIT_CHAR);
    }

    private RpcContext buildChannelHolder(RpcChannelPoolKey.TransactionRole clientRole, String version, String applicationId,
                                          String txServiceGroup, String dbkeys, SeataChannel channel) {
        RpcContext holder = new RpcContext();
        holder.setClientRole(clientRole);
        holder.setVersion(version);
        holder.setClientId(buildClientId(applicationId, channel));
        holder.setApplicationId(applicationId);
        holder.setTransactionServiceGroup(txServiceGroup);
        holder.addResources(dbKeyToSet(dbkeys));
        holder.setChannel(channel);
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
