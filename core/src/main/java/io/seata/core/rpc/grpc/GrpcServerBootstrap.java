package io.seata.core.rpc.grpc;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerTransportFilter;
import io.grpc.util.MutableHandlerRegistry;
import io.seata.common.util.CollectionUtils;
import io.seata.core.rpc.RemotingBootstrap;
import io.seata.core.rpc.grpc.filter.ServerBaseTransportFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcServerBootstrap implements RemotingBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcServerBootstrap.class);

    private Server server;
    private ExecutorService messageExecutor;
    private int listenPort;

    private MutableHandlerRegistry mutableHandlerRegistry;
    private final GrpcServerConfig grpcServerConfig;

    private final List<ServerTransportFilter> transportFilters = new CopyOnWriteArrayList<>();

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public GrpcServerBootstrap(MutableHandlerRegistry mutableHandlerRegistry, GrpcServerConfig grpcServerConfig) {
        this.mutableHandlerRegistry = mutableHandlerRegistry;
        this.grpcServerConfig = grpcServerConfig;
        addServerTransportFilter(new ServerBaseTransportFilter());
    }

    public int getListenPort() {
        if (listenPort <= 0) {
            listenPort = grpcServerConfig.getListenPort();
        }
        return listenPort;
    }

    public void addServerTransportFilter(ServerTransportFilter filter) {
        if (!transportFilters.contains(filter)) {
            transportFilters.add(filter);
        }
    }


    @Override
    public void start() {
        if (initialized.get()) {
            throw new IllegalStateException("Grpc server has started");
        }
        ServerBuilder<?> serverBuilder = ServerBuilder.forPort(getListenPort())
                .fallbackHandlerRegistry(mutableHandlerRegistry);
        if (CollectionUtils.isNotEmpty(transportFilters)) {
            transportFilters.forEach(serverBuilder::addTransportFilter);
        }

        this.server = serverBuilder.build();
        try {
            this.server.start();
            this.server.awaitTermination();
            LOGGER.info("Grpc server started, service listen port: {}", getListenPort());

            // RegistryFactory register service
            initialized.set(true);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Server start failed", e);
        }
    }

    @Override
    public void shutdown() {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Shutting grpc server down, the listen port: {}", getListenPort());
        }
        if (initialized.get() && null != this.server) {
            this.server.shutdown();
        }

        if (null != this.messageExecutor) {
            this.messageExecutor.shutdown();
        }
    }
}
