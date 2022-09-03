package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class BranchCommitMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = 5083828939317068713L;
    String serverAddr;
    List<Long> tranIds;
    List<Long> branchIds;
    String clientIp;
    String appName;
    String dbName;
    String retrySql;
    byte commitMode;
    String udata;
    public ByteBuffer byteBuffer;

    public BranchCommitMessage() {
        this(1024);
    }

    public BranchCommitMessage(int size) {
        this.udata = null;
        this.tranIds = new ArrayList();
        this.branchIds = new ArrayList();
        this.byteBuffer = ByteBuffer.allocate(size);
    }

    public String getRetrySql() {
        return this.retrySql;
    }

    public void setRetrySql(String retrySql) {
        this.retrySql = retrySql;
    }

    public String getClientIp() {
        return this.clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getUdata() {
        return this.udata;
    }

    public void setUdata(String udata) {
        this.udata = udata;
    }

    public String getAppName() {
        return this.appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public List<Long> getTranIds() {
        return this.tranIds;
    }

    public void setTranIds(List<Long> tranIds) {
        this.tranIds = tranIds;
    }

    public List<Long> getBranchIds() {
        return this.branchIds;
    }

    public void setBranchIds(List<Long> branchIds) {
        this.branchIds = branchIds;
    }

    public byte getCommitMode() {
        return this.commitMode;
    }

    public void setCommitMode(byte commitMode) {
        this.commitMode = commitMode;
    }

    public String getServerAddr() {
        return this.serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    @Override
    public String toString() {
        return this.toString(false);
    }

    public String toString(boolean verbose) {
        StringBuilder sb = new StringBuilder("BranchCommitMessage ");
        if (verbose) {
            for(int i = 0; i < this.tranIds.size(); ++i) {
                sb.append(this.branchIds.get(i)).append("\t");
            }
        } else if (this.branchIds.size() > 0) {
            sb.append(this.branchIds.get(0)).append("...");
        }

        sb.append(",size:").append(this.tranIds.size()).append(" DBname:").append(this.dbName).append(",appName:").append(this.appName).append(",commitMode:").append(this.commitMode).append(",udata:").append(this.udata).append(",rtsql:").append(this.retrySql).toString();
        return sb.toString();
    }

    @Override
    public short getTypeCode() {
        return 3;
    }

    @Override
    public byte[] encode() {
        int i;
        if (this.tranIds.size() > 32) {
            i = this.tranIds.size();
            i = i / 64 + 2;
            this.byteBuffer = ByteBuffer.allocate(i * 1024);
        }

        this.byteBuffer.putInt(this.tranIds.size());

        for(i = 0; i < this.tranIds.size(); ++i) {
            this.byteBuffer.putLong((Long)this.tranIds.get(i));
            this.byteBuffer.putLong((Long)this.branchIds.get(i));
        }

        this.byteBuffer.put(this.commitMode);
        byte[] bs;
        if (this.serverAddr != null) {
            bs = this.serverAddr.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.appName != null) {
            bs = this.appName.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.dbName != null) {
            bs = this.dbName.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.udata != null) {
            bs = this.udata.getBytes(UTF8);
            this.byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                this.byteBuffer.put(bs);
            }
        } else {
            this.byteBuffer.putShort((short)0);
        }

        if (this.retrySql != null) {
            bs = this.retrySql.getBytes(UTF8);
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
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 15) {
            return false;
        } else {
            i -= 15;
            int size = in.readInt();
            if (i < 16 * size) {
                return false;
            } else {
                i -= 16 * size;

                for(int idx = 0; idx < size; ++idx) {
                    this.tranIds.add(in.readLong());
                    this.branchIds.add(in.readLong());
                }

                this.commitMode = in.readByte();
                short len = in.readShort();
                byte[] bs;
                if (len > 0) {
                    if (i < len) {
                        return false;
                    }

                    i -= len;
                    bs = new byte[len];
                    in.readBytes(bs);
                    this.setServerAddr(new String(bs, UTF8));
                }

                len = in.readShort();
                if (len > 0) {
                    if (i < len) {
                        return false;
                    }

                    i -= len;
                    bs = new byte[len];
                    in.readBytes(bs);
                    this.setAppName(new String(bs, UTF8));
                }

                len = in.readShort();
                if (len > 0) {
                    if (i < len) {
                        return false;
                    }

                    i -= len;
                    bs = new byte[len];
                    in.readBytes(bs);
                    this.setDbName(new String(bs, UTF8));
                }

                len = in.readShort();
                if (len > 0) {
                    if (i < len) {
                        return false;
                    }

                    i -= len;
                    bs = new byte[len];
                    in.readBytes(bs);
                    this.setUdata(new String(bs, UTF8));
                }

                len = in.readShort();
                if (len > 0) {
                    if (i < len) {
                        return false;
                    }

                    int var10000 = i - len;
                    bs = new byte[len];
                    in.readBytes(bs);
                    this.setRetrySql(new String(bs, UTF8));
                }

                return true;
            }
        }
    }
}
