package io.seata.core.rpc.grpc;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.seata.core.rpc.RemotingBootstrap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * @author xilou31
 **/
public class GrpcServerBootstrap implements RemotingBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerBootstrap.class);
    private GrpcServerConfig serverConfig;
    private Server server;

    @Override
    public void start() {
        try {
            if (serverConfig == null) {
                serverConfig = new GrpcServerConfig();
            }
            int port = serverConfig.getPort();
            server = ServerBuilder.forPort(port)
                    .addService(new ResourceManagerService())
                    .addService(new TransactionManagerService())
                    .addService(new BranchTransactionService())
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
