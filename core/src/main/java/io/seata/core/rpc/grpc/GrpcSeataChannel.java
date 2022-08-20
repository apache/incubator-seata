package io.seata.core.rpc.grpc;

import java.net.SocketAddress;

import com.google.protobuf.Message;
import io.grpc.netty.shaded.io.netty.channel.Channel;
import io.grpc.stub.StreamObserver;
import io.seata.core.rpc.RpcType;
import io.seata.core.rpc.SeataChannel;

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
    public void sendMsg(Object msg) throws Exception{
        synchronized (streamObserver) {
            if (!(msg instanceof Message)) {
                throw new IllegalArgumentException("not supported message type: " + msg.getClass());
            }
            Message toSend = (Message) msg;
            streamObserver.onNext(toSend);
        }
    }
}
