package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class BeginRetryBranchResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = 5790404345818486619L;
    String xid;
    long branchId;

    public BeginRetryBranchResultMessage() {
    }

    public String getXid() {
        return this.xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public long getBranchId() {
        return this.branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    public String toString() {
        return "BeginRetryBranchResultMessage result:" + this.result + ",xid:" + this.xid + ",branchId:" + this.branchId;
    }

    @Override
    public short getTypeCode() {
        return 16;
    }

    @Override
    public byte[] encode() {
        super.encode();
        this.byteBuffer.putLong(this.branchId);
        byte[] bs;
        if (this.xid != null) {
            bs = this.xid.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        this.byteBuffer.flip();
        bs = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(bs);
        return bs;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        this.branchId = byteBuffer.getLong();
        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] msg = new byte[len];
            byteBuffer.get(msg);
            this.setXid(new String(msg, UTF8));
        }

    }
}
