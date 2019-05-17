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
package io.seata.rm.datasource.undo.mysql;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;

import java.util.List;
import java.util.stream.Collectors;

/**
 * The type My sql undo update executor.
 *
 * @author sharajava
 */
public class MySQLUndoUpdateExecutor extends AbstractUndoExecutor {

    /**
     * UPDATE a SET x = ?, y = ?, z = ? WHERE pk = ?
     */
    private static final String UPDATE_SQL_TEMPLATE = "UPDATE %s SET %s WHERE %s = ?";

    /**
     * Undo Update.
     *
     * @return sql
     */
    @Override
    protected String buildUndoSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (beforeImageRows == null || beforeImageRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG"); // TODO
        }
        Row row = beforeImageRows.get(0);
        Field pkField = row.primaryKeys().get(0);
        List<Field> nonPkFields = row.nonPrimaryKeys();
        String updateColumns = nonPkFields.stream()
            .map(field -> keywordChecker.checkAndReplace(field.getName()) + " = ?")
            .collect(Collectors.joining(", "));
        return String.format(UPDATE_SQL_TEMPLATE, keywordChecker.checkAndReplace(sqlUndoLog.getTableName()),
                             updateColumns, keywordChecker.checkAndReplace(pkField.getName()));
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
