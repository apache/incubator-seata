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
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.sql.struct.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

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
     *
     */
    private static final String CHECK_SQL_TEMPLATE = "SELECT * FROM %s WHERE %s in (%s)";

    /**
     * Switch of undo data validation
     */
    public static final boolean IS_UNDO_DATA_VALIDATION_ENABLE = ConfigurationFactory.getInstance()
            .getBoolean(ConfigurationKeys.TRANSACTION_UNDO_DATA_VALIDATION, true);

    /**
     * The Sql undo log.
     */
    protected SQLUndoLog sqlUndoLog;

    /**
     * Build undo sql string.
     *
     * @return the string
     */
    protected abstract String buildUndoSQL();

    /**
     * Instantiates a new Abstract undo executor.
     *
     * @param sqlUndoLog the sql undo log
     */
    public AbstractUndoExecutor(SQLUndoLog sqlUndoLog) {
        this.sqlUndoLog = sqlUndoLog;
    }

    /**
     * Gets sql undo log.
     *
     * @return the sql undo log
     */
    public SQLUndoLog getSqlUndoLog() {
        return sqlUndoLog;
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
            String undoSQL = buildUndoSQL();

            PreparedStatement undoPST = conn.prepareStatement(undoSQL);

            TableRecords undoRows = getUndoRows();

            for (Row undoRow : undoRows.getRows()) {
                ArrayList<Field> undoValues = new ArrayList<>();
                List<Field> pkValues = new ArrayList<>();
                for (Field field : undoRow.getFields()) {
                    if (field.getKeyType() == KeyType.PrimaryKey) {
                        pkValues.add(field);
                    } else {
                        undoValues.add(field);
                    }
                }

                undoPrepare(undoPST, undoValues, pkValues);

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

    /**
     * Undo prepare.
     *
     * @param undoPST    the undo pst
     * @param undoValues the undo values
     * @param pkValues    the pk values
     * @throws SQLException the sql exception
     */
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValues)
        throws SQLException {
        // PK append last
        // INSERT INTO a (x, y, z, pk0, pk1) VALUES (?, ?, ?, ?, ?)
        // UPDATE a SET x=?, y=?, z=? WHERE pk0 = ? and pk1= ?
        // DELETE FROM a WHERE pk0 = ? and pk1 = ? ..
        undoValues.addAll(pkValues);
        int undoIndex = 0;
        for (Field undoValue : undoValues) {
            undoIndex++;
            undoPST.setObject(undoIndex, undoValue.getValue(), undoValue.getType());
        }

    }

    /**
     * Gets undo rows.
     *
     * @return the undo rows
     */
    protected abstract TableRecords getUndoRows();

    /**
     * Data validation.
     *
     * @param conn the conn
     * @return return true if data validation is ok and need continue undo, and return false if no need continue undo.
     * @throws SQLException the sql exception such as has dirty data
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

        // Validate if data is dirty.
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
     *
     * Query current records.
     *
     * @param conn the conn
     * @return the table records
     * @throws SQLException the sql exception
     */
    protected TableRecords queryCurrentRecords(Connection conn) throws SQLException {
        TableRecords undoRecords = getUndoRows();
        TableMeta tableMeta = undoRecords.getTableMeta();

        Object[] pkValues = parsePkValues(undoRecords);
        if (pkValues.length == 0) {
            return TableRecords.empty(tableMeta);
        }
        // build check sql
        String pkNames = getPkColumns(tableMeta);
        String placeholder  = getPkPlaceholder(getUndoRows().getRows());
        String checkSQL = String.format(CHECK_SQL_TEMPLATE, sqlUndoLog.getTableName(), pkNames,placeholder);
        // pares pk values
        List<Field> pkFields = parsePkFields(undoRecords);


        PreparedStatement statement = null;
        ResultSet checkSet = null;
        TableRecords currentRecords;
        try {
            statement = conn.prepareStatement(checkSQL);
            for (int i = 0; i < pkFields.size(); i++) {
                Field field = pkFields.get(i);
                int pkType = tableMeta.getColumnMeta(field.getName()).getDataType();
                statement.setObject(i + 1, field.getValue(), pkType);
            }
            checkSet = statement.executeQuery();
            currentRecords = TableRecords.buildRecords(tableMeta, checkSet);
        } finally {
            if (checkSet != null) {
                try {
                    checkSet.close();
                } catch (Exception e) {
                }
            }
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }
        return currentRecords;
    }


    /**
     * get getPkColumns
     * simple: select * from table_name where (column1,column2) in ((?,?),(?,?))
     * @param tableMeta tableMeta
     * @return getPkColumns  (column1,column2)
     */
    private String getPkColumns(TableMeta tableMeta) {
        List<String> primaryKeys = tableMeta.getPrimaryKeyOnlyName();
        StringJoiner stringJoiner = new StringJoiner(",","(",")");
        for (String primaryKey:primaryKeys) {
            stringJoiner.add(primaryKey);
        }
        return stringJoiner.toString();
    }

    /**
     * simple: select * from table_name where (column1,column2) in ((?,?),(?,?))
     * getPkPlaceholder
     * @param rowList rowList
     * @return pkPlaceHolder ((?,?),(?,?))
     *
     */
    private String getPkPlaceholder(List<Row> rowList) {
        StringJoiner placeholder = new StringJoiner(",");
        for(Row row:rowList){
            StringJoiner joinerField = new StringJoiner(",","(",")");
            for (Field field: row.primaryKeys()) {
                joinerField.add("?");
            }
            placeholder.add(joinerField.toString());
        }
        return placeholder.toString();
    }


    /**
     * Parse pk values object [ ].
     * @param records the records
     * @return the object [ ]
     */
    protected Object[] parsePkValues(TableRecords records) {
        List<Field> pkFields = parsePkFields(records);
        Object[] pkValues = new Object[pkFields.size()];
        for (int i = 0; i < pkFields.size(); i++) {
            pkValues[i] = pkFields.get(i).getValue();
        }
        return pkValues;
    }

    /**
     * Parse pk field list [ ].
     * @param records  the records
     * @return
     */
    private List<Field> parsePkFields(TableRecords records) {
        List<Field> pkFields = new ArrayList<>();
        List<Row> undoRows = records.getRows();
        for (Row row:undoRows) {
            pkFields.addAll(row.primaryKeys());
        }
        return pkFields;
    }


    /**
     * Undo SQL
     * @param keywordChecker keywordChecker
     * @param fields PK fields
     * @return SQL
     */
    protected String buildPkFields (KeywordChecker keywordChecker,List<Field> fields) {
        StringJoiner stringJoiner = new StringJoiner(" AND ");
        for(Field field: fields){
            stringJoiner.add(keywordChecker.checkAndReplace(field.getName())+" = ?");
        }
        return stringJoiner.toString();
    }
}
