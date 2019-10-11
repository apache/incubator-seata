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
package io.seata.rm.datasource.sql;

import io.seata.rm.datasource.ColumnUtils;

import java.util.List;

/**
 * @author jsbxyyx
 */
public class SQLInsertRecognizerAdapter implements SQLInsertRecognizer {

    private final SQLInsertRecognizer sqlInsertRecognizer;
    private final String dbType;

    public SQLInsertRecognizerAdapter(SQLInsertRecognizer sqlInsertRecognizer, String dbType) {
        this.sqlInsertRecognizer = sqlInsertRecognizer;
        this.dbType = dbType;
    }


    @Override
    public List<String> getInsertColumns() {
        List<String> insertColumns = sqlInsertRecognizer.getInsertColumns();
        if (insertColumns == null || insertColumns.isEmpty()) {
            return insertColumns;
        }
        ColumnUtils.addEscape(insertColumns, dbType);
        return insertColumns;
    }

    @Override
    public List<List<Object>> getInsertRows() {
        return sqlInsertRecognizer.getInsertRows();
    }

    @Override
    public SQLType getSQLType() {
        return sqlInsertRecognizer.getSQLType();
    }

    @Override
    public String getTableAlias() {
        return sqlInsertRecognizer.getTableAlias();
    }

    @Override
    public String getTableName() {
        return ColumnUtils.addEscape(sqlInsertRecognizer.getTableName(), dbType);
    }

    @Override
    public String getOriginalSQL() {
        return sqlInsertRecognizer.getOriginalSQL();
    }
}
