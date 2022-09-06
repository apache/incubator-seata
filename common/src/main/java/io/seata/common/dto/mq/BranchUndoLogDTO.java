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
package io.seata.common.dto.mq;


public class BranchUndoLogDTO {
    private String xid;

    private long branchId;

    private String sqlUndoLogs;

    public BranchUndoLogDTO(String xid, long branchId, String sqlUndoLogs) {
        this.xid = xid;
        this.branchId = branchId;
        this.sqlUndoLogs = sqlUndoLogs;
    }

    public String getXid() {
        return xid;
    }

    public void setXid(String xid) {
        this.xid = xid;
    }

    public long getBranchId() {
        return branchId;
    }

    public void setBranchId(long branchId) {
        this.branchId = branchId;
    }

    public String getSqlUndoLogs() {
        return sqlUndoLogs;
    }

    public void setSqlUndoLogs(String sqlUndoLogs) {
        this.sqlUndoLogs = sqlUndoLogs;
    }
}
