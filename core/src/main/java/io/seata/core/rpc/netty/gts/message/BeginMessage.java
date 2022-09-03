package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class BeginMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 7236162274218388376L;
    public long timeout = 60000L;
    private String appname = null;
    private String txcInst = null;

    public BeginMessage() {
    }

    public long getTimeout() {
        return this.timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return "BeginMessage timeout:" + this.timeout + " appname:" + this.appname + " txcInst:" + this.txcInst;
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    @Override
    public short getTypeCode() {
        return 1;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(256);
        byteBuffer.putLong(this.timeout);
        byte[] bs;
        if (this.appname != null) {
            bs = this.appname.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        if (this.txcInst != null) {
            bs = this.txcInst.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        byteBuffer.flip();
        bs = new byte[byteBuffer.limit()];
        byteBuffer.get(bs);
        return bs;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        this.timeout = byteBuffer.getLong();
        short len = byteBuffer.getShort();
        byte[] bs;
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setAppname(new String(bs, UTF8));
        }

        len = byteBuffer.getShort();
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setTxcInst(new String(bs, UTF8));
        }

    }

    public String getAppname() {
        return this.appname;
    }

    public void setAppname(String appname) {
        this.appname = appname;
    }

    public String getTxcInst() {
        return this.txcInst;
    }

    public void setTxcInst(String txcInst) {
        this.txcInst = txcInst;
    }
}
