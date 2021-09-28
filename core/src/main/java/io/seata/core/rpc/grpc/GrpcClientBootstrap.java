package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.seata.core.rpc.RemotingBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author xilou31
 **/
public class GrpcClientBootstrap implements RemotingBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcClientBootstrap.class);
    private GrpcClientConfig clientConfig;
    private Server server;
    private ConcurrentHashMap<InetSocketAddress, Channel> channelMap = new ConcurrentHashMap<>();

    public Channel getNewChannel(InetSocketAddress address) {
        if (channelMap.contains(address)) {
            return channelMap.get(address);
        }
        ManagedChannel channel = null;
        channel = ManagedChannelBuilder.forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();
        channelMap.put(address, channel);
        return channel;
    }

    @Override
    public void start() {
        try {
            if (clientConfig == null) {
                clientConfig = new GrpcClientConfig();
            }
            int port = clientConfig.getPort();
            server = ServerBuilder.forPort(port)
                    .addService(new BranchTransactionService())
                    .build()
                    .start();
            LOGGER.info("Seata Grpc Client Started,Listening On Port:" + port);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    @Override
    public void shutdown() {
        if (server != null) {
            try {
                server.shutdown()
                        .awaitTermination(30, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                exception.printStackTrace();
            }
        }
    }
}
