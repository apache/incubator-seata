package io.seata.core.rpc;

import io.seata.core.rpc.grpc.TmGrpcRemotingClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;

/**
 * @author xilou31
 **/
public class TmRemotingClient {
    RpcType rpcType;

    public static Object getInstance(RpcType framework) {
        if (framework == RpcType.NETTY) {
            return TmNettyRemotingClient.getInstance();
        }
        if (framework == RpcType.GRPC) {
            return TmGrpcRemotingClient.getInstance();
        }
        return null;
    }
}
