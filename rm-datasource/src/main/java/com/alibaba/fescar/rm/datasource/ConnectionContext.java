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

package com.alibaba.fescar.rm.datasource;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;

public class ConnectionContext {
    private String xid;
    private Long branchId;
    private List<String> lockKeysBuffer = new ArrayList<>();
    private List<SQLUndoLog> sqlUndoItemsBuffer = new ArrayList<>();

    void appendLockKey(String lockKey) {
        lockKeysBuffer.add(lockKey);
    }

    void appendUndoItem(SQLUndoLog sqlUndoLog) {
        sqlUndoItemsBuffer.add(sqlUndoLog);
    }

    public boolean inGlobalTransaction() {
        return xid != null;
    }

    public boolean isBranchRegistered() {
        return branchId != null;
    }

    void bind(String xid) {
        if (xid == null) {
            throw new IllegalArgumentException("xid should not be null");
        }
        if (!inGlobalTransaction()) {
            setXid(xid);
        } else {
            if (!this.xid.equals(xid)) {
                throw new ShouldNeverHappenException();
            }
        }
    }

    public boolean hasUndoLog() {
        return sqlUndoItemsBuffer.size() > 0;
    }

    public String getXid() {
        return xid;
    }

    void setXid(String xid) {
        this.xid = xid;
    }

    public Long getBranchId() {
        return branchId;
    }

    void setBranchId(Long branchId) {
        this.branchId = branchId;
    }

    void reset() {
        xid = null;
        branchId = null;
        lockKeysBuffer.clear();
        sqlUndoItemsBuffer.clear();
    }

    void reset(String xid) {
        this.xid = xid;
        branchId = null;
        lockKeysBuffer.clear();
        sqlUndoItemsBuffer.clear();
    }

    public String buildLockKeys() {
        if (lockKeysBuffer.isEmpty()) {
            return null;
        }
        StringBuffer appender = new StringBuffer();
        Iterator<String> iterable = lockKeysBuffer.iterator();
        while (iterable.hasNext()) {
            appender.append(iterable.next());
            if (iterable.hasNext()) {
                appender.append(";");
            }
        }
        return appender.toString();
    }

    public List<SQLUndoLog> getUndoItems() {
        return sqlUndoItemsBuffer;
    }
}
