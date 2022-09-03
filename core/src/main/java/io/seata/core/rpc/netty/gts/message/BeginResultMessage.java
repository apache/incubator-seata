package io.seata.core.rpc.netty.gts.message;

import java.nio.ByteBuffer;

public class BeginResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = -5947172130577163908L;
    String xid;
    String nextSvrAddr;

    public BeginResultMessage() {
    }

    public String getXid() {
        return this.xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public String getNextSvrAddr() {
        return this.nextSvrAddr;
    }

    public void setNextSvrAddr(String nextSvrAddr) {
        this.nextSvrAddr = nextSvrAddr;
    }

    @Override
    public String toString() {
        return "BeginResultMessage result:" + this.result + " xid:" + this.xid;
    }

    @Override
    public short getTypeCode() {
        return 2;
    }

    @Override
    public byte[] encode() {
        super.encode();
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

        if (this.nextSvrAddr != null) {
            bs = this.nextSvrAddr.getBytes(UTF8);
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
        short len = byteBuffer.getShort();
        byte[] bs;
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setXid(new String(bs, UTF8));
        }

        len = byteBuffer.getShort();
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setNextSvrAddr(new String(bs, UTF8));
        }

    }
}
