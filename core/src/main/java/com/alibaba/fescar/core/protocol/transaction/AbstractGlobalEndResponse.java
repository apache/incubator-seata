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

import com.alibaba.fescar.core.model.GlobalStatus;

/**
 * The type Abstract global end response.
 */
public abstract class AbstractGlobalEndResponse extends AbstractTransactionResponse {

    /**
     * The Global status.
     */
    protected GlobalStatus globalStatus;

    /**
     * Gets global status.
     *
     * @return the global status
     */
    public GlobalStatus getGlobalStatus() {
        return globalStatus;
    }

    /**
     * Sets global status.
     *
     * @param globalStatus the global status
     */
    public void setGlobalStatus(GlobalStatus globalStatus) {
        this.globalStatus = globalStatus;
    }

    @Override
    protected void doEncode() {
        super.doEncode();
        byteBuffer.put((byte)globalStatus.getCode());
    }

    @Override
    public void decode(ByteBuffer byteBuffer) {
        super.decode(byteBuffer);
        globalStatus = GlobalStatus.get(byteBuffer.get());
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        result.append("globalStatus=");
        result.append(globalStatus);
        result.append(",");
        result.append("ResultCode=");
        result.append(getResultCode());
        result.append(",");
        result.append("Msg=");
        result.append(getMsg());

        return result.toString();
    }
}
