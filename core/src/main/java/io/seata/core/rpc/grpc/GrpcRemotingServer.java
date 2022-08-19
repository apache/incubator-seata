package io.seata.core.rpc.grpc;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import io.grpc.ServerInterceptor;
import io.grpc.ServerInterceptors;
import io.grpc.util.MutableHandlerRegistry;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingBootstrap;
import io.seata.core.rpc.grpc.interceptor.ServerChannelInterceptor;
import io.seata.core.rpc.grpc.service.HealthCheckService;
import io.seata.core.rpc.grpc.service.ResourceManagerService;
import io.seata.core.rpc.grpc.service.TransactionManagerService;
import io.seata.core.rpc.processor.Pair;
import io.seata.core.rpc.processor.RemotingProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author goodboycoder
 */
public class GrpcRemotingServer extends AbstractGrpcRemoting implements Disposable {
    private static final Logger LOGGER = LoggerFactory.getLogger(GrpcRemotingServer.class);

    private final RemotingBootstrap grpcServerBootstrap;
    private final MutableHandlerRegistry mutableHandlerRegistry;
    private final List<ServerInterceptor> serverInterceptors = new CopyOnWriteArrayList<>();

    public GrpcRemotingServer(ThreadPoolExecutor messageExecutor) {
        super(messageExecutor);
        this.mutableHandlerRegistry = new MutableHandlerRegistry();
        this.grpcServerBootstrap = new GrpcServerBootstrap(this.mutableHandlerRegistry);

        //register server interceptor(must before the service register)
        registerDefaultInterceptor();
        //register default service
        registerDefaultService();
    }

    public void init() {
        grpcServerBootstrap.start();
    }

    @Override
    public void destroy() {
        grpcServerBootstrap.shutdown();
    }

    public void registerProcessor(int messageType, RemotingProcessor processor, ExecutorService executor) {
        Pair<RemotingProcessor, ExecutorService> pair = new Pair<>(processor, executor);
        this.processorTable.put(messageType, pair);
    }


    private void registerDefaultInterceptor() {
        this.serverInterceptors.add(new ServerChannelInterceptor());
    }

    private void registerDefaultService() {
        mutableHandlerRegistry.addService(ServerInterceptors.intercept(new TransactionManagerService(this), serverInterceptors));
        mutableHandlerRegistry.addService(ServerInterceptors.intercept(new ResourceManagerService(this), serverInterceptors));
        mutableHandlerRegistry.addService(ServerInterceptors.intercept(new HealthCheckService(this), serverInterceptors));
    }
}
