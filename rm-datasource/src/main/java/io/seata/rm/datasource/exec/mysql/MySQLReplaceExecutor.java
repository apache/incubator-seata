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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

import com.google.common.base.Joiner;
import io.seata.common.exception.ShouldNeverHappenException;
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
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLReplaceRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author jingliu_xiong@foxmail.com
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLReplaceExecutor extends MySQLInsertExecutor implements Defaultable {
    /**
     * before image sql and after image sql,condition is unique index
     */
    private String selectSQL;
    private static final String COLUMN_SEPARATOR = "|";
    /**
     * the params of selectSQL, value is the unique index
     */
    private ArrayList<List<Object>> paramAppenderList;

    /**
     * is the replaced data exist
     */
    private boolean isExistFlag = false;

    public MySQLReplaceExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    public String getSelectSQL() {
        return selectSQL;
    }

    public ArrayList<List<Object>> getParamAppenderList() {
        return paramAppenderList;
    }

    @Override
    protected Object executeAutoCommitFalse(Object[] args) throws Exception {
        TableRecords beforeImage = beforeImage();
        // replace rows are not existed
        if (CollectionUtils.isNotEmpty(beforeImage.getRows())) {
            isExistFlag = true;
        } else {
            beforeImage = TableRecords.empty(getTableMeta());
        }
        Object result = statementCallback.execute(statementProxy.getTargetStatement(), args);
        int updateCount = statementProxy.getUpdateCount();
        if (updateCount > 0) {
            TableRecords afterImage = afterImage(beforeImage);
            prepareUndoLogAll(beforeImage, afterImage);
        }
        return result;
    }

    @Override
    public TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        TableMeta tableMeta = getTableMeta();
        List<Row> rows = beforeImage.getRows();
        Map<String, ArrayList<Object>> primaryValueMap = new HashMap<>();
        rows.forEach(m -> {
            List<Field> fields = m.primaryKeys();
            fields.forEach(f -> {
                ArrayList<Object> values = primaryValueMap.computeIfAbsent(f.getName(), v -> new ArrayList<>());
                values.add(f.getValue());
            });
        });

        StringBuilder afterImageSql = new StringBuilder(selectSQL);
        for (int i = 0; i < rows.size(); i++) {
            int finalI = i;
            List<String> wherePrimaryList = new ArrayList<>();
            primaryValueMap.forEach((k, v) -> {
                wherePrimaryList.add(k + " = " +  primaryValueMap.get(k).get(finalI) + " ");
            });
            afterImageSql.append(" OR (").append(Joiner.on(" and ").join(wherePrimaryList)).append(") ");
        }
        return buildTableRecords2(tableMeta, afterImageSql.toString(), paramAppenderList);
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tmeta = getTableMeta();
        //after image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            paramAppenderList = new ArrayList<>();
            selectSQL = buildImageSQL(tmeta);
        }
        return buildTableRecords2(tmeta, selectSQL, paramAppenderList);
    }

    /**
     * build TableRecords
     *
     * @param tableMeta
     * @param selectSQL
     * @param paramAppenderList
     * @return the table records
     * @throws SQLException
     */
    public TableRecords buildTableRecords2(TableMeta tableMeta, String selectSQL, ArrayList<List<Object>> paramAppenderList) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(selectSQL + " FOR UPDATE")) {
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
     *
     * @param tableMeta
     * @return image sql
     */
    public String buildImageSQL(TableMeta tableMeta) {
        if (CollectionUtils.isEmpty(paramAppenderList)) {
            paramAppenderList = new ArrayList<>();
        }
        SQLReplaceRecognizer recognizer = (SQLReplaceRecognizer) sqlRecognizer;
        int replaceNum = recognizer.getReplaceValues().size();
        Map<String, ArrayList<Object>> imageParameterMap = buildImageParameters(recognizer);
        StringBuilder prefix = new StringBuilder("SELECT * ");
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        // TODO: implement subSelect before image
        if (!recognizer.selectQueryIsEmpty()) {
            throw new ShouldNeverHappenException("replace include subQuery is not implemented now!");
        }
        boolean[] isContainWhere = {false};
        for (int i = 0; i < replaceNum; i++) {
            int finalI = i;
            List<Object> paramAppenderTempList = new ArrayList<>();
            tableMeta.getAllIndexes().forEach((k, v) -> {
                if (!v.isNonUnique()) {
                    boolean columnIsNull = true;
                    List<String> uniqueList = new ArrayList<>();
                    for (ColumnMeta m : v.getValues()) {
                        String columnName = m.getColumnName();
                        if (imageParameterMap.get(columnName) == null && m.getColumnDef() != null) {
                            uniqueList.add(columnName + " = DEFAULT(" + columnName + ") ");
                            columnIsNull = false;
                            continue;
                        }
                        if ((imageParameterMap.get(columnName) == null && m.getColumnDef() == null) || imageParameterMap.get(columnName).get(finalI) == null || imageParameterMap.get(columnName).get(finalI) instanceof Null) {
                            if (!"PRIMARY".equalsIgnoreCase(k)) {
                                columnIsNull = false;
                                uniqueList.add(columnName + " is ? ");
                                paramAppenderTempList.add("NULL");
                                continue;
                            }
                            break;
                        }
                        columnIsNull = false;
                        uniqueList.add(columnName + " = ? ");
                        paramAppenderTempList.add(imageParameterMap.get(columnName).get(finalI));
                    }
                    if (!columnIsNull) {
                        if (isContainWhere[0]) {
                            suffix.append(" OR (").append(Joiner.on(" and ").join(uniqueList)).append(") ");
                        } else {
                            suffix.append(" WHERE (").append(Joiner.on(" and ").join(uniqueList)).append(") ");
                            isContainWhere[0] = true;
                        }
                    }
                }
            });
            paramAppenderList.add(paramAppenderTempList);
        }
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix.toString(), suffix.toString());
        return selectSQLJoin.toString();
    }

    /**
     * build sql params
     *
     * @param recognizer
     * @return map, key is column, value is paramperter
     */
    @SuppressWarnings("lgtm[java/dereferenced-value-may-be-null]")
    public Map<String, ArrayList<Object>> buildImageParameters(SQLReplaceRecognizer recognizer) {
        List<String> replaceColumns = recognizer.getReplaceColumns();
        Map<String, ArrayList<Object>> imageParamperterMap = new HashMap<>();
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        //  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        List<String> replaceParamsList = recognizer.getReplaceValues();
        int paramsindex = 1;
        for (String replaceParams : replaceParamsList) {
            String[] replaceParamsArray = replaceParams.split(",");
            if (replaceColumns == null) {
                Map<String, ColumnMeta> allColumns = getTableMeta().getAllColumns();
                int i = 0;
                for (Map.Entry<String, ColumnMeta> column : allColumns.entrySet()) {
                    String m = column.getKey();
                    ArrayList<Object> imageListTemp = imageParamperterMap.computeIfAbsent(m, k -> new ArrayList<>());
                    String params = replaceParamsArray[i];
                    if ("?".equals(params.trim())) {
                        ArrayList<Object> objects = parameters.get(paramsindex);
                        imageListTemp.addAll(objects);
                        paramsindex++;
                    } else {
                        // params is character string constant
                        if ((params.trim().startsWith("'") && params.trim().endsWith("'")) || params.trim().startsWith("\"") && params.trim().endsWith("\"")) {
                            params = params.trim();
                            params = params.substring(1, params.length() - 1);
                        }
                        imageListTemp.add(params);
                    }
                    imageParamperterMap.put(m, imageListTemp);
                }
                continue;
            }
            for (int i = 0; i < replaceColumns.size(); i++) {
                String m = replaceColumns.get(i);
                String params = replaceParamsArray[i];
                ArrayList<Object> imageListTemp = imageParamperterMap.computeIfAbsent(m, k -> new ArrayList<>());
                if ("?".equals(params.trim())) {
                    ArrayList<Object> objects = parameters.get(paramsindex);
                    imageListTemp.addAll(objects);
                    paramsindex++;
                } else {
                    // params is character string constant
                    if ((params.trim().startsWith("'") && params.trim().endsWith("'")) || params.trim().startsWith("\"") && params.trim().endsWith("\"")) {
                        params = params.trim();
                        params = params.substring(1, params.length() - 1);
                    }
                    imageListTemp.add(params);
                }
                imageParamperterMap.put(m, imageListTemp);
            }
        }
        return imageParamperterMap;
    }

    /**
     * prepare undo log.
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     */
    protected void prepareUndoLogAll(TableRecords beforeImage, TableRecords afterImage) {
        if (beforeImage.getRows().isEmpty() && afterImage.getRows().isEmpty()) {
            return;
        }
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        String lockKeys = buildLockKey(afterImage);
        connectionProxy.appendLockKey(lockKeys);
        buildUndoItemAll(connectionProxy, beforeImage, afterImage);
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     */
    protected void buildUndoItemAll(ConnectionProxy connectionProxy, TableRecords beforeImage, TableRecords afterImage) {
        if (!isExistFlag) {
            SQLUndoLog sqlUndoLog = buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), afterImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            return;
        }
        List<Row> beforeImageRows = beforeImage.getRows();
        List<String> beforePrimaryValues = new ArrayList<>();
        for (Row r : beforeImageRows) {
            String primaryValue = "";
            for (Field f: r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue() + COLUMN_SEPARATOR;
            }
            beforePrimaryValues.add(primaryValue);
        }
        List<Row> insertRows = new ArrayList<>();
        List<Row> replaceRows = new ArrayList<>();
        List<Row> afterImageRows = afterImage.getRows();
        for (Row r : afterImageRows) {
            String primaryValue = "";
            for (Field f: r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue()  + COLUMN_SEPARATOR;
            }
            if (beforePrimaryValues.contains(primaryValue)) {
                replaceRows.add(r);
            } else {
                insertRows.add(r);
            }
        }
        if (CollectionUtils.isNotEmpty(replaceRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(replaceRows);
            if (beforeImage.getRows().size() != partAfterImage.getRows().size()) {
                throw new ShouldNeverHappenException("Before image size is not equaled to after image size, probably because you updated the primary keys.");
            }
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.REPLACE, beforeImage, partAfterImage));
        }
        if (CollectionUtils.isNotEmpty(insertRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(insertRows);
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), partAfterImage));
        }
    }

    /**
     * build a SQLUndoLog
     *
     * @param sqlType
     * @param beforeImage
     * @param afterImage
     * @return sqlUndoLog the sql undo log
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
}
