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

import com.alibaba.fescar.rm.datasource.sql.SQLType;
import com.alibaba.fescar.rm.datasource.sql.struct.TableMeta;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

public class SQLUndoLog {

    private SQLType sqlType;

    private String tableName;

    private TableRecords beforeImage;

    private TableRecords afterImage;

    public void setTableMeta(TableMeta tableMeta) {
        if (beforeImage != null) {
            beforeImage.setTableMeta(tableMeta);
        }
        if (afterImage != null) {
            afterImage.setTableMeta(tableMeta);
        }
    }

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

    public TableRecords getBeforeImage() {
        return beforeImage;
    }

    public void setBeforeImage(TableRecords beforeImage) {
        this.beforeImage = beforeImage;
    }

    public TableRecords getAfterImage() {
        return afterImage;
    }

    public void setAfterImage(TableRecords afterImage) {
        this.afterImage = afterImage;
    }
}
