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

import io.seata.sqlparser.SQLType;

public class TraceSQLUndoLog {

    private SQLType sqlType;

    private String tableName;

    private TraceTableRecords beforeImage;

    private TraceTableRecords afterImage;

    public SQLType getSqlType() {
        return sqlType;
    }

    public void setSqlType(SQLType sqlType) {
        this.sqlType = sqlType;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public TraceTableRecords getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(TraceTableRecords beforeImage) {
        this.beforeImage = beforeImage;
    }

    public TraceTableRecords getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(TraceTableRecords afterImage) {
        this.afterImage = afterImage;
    }
}
