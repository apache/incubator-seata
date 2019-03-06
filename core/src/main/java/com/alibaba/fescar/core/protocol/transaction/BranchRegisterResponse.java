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

import java.io.Serializable;
import java.nio.ByteBuffer;

import com.alibaba.fescar.core.protocol.AbstractMessage;

/**
 * The type Branch register response.
 */
public class BranchRegisterResponse extends AbstractTransactionResponse implements Serializable {

    private static final long serialVersionUID = 8317040433102745774L;

    private long transactionId;

    private long branchId;

    /**
     * Gets transaction id.
     *
     * @return the transaction id
     */
    public long getTransactionId() {
        return transactionId;
    }

    /**
     * Sets transaction id.
     *
     * @param transactionId the transaction id
     */
    public void setTransactionId(long transactionId) {
        this.transactionId = transactionId;
    }

    /**
     * Gets branch id.
     *
     * @return the branch id
     */
    public long getBranchId() {
        return branchId;
    }

    /**
     * Sets branch id.
     *
     * @param branchId the branch id
     */
    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    @Override
    public short getTypeCode() {
        return AbstractMessage.TYPE_BRANCH_REGISTER_RESULT;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        byteBuffer.putLong(transactionId);
        byteBuffer.putLong(branchId);

    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        this.transactionId = byteBuffer.getLong();
        this.branchId = byteBuffer.getLong();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("BranchRegisterResponse: transactionId=");
        result.append(transactionId);
        result.append(",");
        result.append("branchId=");
        result.append(branchId);
        result.append(",");
        result.append("result code =");
        result.append(getResultCode());
        result.append(",");
        result.append("getMsg =");
        result.append(getMsg());

        return result.toString();
    }
}
