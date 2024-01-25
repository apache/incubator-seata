/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.core.protocol.transaction;

import java.io.Serializable;

import org.apache.seata.core.protocol.MessageType;

/**
 * The type Branch register response.
 *
 */
public class BranchRegisterResponse extends AbstractTransactionResponse implements Serializable {

    private long branchId;


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
        return MessageType.TYPE_BRANCH_REGISTER_RESULT;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("BranchRegisterResponse{");
        sb.append("branchId=").append(branchId);
        sb.append(", resultCode=").append(resultCode);
        sb.append(", msg='").append(msg).append('\'');
        sb.append('}');
        return sb.toString();
    }
}
