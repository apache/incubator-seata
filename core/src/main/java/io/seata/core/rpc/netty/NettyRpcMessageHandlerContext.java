package io.seata.core.rpc.netty;

import io.netty.channel.ChannelHandlerContext;
import io.seata.core.protocol.RpcMessage;
import io.seata.core.rpc.processor.RpcMessageHandlerContext;

/**
 * @author goodboycoder
 */
public class NettyRpcMessageHandlerContext extends RpcMessageHandlerContext {
    private final ChannelHandlerContext ctx;

    public NettyRpcMessageHandlerContext(ChannelHandlerContext ctx, RpcMessage rpcMessage) {
        super(rpcMessage);
        setChannel(new NettySeataChannel(ctx.channel()));
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
