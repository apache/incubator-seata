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

import java.nio.ByteBuffer;

public class BeginRetryBranchMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = -3476983417106748414L;
    public long effectiveTime = 1800000L;
    String dbName;
    byte commitMode;
    String sql;

    public BeginRetryBranchMessage() {
    }

    public long getEffectiveTime() {
        return this.effectiveTime;
    }

    public void setEffectiveTime(long effectiveTime) {
        this.effectiveTime = effectiveTime;
    }

    public String getDbName() {
        return this.dbName;
    }

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public byte getCommitMode() {
        return this.commitMode;
    }

    public void setCommitMode(byte commitMode) {
        this.commitMode = commitMode;
    }

    public String getSql() {
        return this.sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    @Override
    public String toString() {
        return "BeginRetryBranchMessage dbName:" + this.dbName + ",Commit mode:" + this.commitMode + ",effectiveTime:" + this.effectiveTime + ",sql:" + this.sql;
    }

    @Override
    public short getTypeCode() {
        return 15;
    }

    @Override
    public byte[] encode() {
        ByteBuffer byteBuffer = ByteBuffer.allocate(1048576);
        byteBuffer.putLong(this.effectiveTime);
        byteBuffer.put(this.commitMode);
        byte[] bs;
        if (this.dbName != null) {
            bs = this.dbName.getBytes(UTF8);
            byteBuffer.putShort((short)bs.length);
            if (bs.length > 0) {
                byteBuffer.put(bs);
            }
        } else {
            byteBuffer.putShort((short)0);
        }

        if (this.sql != null) {
            bs = this.sql.getBytes(UTF8);
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
        this.effectiveTime = byteBuffer.getLong();
        this.commitMode = byteBuffer.get();
        short len = byteBuffer.getShort();
        byte[] bs;
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setDbName(new String(bs, UTF8));
        }

        len = byteBuffer.getShort();
        if (len > 0) {
            bs = new byte[len];
            byteBuffer.get(bs);
            this.setSql(new String(bs, UTF8));
        }

    }
}
