package io.seata.core.rpc;

import java.util.HashMap;
import java.util.Map;

import io.netty.channel.Channel;
import io.seata.common.util.CollectionUtils;
import io.seata.core.protocol.IncompatibleVersionException;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.rpc.netty.ChannelManager;
import io.seata.core.rpc.netty.NettySeataChannel;

/**
 * @author goodboycoder
 */
public class SeataChannelServerManager {

    public static Map<String, SeataChannel> getAllRmChannels() {
        Map<String, Channel> rmChannels = ChannelManager.getRmChannels();
        Map<String, SeataChannel> rmResultChannels = new HashMap<>();
        if (CollectionUtils.isNotEmpty(rmChannels)) {
            rmChannels.forEach((resourcesId, channel) -> rmResultChannels.put(resourcesId, new NettySeataChannel(channel)));
        }
        return rmResultChannels;
    }

    public static void registerRMChannel(RegisterRMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        if (RpcType.NETTY == channel.getType()) {
            ChannelManager.registerRMChannel(request, (Channel) channel.originChannel());
        }
    }

    public static void registerTMChannel(RegisterTMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        if (RpcType.NETTY == channel.getType()) {
            ChannelManager.registerTMChannel(request, (Channel) channel.originChannel());
        }
    }

    public static boolean isRegistered(SeataChannel channel) {
        if (RpcType.NETTY == channel.getType()) {
            return ChannelManager.isRegistered((Channel) channel.originChannel());
        }
        return false;
    }

    public static RpcContext getContextFromIdentified(SeataChannel channel) {
        if (RpcType.NETTY == channel.getType()) {
            return ChannelManager.getContextFromIdentified((Channel) channel.originChannel());
        }
        return null;
    }
}
