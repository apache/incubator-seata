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
package com.alibaba.fescar.rm.datasource.undo.mysql;

import java.util.List;

import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.KeyType;
import com.alibaba.fescar.rm.datasource.sql.struct.Row;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;
import com.alibaba.fescar.rm.datasource.undo.AbstractUndoExecutor;
import com.alibaba.fescar.rm.datasource.undo.KeywordChecker;
import com.alibaba.fescar.rm.datasource.undo.KeywordCheckerFactory;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;

/**
 * The type My sql undo update executor.
 */
public class MySQLUndoUpdateExecutor extends AbstractUndoExecutor {

    @Override
    protected String buildUndoSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (beforeImageRows == null || beforeImageRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG"); // TODO
        }
        Row row = beforeImageRows.get(0);
        StringBuffer mainSQL = new StringBuffer(
            "UPDATE " + keywordChecker.checkAndReplace(sqlUndoLog.getTableName()) + " SET ");
        StringBuffer where = new StringBuffer(" WHERE ");
        boolean first = true;
        for (Field field : row.getFields()) {
            if (field.getKeyType() == KeyType.PrimaryKey) {
                where.append(keywordChecker.checkAndReplace(field.getName()) + " = ?");
            } else {
                if (first) {
                    first = false;
                } else {
                    mainSQL.append(", ");
                }
                mainSQL.append(keywordChecker.checkAndReplace(field.getName()) + " = ?");
            }

        }
        return mainSQL.append(where).toString();
    }

    /**
     * Instantiates a new My sql undo update executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public MySQLUndoUpdateExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
