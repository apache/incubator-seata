package org.apache.seata.core.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.apache.seata.core.protocol.RpcMessage;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Test client handler that captures server responses
 */
public class TestClientHandler extends ChannelInboundHandlerAdapter {

    private final AtomicReference<Object> responseRef;
    private final CountDownLatch responseLatch;

    public TestClientHandler(AtomicReference<Object> responseRef, CountDownLatch responseLatch) {
        this.responseRef = responseRef;
        this.responseLatch = responseLatch;
    }

    public TestClientHandler() {
        this.responseRef = new AtomicReference<>();
        this.responseLatch = new CountDownLatch(1);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // Handle RpcMessage wrapped responses
        if (msg instanceof RpcMessage) {
            RpcMessage rpcMessage = (RpcMessage) msg;
            Object body = rpcMessage.getBody();
            responseRef.set(body);
        } else {
            // Handle direct responses (for backward compatibility)
            responseRef.set(msg);
        }
        responseLatch.countDown();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        cause.printStackTrace();
        ctx.close();
    }
}
