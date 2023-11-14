package io.seata.mock.protocol;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.processor.RemotingProcessor;

/**
 * Mock Remoting Processor
 *
 * @author minghua.xie
 * @date 2023/11/14
 **/
public class MockRemotingProcessor<REQ, RESP> implements RemotingProcessor {

    private Class<RESP> clazz;
    private RemotingServer remotingServer;

    public MockRemotingProcessor(Class<RESP> clazz, RemotingServer remotingServer) {
        this.clazz = clazz;
        this.remotingServer = remotingServer;
    }

    @Override
    public void process(ChannelHandlerContext ctx, RpcMessage rpcMessage) throws Exception {
        REQ message = (REQ) rpcMessage.getBody();
        System.out.println("message = " + message);

        RESP resp = clazz.newInstance();
        remotingServer.sendAsyncResponse(rpcMessage, ctx.channel(), resp);
    }


}
