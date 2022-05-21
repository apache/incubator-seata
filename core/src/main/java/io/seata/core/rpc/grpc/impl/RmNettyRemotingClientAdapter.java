package io.seata.core.rpc.grpc.impl;

import io.netty.channel.Channel;
import io.seata.core.model.ResourceManager;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.core.rpc.grpc.norelativeimpl.RmRpcCallableClient;
import io.seata.core.rpc.netty.RmNettyRemotingClient;

import java.util.concurrent.TimeoutException;

public class RmNettyRemotingClientAdapter extends RmRpcCallableClient {
    private RmNettyRemotingClient composition;


    @Override
    public void initialize(String applicationId, String transactionServiceGroup, ResourceManager rm, TransactionMessageHandler handler) {
        RmNettyRemotingClient rmNettyRemotingClient = RmNettyRemotingClient.getInstance(applicationId, transactionServiceGroup);
        rmNettyRemotingClient.setResourceManager(rm);
        rmNettyRemotingClient.setTransactionMessageHandler(handler);
        composition = rmNettyRemotingClient;
        rmNettyRemotingClient.init();
    }

    @Override
    public void registerResource(String resourceGroupId, String resourceId) {
        composition.registerResource(resourceGroupId, resourceId);
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
