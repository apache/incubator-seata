package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class QueryLockMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 9102589803150659929L;
    long tranId;
    String key;
    String businessKey;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    public QueryLockMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getBusinessKey() {
        return this.businessKey;
    }

    public void setBusinessKey(String businessKey) {
        this.businessKey = businessKey;
    }

    public String toString() {
        return "QueryLockMessage tranId:" + this.tranId + ",key:" + this.key + ",business key:" + this.businessKey;
    }

    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    public short getTypeCode() {
        return 21;
    }

    public byte[] encode() {
        this.byteBuffer.putLong(this.tranId);
        byte[] bs;
        if (this.key != null) {
            bs = this.key.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.businessKey != null) {
            bs = this.businessKey.getBytes(UTF8);
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
        byte[] bs;
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setKey(new String(bs, UTF8));
        }

        len = byteBuffer.getShort();
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setBusinessKey(new String(bs, UTF8));
        }

    }
}
