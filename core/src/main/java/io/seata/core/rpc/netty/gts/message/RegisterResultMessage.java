package io.seata.core.rpc.netty.gts.message;

import java.io.Serializable;
import java.nio.ByteBuffer;

public class RegisterResultMessage extends AbstractResultMessage implements Serializable {
    private static final long serialVersionUID = 8317040433102745774L;
    long tranId;
    long branchId;

    public RegisterResultMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public long getBranchId() {
        return this.branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    public String toString() {
        return "RegisterResultMessage result:" + this.result + ",tranId:" + this.tranId + ",branchId:" + this.branchId;
    }

    @Override
    public short getTypeCode() {
        return 12;
    }

    @Override
    public byte[] encode() {
        super.encode();
        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.putLong(this.branchId);
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        this.tranId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
    }
}
