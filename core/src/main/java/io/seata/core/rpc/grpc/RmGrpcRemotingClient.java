package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.seata.core.model.ResourceManager;
import io.seata.core.model.grpc.ResourceManagerServiceGrpc;
import io.seata.core.model.grpc.ResourceManagerServiceGrpc.ResourceManagerServiceBlockingStub;

import java.net.InetSocketAddress;

/**
 * @author xilou31
 **/
public class RmGrpcRemotingClient {
    private static ResourceManagerServiceBlockingStub stub;
    private static Channel channel;
    private ResourceManager resourceManager;

    private static volatile RmGrpcRemotingClient instance;

    public static void init(String serverAddress, Integer port) {
        channel = GrpcClientBootstrap.getNewChannel(new InetSocketAddress(serverAddress, port));
        stub = ResourceManagerServiceGrpc.newBlockingStub(channel);
    }

    public static ResourceManagerServiceBlockingStub getInstance() {
        if (stub != null) {
            return stub;
        }
        stub = ResourceManagerServiceGrpc.newBlockingStub(channel);
        return stub;
    }

    public void setResourceManager(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
    }
}
