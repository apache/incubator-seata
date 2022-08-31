package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public interface MergedMessage {
    void decode(ByteBuffer var1);
}
