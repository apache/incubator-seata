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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.rm.datasource.ConnectionProxy;
import org.apache.seata.rm.datasource.sql.struct.Field;
import org.apache.seata.rm.datasource.sql.struct.KeyType;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.seata.sqlparser.struct.SqlServerTableMeta;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.sqlparser.util.JdbcConstants;


public class SqlServerUndoDeleteExecutor extends BaseSqlServerUndoExecutor {

    private boolean tableIdentifyExistence = false;

    /**
     * Instantiates a new sql server delete undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public SqlServerUndoDeleteExecutor(SQLUndoLog sqlUndoLog) {
        super(sqlUndoLog);
    }

    @Override
    protected String buildUndoSQL() {
        TableRecords beforeImage = sqlUndoLog.getBeforeImage();
        List<Row> beforeImageRows = beforeImage.getRows();
        if (CollectionUtils.isEmpty(beforeImageRows)) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        Row row = beforeImageRows.get(0);
        List<Field> fields = new ArrayList<>(row.nonPrimaryKeys());
        fields.addAll(getOrderedPkList(beforeImage, row, JdbcConstants.SQLSERVER));

        // delete sql undo log before image all field come from table meta, need add escape.
        // see BaseTransactionalExecutor#buildTableRecords
        String insertColumns = fields.stream()
                .map(field -> ColumnUtils.addEscape(field.getName(), JdbcConstants.SQLSERVER))
                .collect(Collectors.joining(", "));
        String insertValues = fields.stream().map(field -> "?")
                .collect(Collectors.joining(", "));

        if (tableIdentifyExistence) {
            return "begin " +
                    "SET IDENTITY_INSERT " + sqlUndoLog.getTableName() + " ON;" +
                    "INSERT INTO " + sqlUndoLog.getTableName() + "(" + insertColumns + ") VALUES (" + insertValues + ");" +
                    "SET IDENTITY_INSERT " + sqlUndoLog.getTableName() + " OFF; " +
                    "end";
        }
        return "INSERT INTO " + sqlUndoLog.getTableName() + "(" + insertColumns + ") VALUES (" + insertValues + ");";
    }

    /**
     * Judge whether there is a column of a SQLServer table is with a "IDENTITY"
     *
     * @param connectionProxy
     */
    private void judgeTableIdentifyExistence(ConnectionProxy connectionProxy) {
        TableMeta tableMeta = TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
                .getTableMeta(
                        connectionProxy.getTargetConnection(),
                        sqlUndoLog.getTableName(),
                        connectionProxy.getDataSourceProxy().getResourceId()
                );
        tableIdentifyExistence = ((SqlServerTableMeta) tableMeta).isTableIdentifyExistence();
    }

    /**
     * Execute on.
     *
     * @param connectionProxy the connection proxy
     * @throws SQLException the sql exception
     */
    @Override
    public void executeOn(ConnectionProxy connectionProxy) throws SQLException {
        Connection conn = connectionProxy.getTargetConnection();
        if (IS_UNDO_DATA_VALIDATION_ENABLE && !dataValidationAndGoOn(connectionProxy)) {
            return;
        }

        judgeTableIdentifyExistence(connectionProxy);

        PreparedStatement undoPST = null;
        try {
            String undoSQL = buildUndoSQL();
            undoPST = conn.prepareStatement(undoSQL);
            TableRecords undoRows = getUndoRows();
            for (Row undoRow : undoRows.getRows()) {
                ArrayList<Field> undoValues = new ArrayList<>();
                List<Field> pkValueList = getOrderedPkList(undoRows, undoRow, connectionProxy.getDbType());
                for (Field field : undoRow.getFields()) {
                    if (field.getKeyType() != KeyType.PRIMARY_KEY) {
                        undoValues.add(field);
                    }
                }

                undoPrepare(undoPST, undoValues, pkValueList);

                undoPST.executeUpdate();
            }

        } catch (Exception ex) {
            if (ex instanceof SQLException) {
                throw (SQLException) ex;
            } else {
                throw new SQLException(ex);
            }
        } finally {
            IOUtil.close(undoPST);
        }

    }

    @Override
    protected TableRecords getUndoRows() {
        return sqlUndoLog.getBeforeImage();
    }
}
