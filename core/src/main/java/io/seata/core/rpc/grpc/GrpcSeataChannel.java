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

import java.net.SocketAddress;
import java.util.Objects;

import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.stub.ServerCallStreamObserver;
import io.grpc.stub.StreamObserver;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;

/**
 * @author goodboycoder
 */
public class GrpcSeataChannel implements SeataChannel {
    private final Channel channel;

    private final StreamObserver streamObserver;

    private final String connectionId;

    public GrpcSeataChannel(String connectionId, Channel channel, StreamObserver streamObserver) {
        this.connectionId = connectionId;
        this.channel = channel;
        this.streamObserver = streamObserver;
    }

    @Override
    public String getId() {
        return this.connectionId;
    }

    @Override
    public RpcType getType() {
        return RpcType.GRPC;
    }

    @Override
    public Object originChannel() {
        return this.channel;
    }

    @Override
    public SocketAddress remoteAddress() {
        return this.channel.remoteAddress();
    }

    @Override
    public boolean isActive() {
        boolean streamIsReady = true;
        if (null != streamObserver && streamObserver instanceof ServerCallStreamObserver) {
            streamIsReady = ((ServerCallStreamObserver<?>) streamObserver).isReady();
        }
        return channel.isActive() && streamIsReady;
    }

    @Override
    public void sendMsg(Object msg) {
        synchronized (streamObserver) {
            if (!(msg instanceof GrpcRemoting.BiStreamMessage)) {
                throw new IllegalArgumentException("[GRPC]not supported message type: " + msg.getClass());
            }
            streamObserver.onNext(msg);
        }
    }

    @Override
    public void close() {
        if (streamObserver instanceof ServerCallStreamObserver) {
            ServerCallStreamObserver<?> serverCallStreamObserver = ((ServerCallStreamObserver<?>) streamObserver);
            if (!serverCallStreamObserver.isCancelled()) {
                serverCallStreamObserver.onCompleted();
            }
        }
        channel.close();
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GrpcSeataChannel)) {
            return false;
        }
        GrpcSeataChannel that = (GrpcSeataChannel) o;
        return Objects.equals(channel, that.channel) && Objects.equals(connectionId, that.connectionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(channel, connectionId);
    }
}
