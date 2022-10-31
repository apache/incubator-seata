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
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.LoadLevel;
import io.seata.common.loader.Scope;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.LowerCaseLinkHashMap;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Defaultable;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.util.JdbcConstants;

/**
 * @author yangyicong
 */
@LoadLevel(name = JdbcConstants.MYSQL, scope = Scope.PROTOTYPE)
public class MySQLInsertOnDuplicateUpdateExecutor extends MySQLInsertExecutor implements Defaultable {

    private static final String COLUMN_SEPARATOR = "|";

    public String getSelectSQL() {
        return selectSQL;
    }

    /**
     * just for test
     *
     * @param selectSQL select sql
     */
    public void setSelectSQL(String selectSQL) {
        this.selectSQL = selectSQL;
    }

    /**
     * before image sql and after image sql,condition is unique index
     */
    protected String selectSQL;

    public HashMap<List<String>, List<Object>> getParamAppenderMap() {
        return paramAppenderMap;
    }

    /**
     * the params of selectSQL, value is the unique index
     */
    public HashMap<List<String>, List<Object>> paramAppenderMap;

    /**
     * for test
     *
     * @param paramAppenderMap paramAppenderMap
     */
    public void setParamAppenderMap(HashMap<List<String>, List<Object>> paramAppenderMap) {
        this.paramAppenderMap = paramAppenderMap;
    }

    public MySQLInsertOnDuplicateUpdateExecutor(StatementProxy statementProxy, StatementCallback statementCallback, SQLRecognizer sqlRecognizer) {
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
        if (CollectionUtils.isEmpty(beforeImage.getRows())) {
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
        Map<SQLType, List<Row>> updateAndInsertRow = getUpdateAndInsertRow(beforeImage, afterImage);
        List<Row> insertRows = updateAndInsertRow.get(SQLType.INSERT);
        List<Row> updateRows = updateAndInsertRow.get(SQLType.UPDATE);
        if (CollectionUtils.isNotEmpty(updateRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(updateRows);
            if (beforeImage.getRows().size() != partAfterImage.getRows().size()) {
                throw new ShouldNeverHappenException("Before image size is not equaled to after image size, probably because you updated the primary keys.");
            }
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.UPDATE, beforeImage, partAfterImage));
        }
        if (CollectionUtils.isNotEmpty(insertRows)) {
            TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
            partAfterImage.setTableName(afterImage.getTableName());
            partAfterImage.setRows(insertRows);
            connectionProxy.appendUndoLog(buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), partAfterImage));
        }
    }

    /**
     * if beforeImage and afterImage both have,then sql type is update
     * if afterImage have but beforeImage have not, then sql type is insert
     *
     * @param beforeImage before image
     * @param afterImage  after image
     * @return map
     */
    protected Map<SQLType, List<Row>> getUpdateAndInsertRow(TableRecords beforeImage, TableRecords afterImage) {
        Map<SQLType, List<Row>> result = new HashMap<>(2, 1.001f);
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
        List<Row> updateRows = new ArrayList<>();
        List<Row> afterImageRows = afterImage.getRows();
        for (Row r : afterImageRows) {
            String primaryValue = "";
            for (Field f: r.primaryKeys()) {
                primaryValue = primaryValue + f.getValue()  + COLUMN_SEPARATOR;
            }
            if (beforePrimaryValues.contains(primaryValue)) {
                updateRows.add(r);
            } else {
                insertRows.add(r);
            }
        }
        result.put(SQLType.INSERT, insertRows);
        result.put(SQLType.UPDATE, updateRows);
        return result;
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


    @Override
    public TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        TableMeta tableMeta = getTableMeta();

        List<Row> rows = beforeImage.getRows();
        Map<List<String>, ArrayList<Object>> primaryValueMap = new HashMap<>();
        AtomicReference<List<String>> nameList = new AtomicReference<>();
        rows.forEach(m -> {
            List<Field> fields = m.primaryKeys();
            nameList.set(fields.stream().map(Field::getName).collect(Collectors.toList()));
            ArrayList<Object> tempList = new ArrayList<>();
            fields.forEach(f -> tempList.add(f.getValue()));
            primaryValueMap.computeIfAbsent(nameList.get(), v -> new ArrayList<>()).addAll(tempList);
        });

        // The origin select sql contains the unique keys sql
        StringBuilder afterImageSql = new StringBuilder(selectSQL);
        List<Object> primaryValues = new ArrayList<>();

        // Appends the pk when the origin select sql not contains
        if (CollectionUtils.isNotEmpty(primaryValueMap)) {
            primaryValueMap.forEach((columnsName, columnsValue) -> {
                afterImageSql.append("OR (");
                afterImageSql.append(Joiner.on(",").join(columnsName));
                afterImageSql.append(") in(");
                for (int i = 0; i < columnsValue.size() / columnsName.size(); i++) {
                    afterImageSql.append("(");
                    for (int j = 0; j < columnsName.size(); j++) {
                        afterImageSql.append("?,");
                    }
                    afterImageSql.insert(afterImageSql.length() - 1, ")");
                }
                afterImageSql.deleteCharAt(afterImageSql.length() - 1);
                afterImageSql.append(")");
            });
        }
        ArrayList<Object> pkList = new ArrayList<>();
        primaryValueMap.values().forEach(pkList::addAll);
        return buildTableRecords2(tableMeta, afterImageSql.toString(), new ArrayList<>(paramAppenderMap.values()), pkList);
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        // After image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            selectSQL = buildImageSQL(tableMeta);
        }
        if (CollectionUtils.isEmpty(paramAppenderMap)) {
            throw new ShouldNeverHappenException("can not find unique param,may be you should add unique key" +
                    " when use the sqlType of " + sqlRecognizer.getSQLType().getName());
        }
        return buildTableRecords2(tableMeta, selectSQL, new ArrayList<>(paramAppenderMap.values()), Collections.emptyList());
    }

    /**
     * build TableRecords
     *
     * @param tableMeta  the meta info of  table
     * @param selectSQL  the sql to select images
     * @param paramAppenderList the param list
     * @param primaryKeys the primary keys
     * @return the table records
     * @throws SQLException then execute fail
     */
    public TableRecords buildTableRecords2(TableMeta tableMeta, String selectSQL, ArrayList<List<Object>> paramAppenderList, List<Object> primaryKeys) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection()
            .prepareStatement(primaryKeys.isEmpty() ? selectSQL + " FOR UPDATE" : selectSQL)) {
            int ts = CollectionUtils.isEmpty(paramAppenderList) ? 0 : paramAppenderList.size();
            int ds = ts == 0 ? 0 : paramAppenderList.get(0).size();
            for (int i = 0; i < ts; i++) {
                List<Object> paramAppender = paramAppenderList.get(i);
                for (int j = 0; j < ds; j++) {
                    ps.setObject(i * ds + j + 1, "NULL".equals(paramAppender.get(j).toString()) ? null : paramAppender.get(j));
                }
            }
            for (int i = 0; i < primaryKeys.size(); i++) {
                ps.setObject(ts * ds + i + 1, primaryKeys.get(i));
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
     * @param tableMeta the meta info of  table
     * @return image sql
     */
    public String buildImageSQL(TableMeta tableMeta) {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        int insertNum = getInsertParamsValue().size();
        Map<String, ArrayList<Object>> imageParameterMap = buildImageParameters(recognizer);
        if (Objects.isNull(paramAppenderMap)) {
            paramAppenderMap = new HashMap<>();
        }
        List<Object> nullList = new ArrayList<>();
        List<String> nullColumn = new ArrayList<>();
        String prefix = "SELECT * ";
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        for (int i = 0; i < insertNum; i++) {
            int finalI = i;
            tableMeta.getAllIndexes().forEach((k, v) -> {
                if (!v.isNonUnique()) {
                    List<String> columnList = new ArrayList<>(v.getValues().size());
                    List<Object> columnValue = new ArrayList<>(v.getValues().size());
                    for (ColumnMeta m : v.getValues()) {
                        String columnName = m.getColumnName();
                        if (JdbcConstants.ORACLE.equals(getDbType()) && recognizer.isIgnore()
                                && !columnName.equals(ColumnUtils.delEscape(recognizer.getHintColumnName(), getDbType()))) {
                            break;
                        }
                        List<Object> imageParameters = imageParameterMap.get(columnName);
                        if (imageParameters == null && m.getColumnDef() != null) {
                            columnList.add(columnName);
                            columnValue.add("DEFAULT(" + columnName + ")");
                            continue;
                        }
                        if ((imageParameters == null && m.getColumnDef() == null) || imageParameters.get(finalI) == null || imageParameters.get(finalI) instanceof Null) {
                            if (!"PRIMARY".equalsIgnoreCase(k) && !IndexType.PRIMARY.equals(v.getIndextype())) {
                                nullColumn.add("(" + columnName + " is null or " + columnName + " = ?)");
                                nullList.add("NULL");
                                continue;
                            }
                            // break for the situation of composite primary key
                            break;
                        }
                        columnList.add(columnName);
                        columnValue.add(imageParameterMap.get(columnName).get(finalI));
                    }
                    if (CollectionUtils.isNotEmpty(columnList)) {
                        CollectionUtils.computeIfAbsent(paramAppenderMap, columnList, e -> new ArrayList<>())
                                .addAll(columnValue);
                    }
                }
            });
        }
        suffix.append(" WHERE ");
        if (CollectionUtils.isNotEmpty(nullColumn)) {
            paramAppenderMap.put(nullColumn, nullList);
        }
        paramAppenderMap.forEach((columnsName, columnsValue) -> {
            if (columnsName.equals(nullColumn)) {
                suffix.append(Joiner.on(" OR ").join(nullColumn));
                suffix.append(" OR ");
                return;
            }
            suffix.append("(");
            suffix.append(Joiner.on(",").join(columnsName));
            suffix.append(") in(");
            for (int i = 0; i < columnsValue.size() / columnsName.size(); i++) {
                suffix.append("(");
                for (int j = 0; j < columnsName.size(); j++) {
                    suffix.append("?,");
                }
                suffix.insert(suffix.length() - 1, ")");
            }
            suffix.deleteCharAt(suffix.length() - 1);
            suffix.append(") OR ");
        });
        suffix.delete(suffix.length() - 4, suffix.length() - 1);
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix, suffix.toString());
        return selectSQLJoin.toString();
    }

    /**
     * build sql params
     *
     * @param recognizer the sql recognizer
     * @return map, key is column, value is paramperter
     */
    @SuppressWarnings("lgtm[java/dereferenced-value-may-be-null]")
    public Map<String, ArrayList<Object>> buildImageParameters(SQLInsertRecognizer recognizer) {
        List<String> duplicateKeyUpdateColumns = recognizer.getDuplicateKeyUpdate();
        if (CollectionUtils.isNotEmpty(duplicateKeyUpdateColumns)) {
            List<String> duplicateKeyUpdateLowerCaseColumns =
                duplicateKeyUpdateColumns.parallelStream().map(String::toLowerCase).collect(Collectors.toList());
            getTableMeta().getAllIndexes().forEach((k, v) -> {
                if ("PRIMARY".equalsIgnoreCase(k) && !IndexType.PRIMARY.equals(v.getIndextype())) {
                    for (ColumnMeta m : v.getValues()) {
                        if (duplicateKeyUpdateLowerCaseColumns.contains(m.getColumnName().toLowerCase())) {
                            throw new ShouldNeverHappenException("update pk value is not supported!");
                        }
                    }
                }
            });
        }
        Map<String, ArrayList<Object>> imageParameterMap = new LowerCaseLinkHashMap<>();
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        //  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        List<String> insertParamsList = getInsertParamsValue();
        List<String> insertColumns = Optional.ofNullable(recognizer.getInsertColumns()).map(list -> list.stream()
                .map(column -> ColumnUtils.delEscape(column, getDbType())).collect(Collectors.toList())).orElse(null);
        if (CollectionUtils.isEmpty(insertColumns)) {
            insertColumns = new ArrayList<>(getTableMeta().getAllColumns().keySet());
        }
        int paramsindex = 1;
        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = ColumnUtils.delEscape(insertColumns.get(i), getDbType());
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParameterMap.computeIfAbsent(m, k -> new ArrayList<>());
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
                imageParameterMap.put(m, imageListTemp);
            }
        }
        return imageParameterMap;
    }

    /**
     * just for the different recognize or sql
     * normal to see {@link SQLInsertRecognizer#getInsertParamsValue}
     *
     * @return
     */
    protected List<String> getInsertParamsValue() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        return recognizer.getInsertParamsValue();
    }
}
