package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.grpc.stub.StreamObserver;
import io.seata.core.model.grpc.ResourceManagerServiceGrpc;
import io.seata.core.model.grpc.ResourceManagerServiceGrpc.ResourceManagerServiceBlockingStub;
import io.seata.core.model.grpc.ResourceManagerServiceGrpc.ResourceManagerServiceStub;
import io.seata.core.model.grpc.SeataGrpc.BranchCommitResponse;
import io.seata.core.model.grpc.SeataGrpc.BranchRollbackResponse;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author xilou31
 **/
public class RmGrpcRemotingClient {
    private static ResourceManagerServiceStub asyncStub;
    private static ResourceManagerServiceBlockingStub syncStub;
    private static Channel channel;
    private static ConcurrentHashMap<String, StreamObserver<BranchCommitResponse>> commitMap = new ConcurrentHashMap<>();
    private static ConcurrentHashMap<String, StreamObserver<BranchRollbackResponse>> rollbackMap = new ConcurrentHashMap<>();

    public static void init(String serverAddress, Integer port) {
        channel = GrpcRemotingServer.getChannel(new InetSocketAddress(serverAddress, port));
        asyncStub = ResourceManagerServiceGrpc.newStub(channel);
        syncStub = ResourceManagerServiceGrpc.newBlockingStub(channel);
    }

    public static ResourceManagerServiceStub getAsyncStub() {
        if (asyncStub != null) {
            return asyncStub;
        }
        return ResourceManagerServiceGrpc.newStub(channel);
    }

    public static ResourceManagerServiceBlockingStub getSyncStub() {
        if (syncStub != null) {
            return syncStub;
        }
        return ResourceManagerServiceGrpc.newBlockingStub(channel);
    }

    public static StreamObserver<BranchCommitResponse> getCommitObserver() {
        return commitMap.get(null);
    }

    public static StreamObserver<BranchRollbackResponse> getRollbackObserver() {
        return rollbackMap.get(null);
    }
}
