package io.seata.core.rpc.grpc;

import io.grpc.Channel;
import io.seata.core.model.TransactionManager;
import io.seata.core.model.grpc.TransactionManagerServiceGrpc;
import io.seata.core.model.grpc.TransactionManagerServiceGrpc.TransactionManagerServiceBlockingStub;

import java.net.InetSocketAddress;

/**
 * @author xilou31
 **/
public class TmGrpcRemotingClient {
    private static TransactionManagerServiceBlockingStub stub;
    private static Channel channel;
    private TransactionManager transactionManager;

    private static volatile RmGrpcRemotingClient instance;

    public static void init(String serverAddress, Integer port) {
        channel = GrpcClientBootstrap.getNewChannel(new InetSocketAddress(serverAddress, port));
        stub = TransactionManagerServiceGrpc.newBlockingStub(channel);
    }

    public static TransactionManagerServiceBlockingStub getInstance() {
        if (stub != null) {
            return stub;
        }
        stub = TransactionManagerServiceGrpc.newBlockingStub(channel);
        return stub;
    }

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }
}
