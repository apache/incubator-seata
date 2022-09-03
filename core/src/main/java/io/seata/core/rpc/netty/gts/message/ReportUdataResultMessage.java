package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class ReportUdataResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -4164423237707604299L;

    public ReportUdataResultMessage() {
    }

    @Override
    public String toString() {
        return "ReportUdataResultMessage result:" + this.result;
    }

    @Override
    public short getTypeCode() {
        return 18;
    }

    @Override
    public byte[] encode() {
        super.encode();
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
    }
}
