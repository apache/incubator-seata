package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;

public interface TxcCodec {
    short getTypeCode();

    void setChannelHandlerContext(ChannelHandlerContext var1);

    byte[] encode();

    boolean decode(ByteBuf var1);
}