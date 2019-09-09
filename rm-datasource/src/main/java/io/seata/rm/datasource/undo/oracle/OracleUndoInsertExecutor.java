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
package io.seata.rm.datasource.undo.oracle;

import com.alibaba.druid.util.JdbcConstants;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * The type oralce undo insert executor.
 * @author ccg
 * @date 2019/3/25
 */
public class OracleUndoInsertExecutor extends AbstractUndoExecutor {

    @Override
    protected String buildUndoSQL() {
        KeywordChecker keywordChecker= KeywordCheckerFactory.getKeywordChecker(JdbcConstants.ORACLE);
        TableRecords afterImage = sqlUndoLog.getAfterImage();
        List<Row> afterImageRows = afterImage.getRows();
        if (afterImageRows == null || afterImageRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = afterImageRows.get(0);
        StringBuilder mainSQL = new StringBuilder("DELETE FROM ").append(keywordChecker.checkAndReplace(sqlUndoLog.getTableName()));
        StringBuilder where = new StringBuilder(" WHERE ");
        // For a row, there's only one primary key now
        for (Field field : row.getFields()) {
            if (field.getKeyType() == KeyType.PrimaryKey) {
                where.append(keywordChecker.checkAndReplace(field.getName())).append(" = ?");
            }

        }
        return mainSQL.append(where).toString();
    }

    @Override
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, Field pkValue) throws SQLException {
        undoPST.setObject(1, pkValue.getValue(), pkValue.getType());
    }

    /**
     * Instantiates a new My sql undo insert executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public OracleUndoInsertExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getAfterImage();
    }
}
