package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class GlobalRollbackMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 5208687355023442255L;
    long tranId;
    String realSvrAddr;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(256);

    public GlobalRollbackMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public String getRealSvrAddr() {
        return this.realSvrAddr;
    }

    public void setRealSvrAddr(String realSvrAddr) {
        this.realSvrAddr = realSvrAddr;
    }

    public String toString() {
        return "GlobalRollbackMessage tranId:" + this.tranId;
    }

    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    public short getTypeCode() {
        return 9;
    }

    public byte[] encode() {
        this.byteBuffer.putLong(this.tranId);
        byte[] bs;
        if (this.realSvrAddr != null) {
            bs = this.realSvrAddr.getBytes(UTF8);
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

    public void decode(ByteBuffer byteBuffer) {
        this.tranId = byteBuffer.getLong();
        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            this.setRealSvrAddr(new String(bs, UTF8));
        }

    }
}

