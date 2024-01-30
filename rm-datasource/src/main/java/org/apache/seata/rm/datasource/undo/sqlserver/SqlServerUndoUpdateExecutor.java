/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.undo.sqlserver;

import java.util.List;
import java.util.stream.Collectors;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.rm.datasource.SqlGenerateUtils;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;


public class SqlServerUndoUpdateExecutor extends BaseSqlServerUndoExecutor {
    /**
     * Instantiates a new SqlServer update undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public SqlServerUndoUpdateExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            // TODO
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = beforeImageRows.get(0);

        List<Field> nonPkFields = row.nonPrimaryKeys();
        // update sql undo log before image all field come from table meta. need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String updateColumns = nonPkFields.stream().map(
            field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.SQLSERVER) + " = ?").collect(
            Collectors.joining(", "));

        List<String> pkNameList = getOrderedPkList(beforeImage, row, JdbcConstants.SQLSERVER).stream().map(Field::getName)
            .collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, JdbcConstants.SQLSERVER);

        return "UPDATE " + sqlUndoLog.getTableName() + " SET " + updateColumns + " WHERE " + whereSql;
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
