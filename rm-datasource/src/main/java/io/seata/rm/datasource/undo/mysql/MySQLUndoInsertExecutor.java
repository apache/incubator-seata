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

import java.util.List;
import java.util.stream.Collectors;

import com.alibaba.druid.util.JdbcConstants;
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
     * DELETE FROM a WHERE pk = ?
     */
    private static final String DELETE_SQL_TEMPLATE = "DELETE FROM %s WHERE %s = ?";

    /**
     * row records only require PK fields
     *
     * @return
     */
    @Override
    protected PreparedStatementParams buildUndoPreparedStatementParams() {
        List<Row> onlyPKField = getUndoRecords().getRows().stream()
                .map(row -> new Row(row.primaryKeys()))
                .collect(Collectors.toList());

        return PreparedStatementParams.create(buildDeleteSQL(), onlyPKField);
    }

    /**
     * Undo Insert => Delete
     *
     * @return sql
     */
    protected String buildDeleteSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);

        Row row = getRowDefinition();
        Field pkField = row.primaryKeys().get(0);

        String tableName = keywordChecker.checkAndReplace(sqlUndoLog.getTableName());
        String pkColumnName = keywordChecker.checkAndReplace(pkField.getName());

        return String.format(DELETE_SQL_TEMPLATE, tableName, pkColumnName);
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
    protected TableRecords getUndoRecords() {
        return sqlUndoLog.getAfterImage();
    }

}
