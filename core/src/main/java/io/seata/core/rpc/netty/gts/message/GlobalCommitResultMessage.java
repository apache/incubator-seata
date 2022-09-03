package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class GlobalCommitResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -6911267150170165036L;
    private long tranId;

    public GlobalCommitResultMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public String toString() {
        return "GlobalCommitResultMessage tranId:" + this.tranId + ",result:" + this.result + ",msg:" + this.getMsg();
    }

    public short getTypeCode() {
        return 8;
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
