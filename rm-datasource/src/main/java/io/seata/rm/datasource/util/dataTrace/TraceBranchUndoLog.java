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
package io.seata.rm.datasource.util.dataTrace;

import io.seata.rm.datasource.undo.SQLUndoLog;
import java.util.List;

public class TraceBranchUndoLog {

    private List<TraceSQLUndoLog> sqlUndoLogs;

    private String executeDate;
    private String userName;

    public List<TraceSQLUndoLog> getSqlUndoLogs() {
        return sqlUndoLogs;
    }

    public void setSqlUndoLogs(List<TraceSQLUndoLog> sqlUndoLogs) {
        this.sqlUndoLogs = sqlUndoLogs;
    }

    public String getExecuteDate() {
        return executeDate;
    }

    public void setExecuteDate(String executeDate) {
        this.executeDate = executeDate;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
