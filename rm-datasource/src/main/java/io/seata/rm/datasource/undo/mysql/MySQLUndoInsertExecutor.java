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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type My sql undo insert executor.
 *
 * @author sharajava
 */
public class MySQLUndoInsertExecutor extends AbstractUndoExecutor {

    /**
     * DELETE FROM a WHERE pk = ?
     */
    private static final String DELETE_SQL_TEMPLATE = "DELETE FROM %s WHERE %s ";

    private static final int DELETE_BATCH_NUM = 5000;

    @Override
    public void executeOn(Connection conn) throws SQLException {
        if (IS_UNDO_DATA_VALIDATION_ENABLE && !dataValidationAndGoOn(conn)) {
            return;
        }
        try {
            TableRecords undoRows = getUndoRows();

            List<List<Row>> rowsDouble = CollectionUtils.cutData(undoRows.getRows(), DELETE_BATCH_NUM);
            PreparedStatement undoPSTCache = null;
            for (List<Row> rows : rowsDouble) {
                PreparedStatement undoPST = null;
                if (null != undoPSTCache && rows.size() == DELETE_BATCH_NUM) {
                    undoPST = undoPSTCache;
                } else {
                    undoPST = conn.prepareStatement(generateDeleteSql(rows));
                    undoPSTCache = undoPST;
                }

                int undoIndex = 0;
                for (Row undoRow : rows) {
                    for (Field field : undoRow.getFields()) {
                        if (field.getKeyType() == KeyType.PRIMARY_KEY) {
                            undoPST.setObject(++ undoIndex, field.getValue(), field.getType());
                        }
                    }
                }
                undoPST.executeUpdate();
            }

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                throw new SQLException(ex);
            }
        }
    }

    protected String generateDeleteSql(List<Row> rows) {
        List<String> pkNameList = getOrderedPkList(getUndoRows(), rows.get(0), JdbcConstants.MYSQL).stream().map(
            e -> e.getName()).collect(Collectors.toList());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, rows.size(), JdbcConstants.MYSQL, rows.size());
        return String.format(DELETE_SQL_TEMPLATE, sqlUndoLog.getTableName(), whereSql);
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
