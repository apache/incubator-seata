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

package com.alibaba.fescar.core.rpc;

import java.net.SocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.alibaba.fescar.common.Constants;
import com.alibaba.fescar.core.protocol.*;
import com.alibaba.fescar.core.protocol.RegisterRMRequest;
import com.alibaba.fescar.core.protocol.RegisterTMRequest;
import com.alibaba.fescar.core.rpc.netty.NettyPoolKey.TransactionRole;

import io.netty.channel.Channel;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type channel manager.
 *
 * @Author: jimin.jm @alibaba-inc.com
 * @Project: fescar-all
 * @DateTime: 2018 /12/07 10:50
 * @FileName: ChannelManager
 * @Description:
 */
public class ChannelManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChannelManager.class);
    private static final ConcurrentMap<Channel, RpcContext> IDENTIFIED_CHANNELS
        = new ConcurrentHashMap<Channel, RpcContext>();

    /**
     * dbkey+appname+ip port context
     */
    private static final ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
        RpcContext>>>>
        RM_CHANNELS
        = new ConcurrentHashMap<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
        RpcContext>>>>();

    /**
     * ip+appname,port
     */
    private static final ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> TM_CHANNELS
        = new ConcurrentHashMap<String, ConcurrentMap<Integer, RpcContext>>();

    private static final ConcurrentMap<String, String> DB_GROUP_MAPPING = new ConcurrentHashMap<String, String>();

    /**
     * Is registered boolean.
     *
     * @param channel the channel
     * @return the boolean
     */
    public static boolean isRegistered(Channel channel) {
        return IDENTIFIED_CHANNELS.containsKey(channel);
    }

    /**
     * Gets get role from channel.
     *
     * @param channel the channel
     * @return the get role from channel
     */
    public static TransactionRole getRoleFromChannel(Channel channel) {
        if (IDENTIFIED_CHANNELS.containsKey(channel)) {
            return IDENTIFIED_CHANNELS.get(channel).getClientRole();
        }
        return null;
    }

    /**
     * Gets get context from identified.
     *
     * @param channel the channel
     * @return the get context from identified
     */
    public static RpcContext getContextFromIdentified(Channel channel) {
        return IDENTIFIED_CHANNELS.get(channel);
    }

    private static String buildClientId(String applicationId, Channel channel) {
        return applicationId + Constants.CLIENT_ID_SPLIT_CHAR + getAddressFromChannel(channel);
    }

    private static RpcContext buildChannelHolder(TransactionRole clientRole, String version, String applicationId,
                                                 String txServiceGroup, String dbkeys, Channel channel) {
        RpcContext holder = new RpcContext();
        holder.setClientRole(clientRole);
        holder.setVersion(version);
        String clientId = buildClientId(applicationId, channel);
        holder.setApplicationId(applicationId);
        holder.setTransactionServiceGroup(txServiceGroup);
        holder.addResources(dbKeytoSet(dbkeys));
        holder.setChannel(channel);
        return holder;
    }

    /**
     * Register tm channel.
     *
     * @param request the request
     * @param channel the channel
     * @throws IncompatibleVersionException the incompatible version exception
     */
    public static void registerTMChannel(RegisterTMRequest request, Channel channel)
        throws IncompatibleVersionException {
        Version.checkVersion(request.getVersion());
        RpcContext rpcContext = buildChannelHolder(TransactionRole.TMROLE, request.getVersion(),
            request.getApplicationId(),
            request.getTransactionServiceGroup(),
            null, channel);
        rpcContext.holdInIdentifiedChannels(IDENTIFIED_CHANNELS);
        String clientIdentified = rpcContext.getApplicationId() + Constants.CLIENT_ID_SPLIT_CHAR
            + getClientIpFromChannel(channel);
        TM_CHANNELS.putIfAbsent(clientIdentified, new ConcurrentHashMap<Integer, RpcContext>());
        ConcurrentMap<Integer, RpcContext> clientIdentifiedMap = TM_CHANNELS.get(clientIdentified);
        rpcContext.holdInClientChannels(clientIdentifiedMap);
    }

    /**
     * Register rm channel.
     *
     * @param resourceManagerRequest the resource manager request
     * @param channel                the channel
     * @throws IncompatibleVersionException the incompatible  version exception
     */
    public static void registerRMChannel(RegisterRMRequest resourceManagerRequest, Channel channel)
        throws IncompatibleVersionException {
        Version.checkVersion(resourceManagerRequest.getVersion());
        Set<String> dbkeySet = dbKeytoSet(resourceManagerRequest.getResourceIds());
        RpcContext rpcContext;
        if (!IDENTIFIED_CHANNELS.containsKey(channel)) {
            rpcContext = buildChannelHolder(TransactionRole.RMROLE, resourceManagerRequest.getVersion(),
                resourceManagerRequest.getApplicationId(), resourceManagerRequest.getTransactionServiceGroup(),
                resourceManagerRequest.getResourceIds(), channel);
            rpcContext.holdInIdentifiedChannels(IDENTIFIED_CHANNELS);
        } else {
            rpcContext = IDENTIFIED_CHANNELS.get(channel);
            rpcContext.addResources(dbkeySet);
        }
        if (null == dbkeySet || dbkeySet.isEmpty()) { return; }
        for (String resourceId : dbkeySet) {
            RM_CHANNELS.putIfAbsent(resourceId,
                new ConcurrentHashMap<String, ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>>>());
            ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>>> applicationIdMap
                = RM_CHANNELS.get(resourceId);
            applicationIdMap.putIfAbsent(resourceManagerRequest.getApplicationId(),
                new ConcurrentHashMap<String, ConcurrentMap<Integer, RpcContext>>());
            ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> clientIpMap = applicationIdMap.get(
                resourceManagerRequest.getApplicationId());
            String clientIp = getClientIpFromChannel(channel);
            clientIpMap.putIfAbsent(clientIp, new ConcurrentHashMap<Integer, RpcContext>());
            ConcurrentMap<Integer, RpcContext> portMap = clientIpMap.get(clientIp);
            rpcContext.holdInResourceManagerChannels(resourceId, portMap);
            updateChannelsResource(resourceId, clientIp, resourceManagerRequest.getApplicationId());
        }

    }

    private static void updateChannelsResource(String resourceId, String clientIp, String applicationId) {
        ConcurrentMap<Integer, RpcContext> sourcePortMap = RM_CHANNELS.get(resourceId).get(applicationId).get(clientIp);
        for (ConcurrentMap.Entry<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
            RpcContext>>>> rmChannelEntry : RM_CHANNELS.entrySet()) {
            if (rmChannelEntry.getKey().equals(resourceId)) { continue; }
            ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
                RpcContext>>> applicationIdMap = rmChannelEntry.getValue();
            if (!applicationIdMap.containsKey(applicationId)) { continue; }
            ConcurrentMap<String, ConcurrentMap<Integer,
                RpcContext>> clientIpMap = applicationIdMap.get(applicationId);
            if (!clientIpMap.containsKey(clientIp)) { continue; }
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

    private static String getAddressFromChannel(Channel channel) {
        SocketAddress socketAddress = channel.remoteAddress();
        String address = socketAddress.toString();
        if (socketAddress.toString().indexOf(Constants.ENDPOINT_BEGIN_CHAR) == 0) {
            address = socketAddress.toString().substring(Constants.ENDPOINT_BEGIN_CHAR.length());
        }
        return address;
    }

    private static String getClientIpFromChannel(Channel channel) {
        String address = getAddressFromChannel(channel);
        String clientIp = address;
        if (clientIp.contains(Constants.IP_PORT_SPLIT_CHAR)) {
            clientIp = clientIp.substring(0, clientIp.lastIndexOf(Constants.IP_PORT_SPLIT_CHAR));
        }
        return clientIp;
    }

    private static Integer getClientPortFromChannel(Channel channel) {
        String address = getAddressFromChannel(channel);
        Integer port = 0;
        try {
            if (address.contains(Constants.IP_PORT_SPLIT_CHAR)) {
                port = Integer.parseInt(address.substring(address.lastIndexOf(Constants.IP_PORT_SPLIT_CHAR) + 1));
            }
        } catch (NumberFormatException exx) {
            LOGGER.error(exx.getMessage());
        }
        return port;
    }

    private static Set<String> dbKeytoSet(String dbkey) {
        if (StringUtils.isEmpty(dbkey)) {
            return null;
        }
        Set<String> set = new HashSet<String>();
        for (String s : dbkey.split(Constants.DBKEYS_SPLIT_CHAR)) {
            set.add(s);
        }
        return set;
    }

    /**
     * Release rpc context.
     *
     * @param channel the channel
     */
    public static void releaseRpcContext(Channel channel) {
        if (IDENTIFIED_CHANNELS.containsKey(channel)) {
            RpcContext rpcContext = getContextFromIdentified(channel);
            rpcContext.release();
        }
    }

    /**
     * Gets get same income client channel.
     *
     * @param channel the channel
     * @return the get same income client channel
     */
    public static Channel getSameClientChannel(Channel channel) {
        if (channel.isActive()) {
            return channel;
        }
        RpcContext rpcContext = getContextFromIdentified(channel);
        if (null == rpcContext) {
            LOGGER.error("rpcContext is null,channel:" + channel + ",active:" + channel.isActive());
            return null;
        }
        if (rpcContext.getChannel().isActive()) {
            //recheck
            return rpcContext.getChannel();
        }
        Integer clientPort = getClientPortFromChannel(channel);
        TransactionRole clientRole = rpcContext.getClientRole();
        if (clientRole == TransactionRole.TMROLE) {
            String clientIdentified = rpcContext.getApplicationId() + Constants.CLIENT_ID_SPLIT_CHAR
                + getClientIpFromChannel(channel);
            if (!TM_CHANNELS.containsKey(clientIdentified)) {
                return null;
            }
            ConcurrentMap<Integer, RpcContext> clientRpcMap = TM_CHANNELS.get(clientIdentified);
            return getChannelFromSameClientMap(clientRpcMap, clientPort);
        } else if (clientRole == TransactionRole.RMROLE) {
            for (Map<Integer, RpcContext> clientRmMap : rpcContext.getClientRMHolderMap().values()) {
                Channel sameClientChannel = getChannelFromSameClientMap(clientRmMap, clientPort);
                if (null != sameClientChannel) {
                    return sameClientChannel;
                }
            }
        }
        return null;

    }

    private static Channel getChannelFromSameClientMap(Map<Integer, RpcContext> clientChannelMap, int exclusivePort) {
        if (null != clientChannelMap && !clientChannelMap.isEmpty()) {
            for (ConcurrentMap.Entry<Integer, RpcContext> entry : clientChannelMap.entrySet()) {
                if (entry.getKey().intValue() == exclusivePort) {
                    clientChannelMap.remove(entry.getKey());
                    continue;
                }
                Channel channel = entry.getValue().getChannel();
                if (channel.isActive()) { return channel; }
                clientChannelMap.remove(entry.getKey());
            }
        }
        return null;
    }

    /**
     * Gets get channel.
     *
     * @param resourceId    the db key
     * @param clientIp      the client ip
     * @param clientAppName the client app name
     * @return the get channel
     */
    public static Channel getChannel(String resourceId, String clientIp, String clientAppName) {
        Channel resultChannel = null;
        ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
            RpcContext>>> applicationIdMap = RM_CHANNELS.get(resourceId);
        if (null != applicationIdMap && !applicationIdMap.isEmpty()) {
            ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> clientIpMap = applicationIdMap.get(clientAppName);
            if (null != clientIpMap && !clientIpMap.isEmpty()) {
                ConcurrentMap<Integer, RpcContext> portMap = clientIpMap.get(clientIp);
                if (null != portMap && !portMap.isEmpty()) {
                    for (ConcurrentMap.Entry<Integer, RpcContext> portMapEntry : portMap.entrySet()) {
                        Channel channel = portMapEntry.getValue().getChannel();
                        if (channel.isActive()) {
                            resultChannel = channel;
                            break;
                        }
                        portMap.remove(portMapEntry.getKey());
                    }
                }
                if (null == resultChannel) {
                    for (ConcurrentMap.Entry<String, ConcurrentMap<Integer, RpcContext>> clientIpMapEntry : clientIpMap
                        .entrySet()) {
                        if (clientIpMapEntry.getKey().equals(clientIp)) { continue; }
                        for (ConcurrentMap.Entry<Integer, RpcContext> portMapEntry : clientIpMapEntry.getValue()
                            .entrySet()) {
                            Channel channel = portMapEntry.getValue().getChannel();
                            if (channel.isActive()) {
                                resultChannel = channel;
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info(
                                        "use same appname:" + clientAppName + ",channel:" + channel + ",replace ip:"
                                            + clientIp);
                                }
                                break;
                            }
                            clientIpMapEntry.getValue().remove(portMapEntry.getKey());
                        }
                        if (null != resultChannel) {
                            break;
                        }
                    }
                }
            }
        }
        if (null == resultChannel) {
            for (ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer,
                RpcContext>>> appIdEntryMap : RM_CHANNELS.values()) {
                for (ConcurrentMap<String, ConcurrentMap<Integer,
                    RpcContext>> clientIpEntryMap : appIdEntryMap.values()) {
                    if (clientIpEntryMap.containsKey(clientIp)) {
                        for (ConcurrentMap.Entry<Integer, RpcContext> portMapEntry : clientIpEntryMap.get(clientIp)
                            .entrySet()) {
                            Channel channel = portMapEntry.getValue().getChannel();
                            if (channel.isActive()) {
                                resultChannel = channel;
                                if (LOGGER.isInfoEnabled()) {
                                    LOGGER.info("use another dbkey,channel:" + channel);
                                }
                                break;
                            }
                            clientIpEntryMap.get(clientIp).remove(portMapEntry.getKey());
                        }
                    }
                    if (null != resultChannel) {
                        break;
                    }
                }
                if (null != resultChannel) {
                    break;
                }
            }
        }

        return resultChannel;

    }
}
