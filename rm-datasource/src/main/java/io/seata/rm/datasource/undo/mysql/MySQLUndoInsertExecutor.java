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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;

/**
 * The type My sql undo insert executor.
 *
 * @author sharajava
 */
public class MySQLUndoInsertExecutor extends AbstractUndoExecutor {

    /**
     * single PK: DELETE FROM a WHERE pk = ?
     * multi  PK: DELETE FROM a WHERE pk1 = ? AND pk2 = ? ...
     */
    private static final String DELETE_SQL_TEMPLATE = "DELETE FROM %s WHERE %s";


    /**
     * Undo Inset.
     *
     * @return sql
     */
    @Override
    protected String buildUndoSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);
        TableRecords afterImage = sqlUndoLog.getAfterImage();
        List<Row> afterImageRows = afterImage.getRows();
        if (afterImageRows == null || afterImageRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = afterImageRows.get(0);
        String pkFields = buildPkFields(keywordChecker,row.primaryKeys());
        return String.format(DELETE_SQL_TEMPLATE,
                keywordChecker.checkAndReplace(sqlUndoLog.getTableName()),
                keywordChecker.checkAndReplace(pkFields));

    }



    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValues)
        throws SQLException {
        int parameterIndex = 0;
        for (Field field : pkValues) {
            parameterIndex++;
            undoPST.setObject(parameterIndex, field.getValue(), field.getType());
        }
    }

    /**
     * Instantiates a new My sql undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public MySQLUndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getAfterImage();
    }
}
