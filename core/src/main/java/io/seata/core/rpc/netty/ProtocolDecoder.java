package io.seata.core.rpc.netty;

import io.netty.buffer.ByteBuf;
import io.seata.core.rpc.netty.v0.ProtocolV0RpcMessage;

public interface ProtocolDecoder {

    ProtocolRpcMessage decodeFrame(ByteBuf in);

}
