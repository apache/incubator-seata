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
package com.alibaba.fescar.rm.datasource.undo;

import java.util.List;

/**
 * The type Branch undo log.
 */
public class BranchUndoLog {

    private String xid;

    private long branchId;

    private List<SQLUndoLog> sqlUndoLogs;

    /**
     * Gets xid.
     *
     * @return the xid
     */
    public String getXid() {
        return xid;
    }

    /**
     * Sets xid.
     *
     * @param xid the xid
     */
    public void setXid(String xid) {
        this.xid = xid;
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

    /**
     * Gets sql undo logs.
     *
     * @return the sql undo logs
     */
    public List<SQLUndoLog> getSqlUndoLogs() {
        return sqlUndoLogs;
    }

    /**
     * Sets sql undo logs.
     *
     * @param sqlUndoLogs the sql undo logs
     */
    public void setSqlUndoLogs(List<SQLUndoLog> sqlUndoLogs) {
        this.sqlUndoLogs = sqlUndoLogs;
    }
}
