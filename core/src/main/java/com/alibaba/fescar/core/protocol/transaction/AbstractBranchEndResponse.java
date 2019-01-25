/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.core.protocol.transaction;

import java.nio.ByteBuffer;

import com.alibaba.fescar.core.model.BranchStatus;

import io.netty.buffer.ByteBuf;

public abstract class AbstractBranchEndResponse extends AbstractTransactionResponse {

    protected BranchStatus branchStatus;

    public BranchStatus getBranchStatus() {
        return branchStatus;
    }

    public void setBranchStatus(BranchStatus branchStatus) {
        this.branchStatus = branchStatus;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        byteBuffer.put((byte) branchStatus.ordinal());
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        branchStatus = BranchStatus.get(byteBuffer.get());
    }

    @Override
    public boolean decode(ByteBuf in) {
        boolean s = super.decode(in);
        if (!s) {
            return s;
        }
        branchStatus = BranchStatus.get(in.readByte());
        return true;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("branchStatus=");
        result.append(branchStatus);
        result.append(",");
        result.append("result code =");
        result.append(getResultCode());
        result.append(",");
        result.append("getMsg =");
        result.append(getMsg());

        return result.toString();
    }
}
