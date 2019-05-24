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
package io.seata.rm.datasource.undo;

import com.alibaba.fastjson.JSON;
import io.seata.common.util.StringUtils;

import java.util.ArrayList;

import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type Abstract undo executor.
 *
 * @author sharajava
 * @author Geng Zhang
 */
public abstract class AbstractUndoExecutor {

    /**
     * Logger for AbstractUndoExecutor
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractUndoExecutor.class);

    /**
     * template of check sql
     * <p>
     * TODO support multiple primary key
     */
    private static final String CHECK_SQL_TEMPLATE = "SELECT * FROM %s WHERE %s in (%s)";

    /**
     * Switch of undo data validation
     */
    public static final boolean IS_UNDO_DATA_VALIDATION_ENABLE = ConfigurationFactory.getInstance()
            .getBoolean(ConfigurationKeys.TRANSACTION_UNOD_DATA_VALIDATION, true);

    /**
     * wrap sql with row records
     */
    public static class PreparedStatementParams {
        String sql;
        List<Row> rows;

        private PreparedStatementParams() {
        }

        public static PreparedStatementParams create(String sql, List<Row> rows) {
            PreparedStatementParams params = new PreparedStatementParams();
            params.sql = sql;
            params.rows = rows;
            return params;
        }
    }

    /**
     * The Sql undo log.
     */
    protected SQLUndoLog sqlUndoLog;

    /**
     * Instantiates a new Abstract undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public AbstractUndoExecutor(SQLUndoLog sqlUndoLog) {
        this.sqlUndoLog = sqlUndoLog;
    }


    /**
     * Execute on.
     *
     * @param conn the conn
     * @throws SQLException the sql exception
     */
    public void executeOn(Connection conn) throws SQLException {
        if (IS_UNDO_DATA_VALIDATION_ENABLE && !dataValidationAndGoOn(conn)) {
            return;
        }

        try {
            PreparedStatementParams params = buildUndoPreparedStatementParams();
            PreparedStatement undoPrepStmt = conn.prepareStatement(params.sql);
            // TODO batch update
            for (Row row : params.rows) {
                fillPreparedStatement(undoPrepStmt, row);
                undoPrepStmt.executeUpdate();
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
     * build prepareStatement params of undo action
     *
     * @return
     */
    protected abstract PreparedStatementParams buildUndoPreparedStatementParams();

    /**
     * Fill column value to  PreparedStatement
     *
     * @param preparedStatement the undo statement
     * @throws SQLException the sql exception
     */
    void fillPreparedStatement(PreparedStatement preparedStatement, Row row) throws SQLException {
        List<Field> fields = row.getFields();
        for (int i = 0; i < fields.size(); i++) {
            Field field = fields.get(i);
            preparedStatement.setObject(i + 1, field.getValue(), field.getType());
        }
    }

    /**
     * Assert not empty and get first row
     *
     * @return
     */
    protected Row getRowDefinition() {
        List<Row> undoRows = getUndoRecords().getRows();
        if (undoRows == null || undoRows.size() == 0) {
            throw new ShouldNeverHappenException("Invalid UNDO LOG");
        }
        return undoRows.get(0);
    }

    protected abstract TableRecords getUndoRecords();

    /**
     * Get undo image
     *
     * @return return true if data validation is ok and need continue undo, and return false if no need continue undo.
     * @throws SQLException the sql exception
     */
    protected boolean dataValidationAndGoOn(Connection conn) throws SQLException {

        TableRecords beforeRecords = sqlUndoLog.getBeforeImage();
        TableRecords afterRecords = sqlUndoLog.getAfterImage();

        // Compare current data with before data
        // No need undo if the before data snapshot is equivalent to the after data snapshot.
        if (DataCompareUtils.isRecordsEquals(beforeRecords, afterRecords)) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Stop rollback because there is no data change " +
                        "between the before data snapshot and the after data snapshot.");
            }
            // no need continue undo.
            return false;
        }


        TableRecords currentRecords = queryCurrentRecords(conn);
        // compare with current data and after image.
        if (!DataCompareUtils.isRecordsEquals(afterRecords, currentRecords)) {

            // If current data is not equivalent to the after data, then compare the current data with the before 
            // data, too. No need continue to undo if current data is equivalent to the before data snapshot
            if (DataCompareUtils.isRecordsEquals(beforeRecords, currentRecords)) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Stop rollback because there is no data change " +
                            "between the before data snapshot and the current data snapshot.");
                }
                // no need continue undo.
                return false;
            } else {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("check dirty datas failed, old and new data are not equal," +
                            "tableName:[" + sqlUndoLog.getTableName() + "]," +
                            "oldRows:[" + JSON.toJSONString(afterRecords.getRows()) + "]," +
                            "newRows:[" + JSON.toJSONString(currentRecords.getRows()) + "].");
                }
                throw new SQLException("Has dirty records when undo.");
            }
        }
        return true;
    }

    /**
     * Query current records.
     *
     * @param conn the conn
     * @return the table records
     * @throws SQLException the sql exception
     */
    protected TableRecords queryCurrentRecords(Connection conn) throws SQLException {
        TableRecords undoRecords = getUndoRecords();
        TableMeta tableMeta = undoRecords.getTableMeta();
        String pkName = tableMeta.getPkName();
        int pkType = tableMeta.getColumnMeta(pkName).getDataType();

        // pares pk values
        Object[] pkValues = parsePkValues(getUndoRecords());
        if (pkValues.length == 0) {
            return TableRecords.empty(tableMeta);
        }

        // build check sql
        String holders = Arrays.stream(pkValues).map(f -> "?").collect(Collectors.joining(","));
        String checkSQL = String.format(CHECK_SQL_TEMPLATE, sqlUndoLog.getTableName(), pkName, holders);

        TableRecords currentRecords;
        try (PreparedStatement statement = conn.prepareStatement(checkSQL)) {
            for (int i = 1; i <= pkValues.length; i++) {
                statement.setObject(i, pkValues[i - 1], pkType);
            }
            try (ResultSet checkSet = statement.executeQuery()) {
                currentRecords = TableRecords.buildRecords(tableMeta, checkSet);
            }
        }
        return currentRecords;
    }

    /**
     * Parse pk values object [ ].
     *
     * @param records the records
     * @return the object [ ]
     */
    protected Object[] parsePkValues(TableRecords records) {
        return records.getRows().stream()
                .map(row -> row.primaryKeys().get(0).getValue())
                .toArray();
    }
}
