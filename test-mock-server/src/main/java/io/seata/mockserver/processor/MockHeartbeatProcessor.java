package io.seata.mockserver.processor;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.HeartbeatMessage;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.TransactionMessageHandler;

/**
 * ?
 *
 * @author minghua.xie
 * @date 2023/11/29
 **/
public class MockHeartbeatProcessor extends MockRemotingProcessor{
    public MockHeartbeatProcessor(RemotingServer remotingServer, TransactionMessageHandler handler) {
        super(remotingServer, handler);
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        super.process(ctx, rpcMessage);
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), HeartbeatMessage.PONG);
    }
}
