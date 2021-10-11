package io.seata.core.rpc;

import io.seata.core.rpc.grpc.RmGrpcRemotingClient;
import io.seata.core.rpc.netty.RmNettyRemotingClient;

/**
 * @author xilou31
 **/
public class RmRemotingClient {
    RpcType rpcType;

    public static Object getInstance(RpcType framework) {
        if (framework == RpcType.NETTY) {
            return RmNettyRemotingClient.getInstance();
        }
        if (framework == RpcType.GRPC) {
            return RmGrpcRemotingClient.getAsyncStub();
        }
        return null;
    }
}
