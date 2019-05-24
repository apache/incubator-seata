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
 * The type My sql undo delete executor.
 *
 * @author sharajava
 */
public class MySQLUndoDeleteExecutor extends AbstractUndoExecutor {

    /**
     * Instantiates a new My sql undo delete executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public MySQLUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    private static final String INSERT_SQL_TEMPLATE = "INSERT INTO %s (%s) VALUES (%s)";


    @Override
    protected PreparedStatementParams buildUndoPreparedStatementParams() {
        return PreparedStatementParams.create(buildInsertSQL(), getUndoRecords().getRows());
    }


    /**
     * Undo Delete => Insert
     *
     * @return sql
     */
    protected String buildInsertSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);

        Row row = getRowDefinition();
        List<Field> fields = row.getFields();

        String tableName = keywordChecker.checkAndReplace(sqlUndoLog.getTableName());
        String columns = fields.stream()
                .map(field -> keywordChecker.checkAndReplace(field.getName()))
                .collect(Collectors.joining(", "));
        String holders = fields.stream().map(field -> "?")
                .collect(Collectors.joining(", "));

        return String.format(INSERT_SQL_TEMPLATE, tableName, columns, holders);
    }

    @Override
    protected TableRecords getUndoRecords() {
        return sqlUndoLog.getBeforeImage();
    }
}
