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

import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.rpc.RpcContext;

import java.io.Serializable;

/**
 * The type to delete undolog  request.
 *
 */
public class UndoLogDeleteRequest extends AbstractTransactionRequestToRM implements Serializable {

    private static final long serialVersionUID = 7539732523682335742L;

    public static final short DEFAULT_SAVE_DAYS = 7;

    private String resourceId;

    private short saveDays = DEFAULT_SAVE_DAYS;

    /**
     * The Branch type.
     */
    protected BranchType branchType = BranchType.AT;

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String resourceId) {
        this.resourceId = resourceId;
    }

    public short getSaveDays() {
        return saveDays;
    }

    public void setSaveDays(short saveDays) {
        this.saveDays = saveDays;
    }

    public BranchType getBranchType() {
        return branchType;
    }

    public void setBranchType(BranchType branchType) {
        this.branchType = branchType;
    }

    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        handler.handle(this);
        return null;
    }

    @Override
    public short getTypeCode() {
        return MessageType.TYPE_RM_DELETE_UNDOLOG;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("UndoLogDeleteRequest{");
        sb.append("resourceId='").append(resourceId).append('\'');
        sb.append(", saveDays=").append(saveDays);
        sb.append(", branchType=").append(branchType);
        sb.append('}');
        return sb.toString();
    }
}
