package io.seata.core.rpc;

import java.util.concurrent.TimeoutException;

import io.netty.channel.Channel;
import io.seata.core.protocol.RpcMessage;

/**
 * @author goodboycoder
 */
public class Requester {
    private static volatile Requester instance;

    private RemotingServer remotingServer;

    private Requester() {
    }

    public static Requester getInstance() {
        if (null == instance) {
            synchronized (Requester.class) {
                if (null == instance) {
                    instance = new Requester();
                }
            }
        }
        return instance;
    }

    public void setRemotingServer(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    //    public Object clientSendSyncRequest(Object msg) {
//        return null;
//    }
//
//    public Object clientSendSyncRequest(SeataChannel channel, Object msg) {
//        return null;
//    }
//
//    public void clientSendAsyncRequest(SeataChannel channel, Object msg) {
//
//    }
//
//    public void clientSendAsyncResponse(String serverAddress, RpcMessage rpcMessage, Object msg) {
//
//    }



    public Object sendSyncRequest(String resourceId, String clientId, Object msg) {
        return null;
    }

    public Object sendSyncRequest(SeataChannel channel, Object msg) throws TimeoutException {
        if (RpcType.NETTY == channel.getType()) {
            return remotingServer.sendSyncRequest((Channel) channel.originChannel(), msg);
        }
        return null;
    }

    public void sendAsyncRequest(SeataChannel channel, Object msg) {
        if (RpcType.NETTY == channel.getType()) {
            remotingServer.sendAsyncRequest((Channel) channel.originChannel(), msg);
        }
    }

    public void sendAsyncResponse(RpcMessage rpcMessage, SeataChannel channel, Object msg) {
        if (RpcType.NETTY == channel.getType()) {
            remotingServer.sendAsyncResponse(rpcMessage, (Channel) channel.originChannel(), msg);
        }
    }
}
