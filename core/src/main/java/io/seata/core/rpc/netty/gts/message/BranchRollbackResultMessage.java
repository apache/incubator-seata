package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;

public class BranchRollbackResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = 478110436802982116L;
    long tranId;
    long branchId;

    public BranchRollbackResultMessage() {
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

    public String toString() {
        return this.tranId + ":" + this.branchId + " BranchRollbackResultMessage result:" + this.result;
    }

    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    public short getTypeCode() {
        return 6;
    }

    public byte[] encode() {
        super.encode();
        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.putLong(this.branchId);
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    public boolean decode(ByteBuf in) {
        if (!super.decode(in)) {
            return false;
        } else if (in.readableBytes() < 16) {
            return false;
        } else {
            this.tranId = in.readLong();
            this.branchId = in.readLong();
            return true;
        }
    }
}
