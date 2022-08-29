package io.seata.core.rpc;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import io.seata.core.protocol.RpcMessage;

/**
 * @author goodboycoder
 */
public class ServerRequester {
    private static volatile ServerRequester instance;

    private final Map<RpcType, RemotingServer> remotingServerMap = new ConcurrentHashMap<>();

    private ServerRequester() {
    }

    public static ServerRequester getInstance() {
        if (null == instance) {
            synchronized (ServerRequester.class) {
                if (null == instance) {
                    instance = new ServerRequester();
                }
            }
        }
        return instance;
    }

    public void addRemotingServer(RpcType rpcType, RemotingServer remotingServer) {
        remotingServerMap.put(rpcType, remotingServer);
    }


    public Object sendSyncRequest(String resourceId, String clientId, Object msg) throws TimeoutException {
        RpcType rpcType = SeataChannelServerManager.getRpcType(resourceId, clientId);
        return getServerByRpcType(rpcType).sendSyncRequest(resourceId, clientId, msg);
    }

    public Object sendSyncRequest(SeataChannel channel, Object msg) throws TimeoutException {
        return getServerByRpcType(channel.getType()).sendSyncRequest(channel, msg);
    }

    public void sendAsyncRequest(SeataChannel channel, Object msg) {
        getServerByRpcType(channel.getType()).sendAsyncRequest(channel, msg);
    }

    public void sendAsyncResponse(RpcMessage rpcMessage, SeataChannel channel, Object msg) {
        getServerByRpcType(channel.getType()).sendAsyncResponse(rpcMessage, channel, msg);
    }

    private RemotingServer getServerByRpcType(RpcType rpcType) {
        RemotingServer remotingServer = remotingServerMap.get(rpcType);
        if (null == remotingServer) {
            throw new RuntimeException("Could not find RemoteServer corresponding to RpcType:" + rpcType.name);
        }
        return remotingServer;
    }
}
