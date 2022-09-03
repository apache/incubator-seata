package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class ClusterDumpResultMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = -4566761304528956910L;
    private boolean result;
    private String msg;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(33554432);

    public ClusterDumpResultMessage() {
    }

    public boolean isResult() {
        return this.result;
    }

    public void setResult(boolean result) {
        this.result = result;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public short getTypeCode() {
        return 114;
    }

    @Override
    public String toString() {
        return "ClusterDumpResultMessage msg:" + this.msg;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.put((byte)(this.result ? 1 : 0));
        byte[] bs;
        if (this.msg != null) {
            bs = this.msg.getBytes(UTF8);
            this.byteBuffer.putInt(bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putInt(0);
        }

        this.byteBuffer.flip();
        bs = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(bs);
        return bs;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 5) {
            return false;
        } else {
            i -= 5;
            this.result = in.readBoolean();
            int len = in.readInt();
            if (len > 0) {
                if (i < len) {
                    return false;
                }

                int var10000 = i - len;
                byte[] bs = new byte[len];
                in.readBytes(bs);
                this.setMsg(new String(bs, UTF8));
            }

            return true;
        }
    }
}
