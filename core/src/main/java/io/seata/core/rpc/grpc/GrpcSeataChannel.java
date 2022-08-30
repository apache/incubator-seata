package io.seata.core.rpc.grpc;

import java.net.SocketAddress;
import java.util.Objects;

import io.grpc.netty.shaded.io.netty.channel.Channel;
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
        return channel.isActive();
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
        channel.close();
    }

    @Override
    public void disconnect() {
        channel.disconnect();
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
