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

import javax.sql.rowset.serial.SerialBlob;
import javax.sql.rowset.serial.SerialClob;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.alibaba.fastjson.JSON;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.Result;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.DataCompareUtils;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.KeyType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.util.JdbcUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.seata.core.constants.DefaultValues.DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION;

import java.util.*;
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
     * TODO support multiple primary key
     */
    private static final String CHECK_SQL_TEMPLATE = "SELECT * FROM %s WHERE %s";

    /**
     * Switch of undo data validation
     */
    public static final boolean IS_UNDO_DATA_VALIDATION_ENABLE = ConfigurationFactory.getInstance()
        .getBoolean(ConfigurationKeys.TRANSACTION_UNDO_DATA_VALIDATION, DEFAULT_TRANSACTION_UNDO_DATA_VALIDATION);

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

            TableRecords undoRecords = getUndoRows();

            for (Row undoRow : undoRecords.getRows()) {
                ArrayList<Field> undoValues = new ArrayList<>();
                List<Field> pkValueList =  getOrderedPkList(undoRecords,undoRow);
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

        }

    }

    /**
     * Undo prepare.
     *
     * @param undoPST    the undo pst
     * @param undoValues the undo values
     * @param pkValueList    the pk value
     * @throws SQLException the sql exception
     */
    protected void undoPrepare(PreparedStatement undoPST, ArrayList<Field> undoValues, List<Field> pkValueList)
        throws SQLException {
        int undoIndex = 0;
        for (Field undoValue : undoValues) {
            undoIndex++;
            if (undoValue.getType() == JDBCType.BLOB.getVendorTypeNumber()) {
                SerialBlob serialBlob = (SerialBlob) undoValue.getValue();
                if (serialBlob != null) {
                    undoPST.setBlob(undoIndex, serialBlob.getBinaryStream());
                } else {
                    undoPST.setObject(undoIndex, null);
                }
            } else if (undoValue.getType() == JDBCType.CLOB.getVendorTypeNumber()) {
                SerialClob serialClob = (SerialClob) undoValue.getValue();
                if (serialClob != null) {
                    undoPST.setClob(undoIndex, serialClob.getCharacterStream());
                } else {
                    undoPST.setObject(undoIndex, null);
                }
            } else if (undoValue.getType() == JDBCType.OTHER.getVendorTypeNumber()) {
                undoPST.setObject(undoIndex, undoValue.getValue());
            } else {
                undoPST.setObject(undoIndex, undoValue.getValue(), undoValue.getType());
            }
        }
        // PK is always at last.
        // INSERT INTO a (x, y, z, pk1,pk2) VALUES (?, ?, ?, ? ,?)
        // UPDATE a SET x=?, y=?, z=? WHERE pk1 in (?) and pk2 in (?)
        // DELETE FROM a WHERE pk1 in (?) and pk2 in (?)
        for(Field pkField:pkValueList)
        {
            undoIndex++;
            undoPST.setObject(undoIndex, pkField.getValue(), pkField.getType());
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
        Result<Boolean> beforeEqualsAfterResult = DataCompareUtils.isRecordsEquals(beforeRecords, afterRecords);
        if (beforeEqualsAfterResult.getResult()) {
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
        Result<Boolean> afterEqualsCurrentResult = DataCompareUtils.isRecordsEquals(afterRecords, currentRecords);
        if (!afterEqualsCurrentResult.getResult()) {

            // If current data is not equivalent to the after data, then compare the current data with the before 
            // data, too. No need continue to undo if current data is equivalent to the before data snapshot
            Result<Boolean> beforeEqualsCurrentResult = DataCompareUtils.isRecordsEquals(beforeRecords, currentRecords);
            if (beforeEqualsCurrentResult.getResult()) {
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("Stop rollback because there is no data change " +
                        "between the before data snapshot and the current data snapshot.");
                }
                // no need continue undo.
                return false;
            } else {
                if (LOGGER.isInfoEnabled()) {
                    if (StringUtils.isNotBlank(afterEqualsCurrentResult.getErrMsg())) {
                        LOGGER.info(afterEqualsCurrentResult.getErrMsg(), afterEqualsCurrentResult.getErrMsgParams());
                    }
                }
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
        TableRecords undoRecords = getUndoRows();
        TableMeta tableMeta = undoRecords.getTableMeta();
        //the order of element matters
        List<String> pkNameList = tableMeta.getPrimaryKeyOnlyName();

        // pares pk values
        Map<String,List<Field>> pkRowValues = parsePkValues(getUndoRows());
        if (pkRowValues.size() == 0) {
            return TableRecords.empty(tableMeta);
        }
        // build check sql
        String dbType = getDbType(conn);
        String checkSQL = String.format(CHECK_SQL_TEMPLATE, ColumnUtils.addEscape(sqlUndoLog.getTableName(), dbType),
                buildWhereConditionByPKs(pkNameList,pkRowValues,conn));

        PreparedStatement statement = null;
        ResultSet checkSet = null;
        TableRecords currentRecords;
        try {
            statement = conn.prepareStatement(checkSQL);
            int paramIndex=1;
            for(String key:pkNameList)
            {
                List<Field> fieldList= pkRowValues.get(key);
                for (int i = 0; i<fieldList.size(); i++) {
                    Field field=fieldList.get(i);
                    int dataType=tableMeta.getColumnMeta(field.getName()).getDataType();
                    statement.setObject(paramIndex,field.getValue(),dataType);
                    paramIndex++;
                }
            }
            checkSet = statement.executeQuery();
            currentRecords = TableRecords.buildRecords(tableMeta, checkSet);
        } finally {
            IOUtil.close(checkSet, statement);
        }
        return currentRecords;
    }

    protected List<Field> getOrderedPkList(TableRecords image,Row row){
        List<Field> pkFields = new ArrayList<>();
        // To ensure the order of the pk, we the order should based on getPrimaryKeyOnlyName.
        List<String> pkColumnNameListByOrder = image.getTableMeta().getPrimaryKeyOnlyName();
        List<String> pkColumnNameListNoOrder = row.primaryKeys().stream().map(e->e.getName()).collect(Collectors.toList());
        pkColumnNameListByOrder.forEach(pkName->{
            int pkIndex=pkColumnNameListNoOrder.indexOf(pkName);
            if(pkIndex!=-1)
            {
                // add PK to the last of the list.
                pkFields.add(row.primaryKeys().get(pkIndex));
            }
        });
        return pkFields;
    }

    /**
     * each pk is a condition.the result will like :" id =? and userCode =?"
     * @param pkNameList
     * @return return where condition sql string.the sql can just search one related record.
     */
    protected String buildWhereConditionByPKs(List<String> pkNameList,KeywordChecker keywordChecker) {
        StringBuilder whereStr = new StringBuilder();
        //we must consider the situation of composite primary key
        for (int i =0;i<pkNameList.size();i++)
        {
            if(i>0){
                whereStr.append(" and ");
            }
            String pkName =pkNameList.get(i);
            whereStr.append(keywordChecker.checkAndReplace(pkName));
            whereStr.append(" = ? ");
        }
        return whereStr.toString();

    }
    /**
     * each pk is a condition.the result will like :" id in (?,?,?) and userCode in (?,?,?)"
     * @param pkNameList
     * @param pkRowValues  the kye of map is pk name ,and value of map is pk's value
     * @return return where condition sql string.the sql can search all related records not just one.
     */
    protected String buildWhereConditionByPKs(List<String> pkNameList,Map<String,List<Field>> pkRowValues,Connection conn) throws SQLException {
        StringBuilder whereStr = new StringBuilder();
        //we must consider the situation of composite primary key
        for (int i =0;i<pkNameList.size();i++)
        {
            if(i>0){
                whereStr.append(" and ");
            }
            String pkName =pkNameList.get(i);
            whereStr.append(ColumnUtils.addEscape(pkName,getDbType(conn)));
            whereStr.append(" in ( ");
            List<Field> valueList=pkRowValues.get(pkName);
            StringBuffer pkValueStr = new StringBuffer();
            for(int r=0;r<valueList.size();r++){
                if(r>0){
                    pkValueStr.append(",");
                }
                pkValueStr.append("?");
            }
            whereStr.append(pkValueStr);
            whereStr.append(" )");
        }

        return whereStr.toString();

    }

    /**
     * Parse pk values Field List.
     *
     * @param records the records
     * @return List<List<Field>>   each element represents a row. And inside a row list contains pk columns(Field).
     */
    protected Map<String,List<Field>> parsePkValues(TableRecords records) {
        return parsePkValues(records.getRows(),records.getTableMeta().getPrimaryKeyOnlyName());
    }

    /**
     * Parse pk values Field List.
     *
     * @param rows  pk rows
     * @param pkNameList  pk column name
     * @return List<List<Field>>   each element represents a row. And inside a row list contains pk columns(Field).
     */
    protected Map<String,List<Field>> parsePkValues(List<Row> rows, List<String> pkNameList) {
        List<Field> pkFieldList = new ArrayList<>();
        for (int i = 0; i < rows.size(); i++) {
            List<Field> fields = rows.get(i).getFields();
            if (fields != null) {
                for (Field field : fields) {
                    if (pkNameList.stream().anyMatch(e->field.getName().equals(e))) {
                        pkFieldList.add(field);
                    }
                }
            }
        }
        Map<String,List<Field>> pkValueMap = pkFieldList.stream().collect(Collectors.groupingBy(Field::getName));
        return pkValueMap;
    }

    /**
     * Get db type
     *
     * @param conn the connection
     * @return the db type
     * @throws SQLException
     */
    protected String getDbType(Connection conn) throws SQLException {
        return JdbcUtils.getDbType(conn.getMetaData().getURL());
    }

}
