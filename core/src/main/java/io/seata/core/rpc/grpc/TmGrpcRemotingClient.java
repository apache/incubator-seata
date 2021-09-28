package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.seata.core.protocol.grpc.TransactionManagerServiceGrpc;
import io.seata.core.protocol.grpc.TransactionManagerServiceGrpc.TransactionManagerServiceBlockingStub;

import java.net.InetSocketAddress;

/**
 * @author xilou31
 **/
public class TmGrpcRemotingClient {
    private static TransactionManagerServiceBlockingStub stub;
    private static Channel channel;

    private static volatile RmGrpcRemotingClient instance;

    public static void init(String serverAddress, Integer port) {
        channel = GrpcClientBootstrap.getNewChannel(new InetSocketAddress(serverAddress, port));
        stub = TransactionManagerServiceGrpc.newBlockingStub(channel);
    }

    public TransactionManagerServiceBlockingStub getInstance() {
        if (stub != null) {
            return stub;
        }
        stub = TransactionManagerServiceGrpc.newBlockingStub(channel);
        return stub;
    }
}
