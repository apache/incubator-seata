package io.seata.core.rpc.grpc.impl;

import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.grpc.norelativeimpl.RpcCallableServer;
import io.seata.core.rpc.netty.NettyRemotingServer;
import io.netty.channel.Channel;

import java.util.concurrent.TimeoutException;

public class NettyRemotingServerAdapter implements RpcCallableServer {

    private NettyRemotingServer composition;

    @Override
    public Object sendSyncRequest(String resourceId, String clientId, Object msg) throws TimeoutException {
        return composition.sendSyncRequest(resourceId, clientId, msg);
    }

    @Override
    public Object sendSyncRequest(Channel conn, Object msg) throws TimeoutException {
        return composition.sendSyncRequest(conn,msg);
    }

    @Override
    public void sendAsyncRequest(Channel conn, Object msg) {
        composition.sendAsyncRequest(conn, msg);
    }

    @Override
    public void sendAsyncResponse(RpcMessage rpcMessage, Channel conn, Object msg) {
        composition.sendAsyncResponse(rpcMessage, conn, msg);
    }
}
