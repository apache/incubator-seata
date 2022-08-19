package io.seata.core.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.processor.RpcMessageHandleContext;

/**
 * @author goodboycoder
 */
public class NettyRpcMessageHandleContext extends RpcMessageHandleContext {
    private final ChannelHandlerContext ctx;

    public NettyRpcMessageHandleContext(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        super(rpcMessage, new NettySeataChannel(ctx.channel()));
        this.ctx = ctx;
    }

    @Override
    public void close() {
        this.ctx.close();
    }

    @Override
    public void disconnect() {
        this.ctx.disconnect();
    }
}
