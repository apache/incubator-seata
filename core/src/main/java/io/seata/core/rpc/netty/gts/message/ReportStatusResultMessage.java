package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class ReportStatusResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -5305551094475083258L;
    private long branchId;

    public ReportStatusResultMessage() {
    }

    public long getBranchId() {
        return this.branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public String toString() {
        return "ReportStatusResultMessage branchId:" + this.branchId + ",result:" + this.result;
    }

    public short getTypeCode() {
        return 14;
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

