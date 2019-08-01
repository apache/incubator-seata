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
package io.seata.core.protocol.transaction;

import io.seata.core.model.BranchType;
import io.seata.core.protocol.MessageType;
import io.seata.core.rpc.RpcContext;

import java.io.Serializable;

/**
 * The type to delete undolog  request.
 *
 * @author github-ygy
 * @date 2019-6-14
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
        return "UndoLogDeleteRequest{" +
                "resourceId='" + resourceId + '\'' +
                ", saveDays=" + saveDays +
                ", branchType=" + branchType +
                '}';
    }
}
