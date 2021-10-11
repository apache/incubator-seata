package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.seata.core.model.grpc.TransactionManagerServiceGrpc;
import io.seata.core.model.grpc.TransactionManagerServiceGrpc.TransactionManagerServiceBlockingStub;

import java.net.InetSocketAddress;

/**
 * @author xilou31
 **/
public class TmGrpcRemotingClient {
    private static TransactionManagerServiceBlockingStub syncStub;
    private static Channel channel;

    public static void init(String serverAddress, Integer port) {
        channel = GrpcRemotingServer.getChannel(new InetSocketAddress(serverAddress, port));
        syncStub = TransactionManagerServiceGrpc.newBlockingStub(channel);
    }

    public static TransactionManagerServiceBlockingStub getSyncStub() {
        if (syncStub != null) {
            return syncStub;
        }
        return TransactionManagerServiceGrpc.newBlockingStub(channel);
    }
}
