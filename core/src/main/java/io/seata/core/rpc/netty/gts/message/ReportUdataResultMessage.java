package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class ReportUdataResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -4164423237707604299L;

    public ReportUdataResultMessage() {
    }

    public String toString() {
        return "ReportUdataResultMessage result:" + this.result;
    }

    public short getTypeCode() {
        return 18;
    }

    public byte[] encode() {
        super.encode();
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
    }
}
