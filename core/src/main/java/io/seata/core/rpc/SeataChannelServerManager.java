package io.seata.core.rpc;

import java.util.Map;

import io.seata.core.protocol.IncompatibleVersionException;
import io.seata.core.protocol.RegisterRMRequest;
import io.seata.core.protocol.RegisterTMRequest;
import io.seata.core.rpc.netty.ChannelManager;

/**
 * @author goodboycoder
 */
public class SeataChannelServerManager {

    public static Map<String, SeataChannel> getAllRmChannels() {
        return ChannelManager.getRmChannels();
    }

    public static void registerRMChannel(RegisterRMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        if (RpcType.NETTY == channel.getType()) {
            ChannelManager.registerRMChannel(request, channel);
        }
    }

    public static void registerTMChannel(RegisterTMRequest request, SeataChannel channel) throws IncompatibleVersionException {
        if (RpcType.NETTY == channel.getType()) {
            ChannelManager.registerTMChannel(request, channel);
        }
    }

    public static boolean isRegistered(SeataChannel channel) {
        if (RpcType.NETTY == channel.getType()) {
            return ChannelManager.isRegistered(channel);
        }
        return false;
    }

    public static RpcContext getContextFromIdentified(SeataChannel channel) {
        if (RpcType.NETTY == channel.getType()) {
            return ChannelManager.getContextFromIdentified(channel);
        }
        return null;
    }
}
