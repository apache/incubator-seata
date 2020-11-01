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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * INSERT INTO a (x, y, z, pk) VALUES (?, ?, ?, ?),  (?, ?, ?, ?)
     */
    private static final String INSERT_SQL_TEMPLATE = "INSERT INTO %s (%s) VALUES %s";

    private static final int INSERT_BATCH_NUM = 5000;

    @Override
    public void executeOn(Connection conn) throws SQLException {
        if (IS_UNDO_DATA_VALIDATION_ENABLE && !dataValidationAndGoOn(conn)) {
            return;
        }

        try {
            TableRecords undoRows = getUndoRows();

            List<List<Row>> rowsDouble = CollectionUtils.cutData(undoRows.getRows(), INSERT_BATCH_NUM);
            PreparedStatement undoPstCache = null;
            for (List<Row> rows : rowsDouble) {
                PreparedStatement undoPst = null;
                if (null != undoPstCache && rows.size() == INSERT_BATCH_NUM) {
                    undoPst = undoPstCache;
                } else {
                    undoPst = conn.prepareStatement(generateInsertSql(rows));
                    undoPstCache = undoPst;
                }

                int undoIndex = 0;
                for (Row undoRow : rows) {
                    for (Field field : undoRow.nonPrimaryKeys()) {
                        undoPst.setObject(++ undoIndex, field.getValue(), field.getType());
                    }
                    for (Field field : getOrderedPkList(getUndoRows(), undoRow, JdbcConstants.MYSQL)) {
                        undoPst.setObject(++ undoIndex, field.getValue(), field.getType());
                    }
                }
                undoPst.executeUpdate();
            }

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                throw new SQLException(ex);
            }
        }
    }

    /**
     * Undo delete.
     *
     * Notice: PK is at last one.
     * @see AbstractUndoExecutor#undoPrepare
     *
     * @return sql
     */
    protected String generateInsertSql(List<Row> rows) {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();

        Row row = rows.get(0);
        List<Field> fields = new ArrayList<>(row.nonPrimaryKeys());
        fields.addAll(getOrderedPkList(beforeImage,row,JdbcConstants.MYSQL));

        // delete sql undo log before image all field come from table meta, need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String insertColumns = fields.stream()
                .map(field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.MYSQL))
                .collect(Collectors.joining(", "));
        String insertValueOneRow = ("(" + fields.stream().map(field -> "?")
                .collect(Collectors.joining(", ")) + ")");
        String insertValues = String.join("", Collections.nCopies(rows.size(), insertValueOneRow));

        return String.format(INSERT_SQL_TEMPLATE, sqlUndoLog.getTableName(), insertColumns, insertValues);
    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
