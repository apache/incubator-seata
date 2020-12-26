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
package io.seata.rm.datasource.exec.mysql;

import com.google.common.base.Joiner;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * @version: 1.00.00
 * @description:
 * @author: yangyicong
 * @date: 2020-11-26 09:06
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLInsertOrUpdateExecutor extends MySQLInsertExecutor implements Defaultable {

    private static final Logger LOGGER = LoggerFactory.getLogger(MySQLInsertOrUpdateExecutor.class);

    //is updated or not
    private boolean isUpdateFlag = false;

    //before image sql and after image sql,condition is unique index
    private String selectSQL;

    //the params of selectSQL, value is the unique index
    private ArrayList<List<Object>> paramAppenderList;

    //key is unique index name, value is unique index
    private Map<String, List<String>> beforeUniqueIndexMap = new HashMap<>();

    public MySQLInsertOrUpdateExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    /**
     * Execute auto commit false t.
     *
     * @param args the args
     * @return the t
     * @throws Exception the exception
     */
    @Override
    protected Object executeAutoCommitFalse(Object[] args) throws Exception {
        if (!JdbcConstants.MYSQL.equalsIgnoreCase(getDbType()) && getTableMeta().getPrimaryKeyOnlyName().size() > 1) {
            throw new NotSupportYetException("multi pk only support mysql!");
        }
        TableRecords beforeImage = beforeImage();
        if (CollectionUtils.isNotEmpty(beforeImage.getRows())) {
            isUpdateFlag = true;
        } else {
            beforeImage = TableRecords.empty(getTableMeta());
        }
        Object result = statementCallback.execute(statementProxy.getTargetStatement(), args);
        TableRecords afterImage = afterImage(beforeImage);
        prepareUndoLog2(beforeImage, afterImage);
        return result;
    }

    /**
     * prepare undo log.
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @throws SQLException the sql exception
     */
    protected void prepareUndoLog2(TableRecords beforeImage, TableRecords afterImage) {
        if (beforeImage.getRows().isEmpty() && afterImage.getRows().isEmpty()) {
            return;
        }
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        TableRecords lockKeyRecords = sqlRecognizer.getSQLType() == SQLType.DELETE ? beforeImage : afterImage;
        String lockKeys = buildLockKey(lockKeyRecords);
        connectionProxy.appendLockKey(lockKeys);
        buildUndoItemAll(connectionProxy,beforeImage, afterImage);
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @return sql undo log
     */
    protected void buildUndoItemAll(ConnectionProxy connectionProxy, TableRecords beforeImage, TableRecords afterImage) {
        if (!isUpdateFlag) {
            SQLUndoLog sqlUndoLog = buildUndoItem(TableRecords.empty(getTableMeta()), afterImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            return;
        }
        List<Row> beforeImageRows = beforeImage.getRows();
        for (Row r : beforeImageRows) {
            getTableMeta().getAllIndexes().forEach((k, v) -> {
                if (!"PRIMARY".equals(k.toUpperCase()) && !v.isNonUnique()) {
                    StringBuilder everyRowIndex = new StringBuilder();
                    v.getValues().forEach(m -> {
                        for (Field f : r.getFields()) {
                            if (m.getColumnName().equals(f.getName())) {
                                everyRowIndex.append(f.getValue());
                            }
                        }
                    });
                    List<String> indexList = beforeUniqueIndexMap.get(k.toUpperCase());
                    if (CollectionUtils.isEmpty(indexList)) {
                        indexList = new ArrayList<>();
                    }
                    indexList.add(everyRowIndex.toString());
                    beforeUniqueIndexMap.put(k.toUpperCase(), indexList);
                }
            });
        }
        List<Row> insertRows = new ArrayList<>();
        List<Row> updateRows = new ArrayList<>();
        List<Row> afterImageRows = afterImage.getRows();
        for (Row r : afterImageRows) {
            Boolean[] isUpdateRows = {false};
            getTableMeta().getAllIndexes().forEach((k, v) -> {
                if (!"PRIMARY".equals(k.toUpperCase()) && !v.isNonUnique()) {
                    StringBuilder everyRowIndex = new StringBuilder();
                    v.getValues().forEach(m -> {
                        for (Field f : r.getFields()) {
                            if (m.getColumnName().equals(f.getName())) {
                                everyRowIndex.append(f.getValue());
                            }
                        }
                    });
                    List<String> indexList = beforeUniqueIndexMap.get(k.toUpperCase());
                    if (CollectionUtils.isNotEmpty(indexList)) {
                        indexList.forEach(m -> {
                            if (m.equals(everyRowIndex.toString())) {
                                isUpdateRows[0] = true;
                            }
                        });
                    }
                }
            });
            if (isUpdateRows[0]) {
                updateRows.add(r);
            } else {
                insertRows.add(r);
            }
        }
        if (CollectionUtils.isNotEmpty(updateRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(updateRows);
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.UPDATE, beforeImage, partAfterImage));
        }
        if (CollectionUtils.isNotEmpty(insertRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(insertRows);
            connectionProxy.appendUndoLog(buildUndoItem(TableRecords.empty(getTableMeta()), partAfterImage));
        }
    }

    /**
     * build a SQLUndoLog
     * @param sqlType
     * @param beforeImage
     * @param afterImage
     * @return
     */
    protected SQLUndoLog buildUndoItem(SQLType sqlType, TableRecords beforeImage, TableRecords afterImage) {
        String tableName = sqlRecognizer.getTableName();
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }


    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        Map<String, List<Object>> pkValues = getPkValues();
        if (isUpdateFlag) {
            return buildImage();
        } else {
            return afterImageInsert(pkValues);
        }
    }

    private TableRecords afterImageInsert(Map<String, List<Object>> pkValues) throws SQLException {
        TableRecords afterImage = buildTableRecords(pkValues);
        if (afterImage == null) {
            throw new SQLException("Failed to build after-image for insert");
        }
        return afterImage;
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return buildImage();
    }

    /**
     * build before image and after image
     * @return
     * @throws SQLException
     */
    protected TableRecords buildImage() throws SQLException {
        TableMeta tmeta = getTableMeta();
        //after image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            paramAppenderList = new ArrayList<>();
            selectSQL = buildImageSQL(tmeta);
        }
        return buildTableRecords2(tmeta, selectSQL, paramAppenderList);
    }

    /**
     *  build TableRecords
     * @param tableMeta
     * @param selectSQL
     * @param paramAppenderList
     * @return
     * @throws SQLException
     */
    protected TableRecords buildTableRecords2(TableMeta tableMeta, String selectSQL, ArrayList<List<Object>> paramAppenderList) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(selectSQL)) {
            if (CollectionUtils.isNotEmpty(paramAppenderList)) {
                for (int i = 0, ts = paramAppenderList.size(); i < ts; i++) {
                    List<Object> paramAppender = paramAppenderList.get(i);
                    for (int j = 0, ds = paramAppender.size(); j < ds; j++) {
                        ps.setObject(i * ds + j + 1, "NULL".equals(paramAppender.get(j).toString()) ? null : paramAppender.get(j));
                    }
                }
            }
            rs = ps.executeQuery();
            return TableRecords.buildRecords(tableMeta, rs);
        } finally {
            IOUtil.close(rs);
        }
    }

    /**
     * build image sql
     * @param tableMeta
     * @return
     */
    private String buildImageSQL(TableMeta tableMeta) {
        if (CollectionUtils.isEmpty(paramAppenderList)) {
            paramAppenderList = new ArrayList<>();
        }
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        Map<String, ArrayList<Object>> imageParamperterMap = new HashMap<>();
        int insertNum = buildImageParamperters(recognizer, imageParamperterMap);
        StringBuilder prefix = new StringBuilder("SELECT * ");
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        boolean[] isContainWhere = {false};
        for (int i = 0; i < insertNum; i++) {
            int finalI = i;
            List<Object> paramAppenderTempList = new ArrayList<>();
            tableMeta.getAllIndexes().forEach((k, v) -> {
                if (!v.isNonUnique()) {
                    List<String> uniqueList = new ArrayList<>();
                    for (ColumnMeta m : v.getValues()) {
                        String columnName = m.getColumnName();
                        if (imageParamperterMap.get(columnName) == null || imageParamperterMap.get(columnName).get(finalI) == null) {
                            continue;
                        }
                        uniqueList.add(columnName + " = ? ");
                        paramAppenderTempList.add(imageParamperterMap.get(columnName).get(finalI));
                    }
                    if (isContainWhere[0]) {
                        suffix.append(" OR (").append(Joiner.on(" and ").join(uniqueList)).append(") ");
                    } else {
                        suffix.append(" WHERE (").append(Joiner.on(" and ").join(uniqueList)).append(") ");
                        isContainWhere[0] = true;
                    }
                }
            });
            paramAppenderList.add(paramAppenderTempList);
        }
        suffix.append(" FOR UPDATE");
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix.toString(), suffix.toString());
        return selectSQLJoin.toString();
    }

    /**
     * build sql params
     * @param recognizer
     * @param imageParamperterMap
     * @return
     */
    private int buildImageParamperters(SQLInsertRecognizer recognizer, Map<String, ArrayList<Object>> imageParamperterMap) {
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        //  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        List<String> insertParamsList = recognizer.getInsertParamsValue();
        List<String> insertColumns = recognizer.getInsertColumns();
        int paramsindex = 1;
        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = insertColumns.get(i);
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParamperterMap.get(m);
                if (imageListTemp == null) {
                    imageListTemp = new ArrayList<>();
                }
                if ("?".equals(params.toString().trim())) {
                    ArrayList<Object> objects = parameters.get(paramsindex);
                    imageListTemp.addAll(objects);
                    paramsindex++;
                } else if (params != null && params instanceof String) {
                    // params is characterstring constant
                    if ((params.trim().startsWith("'") && params.trim().endsWith("'")) || params.trim().startsWith("\"") && params.trim().endsWith("\"")) {
                        params = params.trim();
                        params = params.substring(1,params.length() - 1);
                    }
                    imageListTemp.add(params);
                } else {
                    imageListTemp.add(params);
                }
                imageParamperterMap.put(m, imageListTemp);
            }
        }
        return insertParamsList.size();
    }

}
