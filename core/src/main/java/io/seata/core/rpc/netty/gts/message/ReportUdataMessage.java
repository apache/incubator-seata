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

public class ReportUdataMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 8761493131644499343L;
    long tranId;
    long branchId;
    String key;
    String udata = null;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(1024);

    public ReportUdataMessage() {
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
        return this.tranId + ":" + this.branchId + " ReportUdataMessage udata:" + this.udata;
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    @Override
    public short getTypeCode() {
        return 17;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.putLong(this.branchId);
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
