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
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.seata.common.util.CollectionUtils;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.AbstractUndoExecutor;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * The type My sql undo update executor.
 *
 * @author sharajava
 */
public class MySQLUndoUpdateExecutor extends AbstractUndoExecutor {

    private static final int UPDATE_BATCH_NUM = 500;

     /**
     * UPDATE a
     * SET x =
     *  case (pk1, pk2)
     *      when (?, ?) then ?
     *      when (?, ?) then ?
     *  end，
     * y = case (pk1, pk2)
     *     when (?, ?) then ?
     *     when (?, ?) then ?
     *  end
     * WHERE (pk1, pk2) in ((?, ?), (?, ?))
     */
    protected String generateUpdateSql(List<Row> rows) {
        TableRecords beforeImage = getUndoRows();

        List<Field> nonPkFields = rows.get(0).nonPrimaryKeys();
        // update sql undo log before image all field come from table meta. need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        List<String> updateColumns = nonPkFields.stream().map(
                field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.MYSQL)).collect(
                Collectors.toList());


        List<String> pkNameList = getOrderedPkList(beforeImage, rows.get(0), JdbcConstants.MYSQL).stream().map(Field::getName)
                .collect(Collectors.toList());

        StringBuilder caseCondition = new StringBuilder("case (").append(String.join(",", pkNameList)).append(")");
        String s = "when (" + String.join(",", Collections.nCopies(pkNameList.size(), "?")) + ") then ?";
        caseCondition.append(String.join(" ", Collections.nCopies(rows.size(), s))).append("end ");

        StringBuilder updateSql = new StringBuilder("UPDATE ").append(sqlUndoLog.getTableName()).append(" SET ");

        for (String fieldName : updateColumns) {
            updateSql.append(fieldName).append("=").append(caseCondition).append(",");
        }
        updateSql.deleteCharAt(updateSql.length() - 1);

        updateSql.append(" where ").
                append(SqlGenerateUtils.buildWhereConditionByPKs(pkNameList, rows.size(), JdbcConstants.MYSQL, rows.size()));

        return updateSql.toString();
    }

    @Override
    public void executeOn(Connection conn) throws SQLException {
        if (IS_UNDO_DATA_VALIDATION_ENABLE && !dataValidationAndGoOn(conn)) {
            return;
        }

        try {
            TableRecords undoRows = getUndoRows();

            List<List<Row>> rowsDouble = CollectionUtils.cutData(undoRows.getRows(), UPDATE_BATCH_NUM);
            PreparedStatement undoPstCache = null;
            for (List<Row> rows : rowsDouble) {
                PreparedStatement undoPst = null;
                if (null != undoPstCache && rows.size() == UPDATE_BATCH_NUM) {
                    undoPst = undoPstCache;
                } else {
                    String sql = generateUpdateSql(rows);
                    undoPst = conn.prepareStatement(sql);
                    undoPstCache = undoPst;
                }

                int nonPkFieldNum = rows.get(0).nonPrimaryKeys().size();
                int pkFieldNum = rows.get(0).primaryKeys().size();
                int pkFieldPlus1 = pkFieldNum + 1;

                for (int i = 0; i < rows.size(); i ++) {
                    Row row = rows.get(i);
                    List<Field> pks = getOrderedPkList(getUndoRows(), row, JdbcConstants.MYSQL);
                    List<Field> nonPks = row.nonPrimaryKeys();

                    for (int j = 0; j < nonPks.size(); j++) {
                        Field field = nonPks.get(j);

                        int k = 0;
                        for (; k < pkFieldNum; k ++) {
                            Field pkField = pks.get(k);
                            undoPst.setObject(pkFieldPlus1 * rows.size() * j + pkFieldPlus1 * i + k + 1,
                                pkField.getValue(), pkField.getType());
                        }
                        undoPst.setObject(pkFieldPlus1 * rows.size() * j + pkFieldPlus1 * i + k + 1,
                                field.getValue(), field.getType());
                    }
                    for (int j = 0; j < pkFieldNum; j ++) {
                        undoPst.setObject(nonPkFieldNum * pkFieldPlus1 * rows.size() + i * pkFieldNum + j + 1,
                                pks.get(j).getValue(), pks.get(j).getType());
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
