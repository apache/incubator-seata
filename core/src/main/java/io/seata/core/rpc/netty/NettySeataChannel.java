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
    public void close() {
        channel.disconnect();
        channel.close();
    }

    @Override
    public void sendMsg(Object msg) {
        channel.writeAndFlush(msg);
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
