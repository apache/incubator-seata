package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public class ClusterDumpMessage extends TxcMessage {
    private static final long serialVersionUID = -6826254198463287830L;
    private boolean verbose;

    public ClusterDumpMessage() {
    }

    public boolean isVerbose() {
        return this.verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public short getTypeCode() {
        return 113;
    }

    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.put((byte)(this.verbose ? 1 : 0));
        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    public boolean decode(ByteBuf in) {
        if (in.readableBytes() < 1) {
            return false;
        } else {
            this.verbose = in.readByte() == 1;
            return true;
        }
    }
}
