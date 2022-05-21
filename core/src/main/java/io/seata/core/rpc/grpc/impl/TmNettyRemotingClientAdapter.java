package io.seata.core.rpc.grpc.impl;

import io.netty.channel.Channel;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.grpc.norelativeimpl.TmRpcCallableClient;
import io.seata.core.rpc.netty.TmNettyRemotingClient;

import java.util.concurrent.TimeoutException;

public class TmNettyRemotingClientAdapter extends TmRpcCallableClient {
    private TmNettyRemotingClient composition;

    @Override
    public void initialize(String applicationId, String transactionServiceGroup) {
        TmNettyRemotingClient tmNettyRemotingClient = TmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        composition = tmNettyRemotingClient;
        tmNettyRemotingClient.init();
    }

    @Override
    public Object sendSyncRequest(Object msg) throws TimeoutException {
        return composition.sendSyncRequest(msg);
    }

    @Override
    public Object sendSyncRequest(Channel channel, Object msg) throws TimeoutException {
        return composition.sendSyncRequest(channel, msg);
    }

    @Override
    public void sendAsyncRequest(Channel channel, Object msg) {
        composition.sendAsyncRequest(channel, msg);
    }

    @Override
    public void sendAsyncResponse(String serverAddress, RpcMessage rpcMessage, Object msg) {
        composition.sendAsyncResponse(serverAddress, rpcMessage, msg);
    }
}
