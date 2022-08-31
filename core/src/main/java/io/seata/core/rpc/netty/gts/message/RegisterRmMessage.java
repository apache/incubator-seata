package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
// TODO 对接gts和seata的version
import io.seata.core.protocol.Version;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class RegisterRmMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = 7539732523682335742L;
    private String dbKeys;
    private String version = Version.getCurrent();
    private short type = 0;
    private String appName;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(1048576);

    public RegisterRmMessage() {
    }

    public RegisterRmMessage(String dbKeys) {
        this.dbKeys = dbKeys;
        this.type = 0;
        this.appName = null;
    }

    public String getDbKeys() {
        return this.dbKeys;
    }

    public void setDbKeys(String dbKeys) {
        this.dbKeys = dbKeys;
    }

    public String getVersion() {
        return this.version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public short getType() {
        return this.type;
    }

    public void setType(short type) {
        this.type = type;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public short getTypeCode() {
        return 103;
    }

    public byte[] encode() {
        byte[] bs;
        if (this.dbKeys != null) {
            bs = this.dbKeys.getBytes(UTF8);
            this.byteBuffer.putInt(bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putInt(0);
        }

        if (this.version != null) {
            bs = this.version.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.version != null) {
            this.byteBuffer.putShort(this.type);
            if (this.appName != null) {
                bs = this.appName.getBytes(UTF8);
                this.byteBuffer.putShort((short)bs.length);
                if (bs.length > 0) {
                    this.byteBuffer.put(bs);
                }
            } else {
                this.byteBuffer.putShort((short)0);
            }
        }

        this.byteBuffer.flip();
        bs = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(bs);
        return bs;
    }

    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 6) {
            return false;
        } else {
            i -= 6;
            int iLen = in.readInt();
            byte[] bs;
            if (iLen > 0) {
                if (i < iLen) {
                    return false;
                }

                i -= iLen;
                bs = new byte[iLen];
                in.readBytes(bs);
                this.setDbKeys(new String(bs, UTF8));
            }

            short len = in.readShort();
            if (len > 0) {
                if (i < len) {
                    return false;
                }

                i -= len;
                bs = new byte[len];
                in.readBytes(bs);
                this.setVersion(new String(bs, UTF8));
            }

            if (this.version != null) {
                if (i < 4) {
                    return false;
                }

                i -= 4;
                this.type = in.readShort();
                len = in.readShort();
                if (len > 0) {
                    if (i < len) {
                        return false;
                    }

                    int var10000 = i - len;
                    bs = new byte[len];
                    in.readBytes(bs);
                    this.setAppName(new String(bs, UTF8));
                }
            }

            return true;
        }
    }

    public String toString() {
        return "RegisterRmMessage dbkey:" + this.dbKeys + ",appname:" + this.appName;
    }
}
