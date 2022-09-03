package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.nio.ByteBuffer;

public abstract class AbstractResultMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 6540352050650203313L;
    int result;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(512);
    private String msg;

    public AbstractResultMessage() {
    }

    public int getResult() {
        return this.result;
    }

    public void setResult(int result) {
        this.result = result;
    }

    public String getMsg() {
        return this.msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.put((byte)this.result);
        if (this.result != 1) {
            if (this.getMsg() != null) {
                String msg;
                if (this.getMsg().length() > 128) {
                    msg = this.getMsg().substring(0, 128);
                } else {
                    msg = this.getMsg();
                }

                byte[] bs = msg.getBytes(UTF8);
                if (bs.length > 400 && this.getMsg().length() > 64) {
                    msg = this.getMsg().substring(0, 64);
                    bs = msg.getBytes(UTF8);
                }

                this.byteBuffer.putShort((short)bs.length);
                if (bs.length > 0) {
                    this.byteBuffer.put(bs);
                }
            } else {
                this.byteBuffer.putShort((short)0);
            }
        }

        return null;
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        this.setResult(byteBuffer.get());
        if (this.result != 1) {
            short len = byteBuffer.getShort();
            if (len > 0) {
                byte[] msg = new byte[len];
                byteBuffer.get(msg);
                this.setMsg(new String(msg, UTF8));
            }
        }

    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 1) {
            return false;
        } else {
            this.setResult(in.readByte());
            --i;
            if (this.result != 1) {
                if (i < 2) {
                    return false;
                }

                short len = in.readShort();
                i -= 2;
                if (i < len) {
                    return false;
                }

                if (len > 0) {
                    byte[] msg = new byte[len];
                    in.readBytes(msg);
                    this.setMsg(new String(msg, UTF8));
                }
            }

            return true;
        }
    }
}
