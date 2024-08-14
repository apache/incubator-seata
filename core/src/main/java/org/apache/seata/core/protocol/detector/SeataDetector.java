package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import org.apache.seata.core.rpc.netty.v1.ProtocolDecoderV1;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;

public class SeataDetector implements ProtocolDetector {
    private final byte[] MAGIC_CODE_BYTES = {(byte) 0xda, (byte) 0xda};
    private ChannelHandler[] serverHandlers;

    public SeataDetector(ChannelHandler[] serverHandlers) {
        this.serverHandlers = serverHandlers;
    }

    @Override
    public boolean detect(ByteBuf in) {
        if (in.readableBytes() < MAGIC_CODE_BYTES.length) {
            return false;
        }
        for (int i = 0; i < MAGIC_CODE_BYTES.length; i++) {
            if (in.getByte(i) != MAGIC_CODE_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ChannelHandler[] getHandlers() {
        ChannelHandler[] ret = new ChannelHandler[2 + serverHandlers.length];
        ret[0] = new ProtocolDecoderV1();
        ret[1] = new ProtocolEncoderV1();
        for (int i = 0; i < serverHandlers.length; i++) {
            ret[2 + i] = serverHandlers[i];
        }
        return ret;
    }
}