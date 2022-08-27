package io.seata.core.rpc.grpc;

import java.net.SocketAddress;

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


    public GrpcClientSeataChannel(ManagedChannel managedChannel) {
        this.managedChannel = managedChannel;
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
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public void close() {

    }

    @Override
    public void disconnect() {

    }

    @Override
    public void sendMsg(Object msg) {
        if (!(msg instanceof GrpcRemoting.BiStreamMessage)) {
            throw new IllegalArgumentException("[GRPC]not supported message type: " + msg.getClass());
        }
        streamObserver.onNext(msg);
    }
}
