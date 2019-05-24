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
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.KeywordChecker;
import io.seata.rm.datasource.undo.KeywordCheckerFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;

import java.util.ArrayList;
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
     * Update SQL , PK column on last one
     *
     * @return
     */
    @Override
    protected PreparedStatementParams buildUndoPreparedStatementParams() {
        List<Row> pkOnLastOne = getUndoRecords().getRows().stream()
                .map(src -> {
                    List<Field> sortedFields = new ArrayList<>(src.getFields().size());
                    sortedFields.addAll(src.nonPrimaryKeys());
                    sortedFields.addAll(src.primaryKeys());

                    return new Row(sortedFields);
                })
                .collect(Collectors.toList());
        return PreparedStatementParams.create(buildUpdateSQL(), pkOnLastOne);
    }

    /**
     * Undo Update => Update
     *
     * @return sql
     */
    protected String buildUpdateSQL() {
        KeywordChecker keywordChecker = KeywordCheckerFactory.getKeywordChecker(JdbcConstants.MYSQL);

        Row row = getRowDefinition();
        Field pkField = row.primaryKeys().get(0);
        List<Field> nonPkFields = row.nonPrimaryKeys();

        String tableName = keywordChecker.checkAndReplace(sqlUndoLog.getTableName());
        String columns = nonPkFields.stream()
                .map(field -> keywordChecker.checkAndReplace(field.getName()) + " = ?")
                .collect(Collectors.joining(", "));
        String pkColumnName = keywordChecker.checkAndReplace(pkField.getName());

        return String.format(UPDATE_SQL_TEMPLATE, tableName, columns, pkColumnName);
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
    protected TableRecords getUndoRecords() {
        return sqlUndoLog.getBeforeImage();
    }

}
