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
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Collections;
import java.util.Set;
import java.util.StringJoiner;
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
import io.seata.sqlparser.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.struct.IndexMeta;
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

    /**
     * is updated or not
     */
    private boolean isUpdateFlag = false;

    public String getSelectSQL() {
        return selectSQL;
    }

    /**
     * before image sql and after image sql,condition is unique index
     */
    private String selectSQL;

    public ArrayList<List<Object>> getParamAppenderList() {
        return paramAppenderList;
    }

    /**
     * the params of selectSQL, value is the unique index
     */
    private ArrayList<List<Object>> paramAppenderList;

    /**
     * the primary keys in before image sql. if the primary key is auto increment,the set is empty
     */
    private Set<String> primaryKeysInBeforeImageSql = new HashSet<>(4);

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
        if (CollectionUtils.isNotEmpty(beforeImage.getRows())) {
            isUpdateFlag = true;
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
        if (!isUpdateFlag) {
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
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
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

        // The origin select sql contains the unique keys sql
        StringBuilder afterImageSql = new StringBuilder(selectSQL);
        List<Object> primaryValues = new ArrayList<>();

        // Appends the pk when the origin select sql not contains
        for (int i = 0; i < rows.size(); i++) {
            List<String> wherePrimaryList = new ArrayList<>();
            primaryValueMap.forEach((k, v) -> {
                if (!primaryKeysInBeforeImageSql.contains(k)) {
                    wherePrimaryList.add(k + " = ? ");
                    primaryValues.add(v);
                }
            });
            if (wherePrimaryList.size() > 0) {
                afterImageSql.append(" OR (").append(Joiner.on(" and ").join(wherePrimaryList)).append(") ");
            }
        }

        return buildTableRecords2(tableMeta, afterImageSql.toString(), paramAppenderList, primaryValues);
    }

    @Override
    public TableRecords beforeImage() throws SQLException {
        TableMeta tableMeta = getTableMeta();
        // After image sql the same of before image
        if (StringUtils.isBlank(selectSQL)) {
            paramAppenderList = new ArrayList<>();
            selectSQL = buildImageSQL(tableMeta);
        }
        return buildTableRecords2(tableMeta, selectSQL, paramAppenderList, Collections.emptyList());
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
        if (CollectionUtils.isEmpty(paramAppenderList)) {
            throw new NotSupportYetException("the SQL statement has no primary key or unique index value, it will not hit any row data.recommend to convert to a normal insert statement");
        }
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection()
            .prepareStatement(primaryKeys.isEmpty() ? selectSQL + " FOR UPDATE" : selectSQL)) {
            int paramAppenderCount = 0;
            int ts = CollectionUtils.isEmpty(paramAppenderList) ? 0 : paramAppenderList.size();
            for (int i = 0; i < ts; i++) {
                List<Object> paramAppender = paramAppenderList.get(i);
                for (int j = 0; j < paramAppender.size(); j++) {
                    Object param = paramAppender.get(j);
                    ps.setObject(paramAppenderCount + 1, (param instanceof Null) ? null : param);
                    paramAppenderCount++; 
                }
            }
            for (int i = 0; i < primaryKeys.size(); i++) {
                ps.setObject(paramAppenderCount + i + 1, primaryKeys.get(i));
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
        if (CollectionUtils.isEmpty(paramAppenderList)) {
            paramAppenderList = new ArrayList<>();
        }
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        int insertNum = recognizer.getInsertRows(getPkIndex().values()).size();
        Map<String, ArrayList<Object>> imageParameterMap = buildImageParameters(recognizer);
        String prefix = "SELECT * ";
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        boolean[] isContainWhere = {false};
        for (int i = 0; i < insertNum; i++) {
            int finalI = i;
            List<Object> paramAppenderTempList = new ArrayList<>();
            tableMeta.getAllIndexes().forEach((k, v) -> {
                if (!v.isNonUnique() && isIndexValueNotNull(v, imageParameterMap, finalI)) {
                    boolean columnIsNull = true;
                    List<String> uniqueList = new ArrayList<>();
                    for (ColumnMeta m : v.getValues()) {
                        String columnName = m.getColumnName();
                        List<Object> imageParameters = imageParameterMap.get(columnName);
                        if (imageParameters == null && m.getColumnDef() != null) {
                            uniqueList.add(columnName + " = DEFAULT(" + columnName + ") ");
                            if ("PRIMARY".equalsIgnoreCase(k)) {
                                primaryKeysInBeforeImageSql.add(columnName);
                            }
                            columnIsNull = false;
                            continue;
                        }
                        if ("PRIMARY".equalsIgnoreCase(k)) {
                            primaryKeysInBeforeImageSql.add(columnName);
                        }
                        columnIsNull = false;
                        uniqueList.add(columnName + " = ? ");
                        paramAppenderTempList.add(imageParameters.get(finalI));
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
            if (CollectionUtils.isNotEmpty(paramAppenderTempList)) {
                paramAppenderList.add(paramAppenderTempList);
            }
        }
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
                if ("PRIMARY".equalsIgnoreCase(k)) {
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
        List<String> sqlRecognizerColumns = recognizer.getInsertColumns();
        List<String> insertColumns = CollectionUtils.isEmpty(sqlRecognizerColumns) ? new ArrayList<>(getTableMeta().getAllColumns().keySet()) : sqlRecognizerColumns;
        final Map<String,Integer> pkIndexMap = getPkIndex();
        List<List<Object>> insertRows = recognizer.getInsertRows(pkIndexMap.values());
        int placeHolderIndex = 1;
        for (List<Object> row : insertRows) {
            if (row.size() != insertColumns.size()) {
                throw new IllegalArgumentException("insert row's size is not equal to column size");
            }
            for (int i = 0;i < insertColumns.size();i++) {
                String column = ColumnUtils.delEscape(insertColumns.get(i),getDbType());
                Object value = row.get(i);
                ArrayList<Object> columnImages = imageParameterMap.computeIfAbsent(column, k -> new ArrayList<>());
                if (PLACEHOLDER.equals(value)) {
                    ArrayList<Object> objects = parameters.get(placeHolderIndex);
                    columnImages.addAll(objects);
                    placeHolderIndex++;
                } else {
                    columnImages.add(value);
                }
                imageParameterMap.put(column,columnImages);
            }
        }
        return imageParameterMap;
    }

    private boolean isIndexValueNotNull(IndexMeta indexMeta, Map<String, ArrayList<Object>> imageParameterMap, int rowIndex) {
        for (ColumnMeta columnMeta : indexMeta.getValues()) {
            String columnName = columnMeta.getColumnName();
            List<Object> imageParameters = imageParameterMap.get(columnName);
            if (imageParameters == null && columnMeta.getColumnDef() == null) {
                return false;
            } else if (imageParameters != null && (imageParameters.get(rowIndex) == null || imageParameters.get(rowIndex) instanceof Null)) {
                return false;
            }
        }
        return true;
    }

}
