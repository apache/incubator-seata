package io.seata.core.rpc.netty.v0;

public interface SerializerV0 {

    MessageCodecV0 getMsgInstanceByCode(short typeCode);
}
