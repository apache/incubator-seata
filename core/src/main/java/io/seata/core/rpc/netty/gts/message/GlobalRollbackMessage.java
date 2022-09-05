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

public class GlobalRollbackMessage extends TxcMessage implements MergedMessage {
    private static final long serialVersionUID = 5208687355023442255L;
    long tranId;
    String realSvrAddr;
    public ByteBuffer byteBuffer = ByteBuffer.allocate(256);

    public GlobalRollbackMessage() {
    }

    public long getTranId() {
        return this.tranId;
    }

    public void setTranId(long tranId) {
        this.tranId = tranId;
    }

    public String getRealSvrAddr() {
        return this.realSvrAddr;
    }

    public void setRealSvrAddr(String realSvrAddr) {
        this.realSvrAddr = realSvrAddr;
    }

    @Override
    public String toString() {
        return "GlobalRollbackMessage tranId:" + this.tranId;
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    @Override
    public short getTypeCode() {
        return 9;
    }

    @Override
    public byte[] encode() {
        this.byteBuffer.putLong(this.tranId);
        byte[] bs;
        if (this.realSvrAddr != null) {
            bs = this.realSvrAddr.getBytes(UTF8);
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
        short len = byteBuffer.getShort();
        if (len > 0) {
            byte[] bs = new byte[len];
            byteBuffer.get(bs);
            this.setRealSvrAddr(new String(bs, UTF8));
        }

    }
}

