package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public class RedressMessage extends TxcMessage {
    private static final long serialVersionUID = -2213516219582268198L;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(33554432);
    private String msg;

    public RedressMessage() {
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public short getTypeCode() {
        return 121;
    }

    @Override
    public byte[] encode() {
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
        if (i < 4) {
            return false;
        } else {
            i -= 4;
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

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }
}
