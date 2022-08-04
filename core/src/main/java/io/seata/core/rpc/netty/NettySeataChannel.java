package io.seata.core.rpc.netty;

import java.net.SocketAddress;

import io.netty.channel.Channel;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.SeataChannel;

/**
 * @author goodboycoder
 */
public class NettySeataChannel implements SeataChannel {
    private final Channel channel;

    public NettySeataChannel(Channel channel) {
        this.channel = channel;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public RpcType getType() {
        return RpcType.NETTY;
    }

    @Override
    public Object originChannel() {
        return channel;
    }

    @Override
    public SocketAddress remoteAddress() {
        return channel.remoteAddress();
    }
}
