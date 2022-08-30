package io.seata.core.rpc.grpc;

import java.net.SocketAddress;
import java.util.Objects;

import io.grpc.ManagedChannel;
import io.grpc.stub.StreamObserver;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.SeataChannel;
import io.seata.core.rpc.grpc.generated.GrpcRemoting;

/**
 * @author goodboycoder
 */
public class GrpcClientSeataChannel implements SeataChannel {
    private ManagedChannel managedChannel;

    private StreamObserver streamObserver;

    private SocketAddress remoteAddress;


    public GrpcClientSeataChannel(ManagedChannel managedChannel, SocketAddress remoteAddress) {
        this.managedChannel = managedChannel;
        this.remoteAddress = remoteAddress;
    }

    public StreamObserver getStreamObserver() {
        return streamObserver;
    }

    public void setStreamObserver(StreamObserver streamObserver) {
        this.streamObserver = streamObserver;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public RpcType getType() {
        return RpcType.GRPC;
    }

    @Override
    public Object originChannel() {
        return managedChannel;
    }

    @Override
    public SocketAddress remoteAddress() {
        return remoteAddress;
    }

    @Override
    public boolean isActive() {
        return !managedChannel.isTerminated() && !managedChannel.isShutdown();
    }

    @Override
    public void close() {
        if (streamObserver != null) {
            streamObserver.onCompleted();
        }
    }

    @Override
    public void disconnect() {
        if (null != managedChannel && !managedChannel.isShutdown()) {
            managedChannel.shutdown();
        }
    }

    @Override
    public void sendMsg(Object msg) {
        if (!(msg instanceof GrpcRemoting.BiStreamMessage)) {
            throw new IllegalArgumentException("[GRPC]not supported message type: " + msg.getClass());
        }
        streamObserver.onNext(msg);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof GrpcClientSeataChannel)) {
            return false;
        }
        GrpcClientSeataChannel that = (GrpcClientSeataChannel) o;
        return Objects.equals(managedChannel, that.managedChannel) && Objects.equals(remoteAddress, that.remoteAddress);
    }

    @Override
    public int hashCode() {
        return Objects.hash(managedChannel, remoteAddress);
    }
}
