package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.seata.core.model.grpc.BranchTransactionServiceGrpc;
import io.seata.core.model.grpc.BranchTransactionServiceGrpc.BranchTransactionServiceBlockingStub;

import java.net.InetSocketAddress;

/**
 * @author xilou31
 **/
public class GrpcRemotingServer {
    private static BranchTransactionServiceBlockingStub stub;
    private static Channel channel;

    public static void init(String serverAddress, Integer port) {
        channel = GrpcServerBootstrap.getNewChannel(new InetSocketAddress(serverAddress, port));
        stub = BranchTransactionServiceGrpc.newBlockingStub(channel);
    }

    public static BranchTransactionServiceBlockingStub getInstance() {
        if (stub != null) {
            return stub;
        }
        stub = BranchTransactionServiceGrpc.newBlockingStub(channel);
        return stub;
    }
}
