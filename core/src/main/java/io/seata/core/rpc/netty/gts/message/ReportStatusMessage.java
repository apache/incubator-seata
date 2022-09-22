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

public class ReportStatusMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 1882313813620375335L;
    long tranId;
    long branchId;
    boolean success;
    String key;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(1024);
    String udata = null;

    public ReportStatusMessage() {
    }

    public String getUdata() {
        return this.udata;
    }

    public void setUdata(String udata) {
        this.udata = udata;
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public boolean isSuccess() {
        return this.success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public long getBranchId() {
        return this.branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    public String toString() {
        return this.tranId + ":" + this.branchId + " ReportStatusMessage:" + this.success + ",key:" + this.key;
    }

    @Override
    public short getTypeCode() {
        return 13;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.putLong(this.branchId);
        this.byteBuffer.put((byte)(this.success ? 1 : 0));
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
    public void decode(ByteBuffer byteBuffer) {
        this.tranId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
        this.success = byteBuffer.get() == 1;
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
            this.setUdata(new String(bs, UTF8));
        }

    }
}
