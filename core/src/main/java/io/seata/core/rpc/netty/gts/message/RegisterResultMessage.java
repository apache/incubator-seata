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

import java.io.Serializable;
import java.nio.ByteBuffer;

public class RegisterResultMessage extends AbstractResultMessage implements Serializable {
    private static final long serialVersionUID = 8317040433102745774L;
    long tranId;
    long branchId;

    public RegisterResultMessage() {
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
        return "RegisterResultMessage result:" + this.result + ",tranId:" + this.tranId + ",branchId:" + this.branchId;
    }

    @Override
    public short getTypeCode() {
        return 12;
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
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        this.tranId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
    }
}
