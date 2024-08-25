package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import org.apache.seata.core.rpc.netty.MultiProtocolDecoder;

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
        MultiProtocolDecoder multiProtocolDecoder = new MultiProtocolDecoder(serverHandlers);

        return new ChannelHandler[]{multiProtocolDecoder};
    }
}