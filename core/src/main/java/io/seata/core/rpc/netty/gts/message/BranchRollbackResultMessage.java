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

public class BranchRollbackResultMessage extends AbstractResultMessage {
    private static final long serialVersionUID = 478110436802982116L;
    long tranId;
    long branchId;

    public BranchRollbackResultMessage() {
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
        return this.tranId + ":" + this.branchId + " BranchRollbackResultMessage result:" + this.result;
    }

    @Override
    public void handleMessage(long msgId, String dbKeys, String clientIp, String clientAppName, String vgroupName, TxcMessage message, AbstractResultMessage[] results, int idx) {
        ((TxcMsgHandler)this.handler).handleMessage(msgId, dbKeys, clientIp, clientAppName, vgroupName, this, results, idx);
    }

    @Override
    public short getTypeCode() {
        return 6;
    }

    @Override
    public byte[] encode() {
        super.encode();
        this.byteBuffer.putLong(this.tranId);
        this.byteBuffer.putLong(this.branchId);
        this.byteBuffer.flip();
        byte[] content = new byte[this.byteBuffer.limit()];
        this.byteBuffer.get(content);
        return content;
    }

    @Override
    public boolean decode(ByteBuf in) {
        if (!super.decode(in)) {
            return false;
        } else if (in.readableBytes() < 16) {
            return false;
        } else {
            this.tranId = in.readLong();
            this.branchId = in.readLong();
            return true;
        }
    }
}
