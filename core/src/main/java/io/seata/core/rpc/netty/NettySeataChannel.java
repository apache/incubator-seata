package io.seata.core.rpc.netty;

import java.net.SocketAddress;
import java.util.Objects;

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

    @Override
    public boolean isActive() {
        return channel.isActive();
    }

    @Override
    public void sendMsg(Object msg) {
        //do nothing
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NettySeataChannel)) {
            return false;
        }
        NettySeataChannel channel1 = (NettySeataChannel) o;
        return Objects.equals(channel, channel1.channel);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel);
    }
}
