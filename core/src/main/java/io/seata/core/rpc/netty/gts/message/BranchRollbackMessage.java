/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.core.rpc.netty.gts.message;

import io.netty.buffer.ByteBuf;
import java.io.Serializable;
import java.nio.ByteBuffer;

public class BranchRollbackMessage extends TxcMessage implements Serializable {
    private static final long serialVersionUID = 6585956272089436177L;
    String serverAddr;
    long tranId;
    long branchId;
    String appName;
    String dbName;
    byte commitMode;
    byte isDelLock;
    String udata = null;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    public BranchRollbackMessage() {
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

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public byte getCommitMode() {
        return this.commitMode;
    }

    public void setCommitMode(byte commitMode) {
        this.commitMode = commitMode;
    }

    public byte getIsDelLock() {
        return this.isDelLock;
    }

    public void setIsDelLock(byte deleteLock) {
        this.isDelLock = deleteLock;
    }

    public long getBranchId() {
        return this.branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public String getServerAddr() {
        return this.serverAddr;
    }

    public void setServerAddr(String serverAddr) {
        this.serverAddr = serverAddr;
    }

    @Override
    public String toString() {
        return this.tranId + ":" + this.branchId + " BranchRollbackMessage DBname:" + this.dbName + ",appName:" + this.appName + ",commitMode:" + this.commitMode + ",isDelLock:" + this.isDelLock + ",udata:" + this.udata;
    }

    @Override
    public short getTypeCode() {
        return 5;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.putLong(this.branchId);
        this.byteBuffer.put(this.commitMode);
        this.byteBuffer.put(this.isDelLock);
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

        this.byteBuffer.flip();
        bs = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(bs);
        return bs;
    }

    @Override
    public boolean decode(ByteBuf in) {
        int i = in.readableBytes();
        if (i < 26) {
            return false;
        } else {
            this.tranId = in.readLong();
            this.branchId = in.readLong();
            this.commitMode = in.readByte();
            this.isDelLock = in.readByte();
            short len = in.readShort();
            i -= 26;
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

                bs = new byte[len];
                in.readBytes(bs);
                this.setUdata(new String(bs, UTF8));
            }

            return true;
        }
    }
}
