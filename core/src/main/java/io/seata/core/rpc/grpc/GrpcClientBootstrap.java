package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

import java.net.InetSocketAddress;

/**
 * @author xilou31
 **/
public class GrpcClientBootstrap {
    public Channel getNewChannel(InetSocketAddress address) {
        ManagedChannel channel = null;
        channel = ManagedChannelBuilder.forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();
        return channel;
    }
}
