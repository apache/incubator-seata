package io.seata.core.rpc.grpc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import io.grpc.ServerInterceptor;
import io.grpc.util.MutableHandlerRegistry;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingBootstrap;
import io.seata.core.rpc.grpc.interceptor.ServerChannelInterceptor;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcRemotingServer implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRemotingServer.class);

    private final RemotingBootstrap grpcServerBootstrap;
    private final MutableHandlerRegistry mutableHandlerRegistry;
    private final List<ServerInterceptor> serverInterceptors = new CopyOnWriteArrayList<>();

    public GrpcRemotingServer() {
        this.mutableHandlerRegistry = new MutableHandlerRegistry();
        this.grpcServerBootstrap = new GrpcServerBootstrap(this.mutableHandlerRegistry);

        //register server interceptor
        registerDefaultInterceptor();
    }

    public void init() {
        grpcServerBootstrap.start();
    }

    @Override
    public void destroy() {
        grpcServerBootstrap.shutdown();
    }

    public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {

    }

    private void registerDefaultInterceptor() {
        this.serverInterceptors.add(new ServerChannelInterceptor());
    }
}
