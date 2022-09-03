package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class GlobalCommitMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = -6389141795815407988L;
    long tranId;

    public GlobalCommitMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    @Override
    public String toString() {
        return "GlobalCommitMessage tranId:" + this.tranId;
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    @Override
    public short getTypeCode() {
        return 7;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(16);
        byteBuffer.putLong(this.tranId);
        byteBuffer.flip();
        byte[] content = new byte[byteBuffer.limit()];
        byteBuffer.get(content);
        return content;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        this.tranId = byteBuffer.getLong();
    }
}

