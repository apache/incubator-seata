package io.seata.core.rpc.netty;

import io.seata.core.protocol.RpcMessage;

/**
 * The protocol RPC message.
 *
 * @author Bughue
 */
public interface ProtocolRpcMessage {
    RpcMessage convert2RpcMsg();

}
