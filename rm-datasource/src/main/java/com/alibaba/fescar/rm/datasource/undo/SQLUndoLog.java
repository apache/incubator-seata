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

/**
 * The type Sql undo log.
 */
public class SQLUndoLog {

    private SQLType sqlType;

    private String tableName;

    private TableRecords beforeImage;

    private TableRecords afterImage;

    /**
     * Sets table meta.
     *
     * @param tableMeta the table meta
     */
    public void setTableMeta(TableMeta tableMeta) {
        if (beforeImage != null) {
            beforeImage.setTableMeta(tableMeta);
        }
        if (afterImage != null) {
            afterImage.setTableMeta(tableMeta);
        }
    }

    /**
     * Gets sql type.
     *
     * @return the sql type
     */
    public SQLType getSqlType() {
        return sqlType;
    }

    /**
     * Sets sql type.
     *
     * @param sqlType the sql type
     */
    public void setSqlType(SQLType sqlType) {
        this.sqlType = sqlType;
    }

    /**
     * Gets table name.
     *
     * @return the table name
     */
    public String getTableName() {
        return tableName;
    }

    /**
     * Sets table name.
     *
     * @param tableName the table name
     */
    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    /**
     * Gets before image.
     *
     * @return the before image
     */
    public TableRecords getBeforeImage() {
        return beforeImage;
    }

    /**
     * Sets before image.
     *
     * @param beforeImage the before image
     */
    public void setBeforeImage(TableRecords beforeImage) {
        this.beforeImage = beforeImage;
    }

    /**
     * Gets after image.
     *
     * @return the after image
     */
    public TableRecords getAfterImage() {
        return afterImage;
    }

    /**
     * Sets after image.
     *
     * @param afterImage the after image
     */
    public void setAfterImage(TableRecords afterImage) {
        this.afterImage = afterImage;
    }
}
