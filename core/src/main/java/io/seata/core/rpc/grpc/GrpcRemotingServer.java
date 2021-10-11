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
public class GrpcRemotingServer implements RemotingBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRemotingServer.class);
    private Server server;
    private static final ConcurrentHashMap<InetSocketAddress, Channel> IDENTIFIED_CHANNELS = new ConcurrentHashMap<>();
    private final Integer port;

    public GrpcRemotingServer(Integer port) {
        this.port = port;
    }


    public static Channel getChannel(InetSocketAddress address) {
        if (IDENTIFIED_CHANNELS.containsKey(address)) {
            return IDENTIFIED_CHANNELS.get(address);
        }
        ManagedChannel channel = null;
        channel = ManagedChannelBuilder.forAddress(address.getHostName(), address.getPort())
                .usePlaintext()
                .build();
        IDENTIFIED_CHANNELS.put(address, channel);
        return channel;
    }

    @Override
    public void start() {
        try {
            server = ServerBuilder.forPort(port)
                    .addService(new ResourceManagerService())
                    .addService(new TransactionManagerService())
                    .build()
                    .start();
            LOGGER.info("Seata Grpc Server Started,Listening On Port:" + port);
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
