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
package io.seata.rm.datasource.exec;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.seata.common.exception.NotSupportYetException;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.rm.datasource.ColumnUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.PreparedStatementProxy;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.struct.ColumnMeta;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.IndexType;
import io.seata.rm.datasource.sql.struct.Row;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.struct.Null;
import io.seata.sqlparser.struct.Sequenceable;
import io.seata.sqlparser.struct.SqlDefaultExpr;
import io.seata.sqlparser.struct.SqlMethodExpr;
import io.seata.sqlparser.struct.SqlSequenceExpr;
import io.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Base Insert Executor.
 * @author jsbxyyx
 */
public abstract class BaseInsertExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> implements InsertExecutor<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(BaseInsertExecutor.class);

    protected static final String PLACEHOLDER = "?";

    protected static final String FOR_UPDATE = " FOR UPDATE";

    private static final String SELECT = "SELECT";

    /**
     * the params of selectSQL, value is the unique index
     */
    public HashMap<List<String>, List<Object>> paramAppenderMap;

    public HashMap<List<String>, List<Object>> getParamAppenderMap() {
        return paramAppenderMap;
    }

    /**
     * just for test
     *
     * @param paramAppenderMap paramAppenderMap
     */
    public void setParamAppenderMap(HashMap<List<String>, List<Object>> paramAppenderMap) {
        this.paramAppenderMap = paramAppenderMap;
    }
    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public BaseInsertExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                              SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        return TableRecords.empty(getTableMeta());
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        Map<String, List<Object>> pkValues = getPkValues();
        TableRecords afterImage = buildTableRecords(pkValues);
        if (afterImage == null) {
            throw new SQLException("Failed to build after-image for insert");
        }
        return afterImage;
    }

    @Override
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) {
        if (beforeImage.getRows().isEmpty() && afterImage.getRows().isEmpty()) {
            return;
        }
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        TableRecords lockKeyRecords = afterImage;
        String lockKeys = buildLockKey(lockKeyRecords);
        connectionProxy.appendLockKey(lockKeys);
        buildUndoItemAll(connectionProxy, beforeImage, afterImage);
    }

    protected boolean containsPK() {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isEmpty(insertColumns)) {
            return false;
        }
        return containsPK(insertColumns);
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     */
    protected void buildUndoItemAll(ConnectionProxy connectionProxy, TableRecords beforeImage, TableRecords afterImage) {
        // the situation of normal insert or insert select when select is empty
        if (CollectionUtils.isEmpty(beforeImage.getRows())) {
            SQLUndoLog sqlUndoLog = buildUndoItem(SQLType.INSERT, TableRecords.empty(getTableMeta()), afterImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            return;
        }
        Map<SQLType, List<Row>> undoRowMap = buildUndoRow(beforeImage, afterImage);
        undoRowMap.forEach((sqlType, rows) -> {
            if (CollectionUtils.isNotEmpty(rows)) {
                TableRecords partAfterImage = new TableRecords(afterImage.getTableMeta());
                partAfterImage.setRows(rows);
                if (SQLType.INSERT == sqlType) {
                    connectionProxy.appendUndoLog(buildUndoItem(sqlType, TableRecords.empty(getTableMeta()), partAfterImage));
                }
                if (SQLType.UPDATE == sqlType) {
                    connectionProxy.appendUndoLog(buildUndoItem(sqlType, beforeImage, partAfterImage));
                }
            }
        });
    }

    /**
     * build undo row when happens collision,
     * when there has no collision but execute method, just throw exception
     *
     * @param beforeImage before image
     * @param afterImage  after image
     * @return Map<SQLType, List < Row>>
     */
    protected Map<SQLType, List<Row>> buildUndoRow(TableRecords beforeImage, TableRecords afterImage) {
        throw new ShouldNeverHappenException("");
    }

    /**
     * build a SQLUndoLog
     *
     * @param sqlType     sql type
     * @param beforeImage before image
     * @param afterImage  after image
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

    /**
     * build tableRecords from databases
     *
     * @param tableMeta      tableMeta
     * @param selectSQL      the select SQL contain eg: select * from test where id in (?,?)
     * @param sequenceValues the values to fill the select SQL by sequence
     * @return TableRecords
     * @throws SQLException
     */
    public TableRecords buildTableRecords2(TableMeta tableMeta, String selectSQL, List<List<Object>> sequenceValues) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(selectSQL + FOR_UPDATE)) {
            if (CollectionUtils.isNotEmpty(sequenceValues)) {
                int i = 1;
                for (List<Object> sequenceValue : sequenceValues) {
                    for (Object o : sequenceValue) {
                        ps.setObject(i++, o);
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
     * judge sql specify column
     * @return true: contains column. false: not contains column.
     */
    protected boolean containsColumns() {
        return !((SQLInsertRecognizer) sqlRecognizer).insertColumnsIsEmpty();
    }

    /**
     * get pk index
     * @return the key is pk column name and the value is index of the pk column
     */
    protected Map<String, Integer> getPkIndex() {
        Map<String, Integer> pkIndexMap = new HashMap<>();
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        List<String> insertColumns = recognizer.getInsertColumns();
        if (CollectionUtils.isNotEmpty(insertColumns)) {
            final int insertColumnsSize = insertColumns.size();
            for (int paramIdx = 0; paramIdx < insertColumnsSize; paramIdx++) {
                String sqlColumnName = insertColumns.get(paramIdx);
                if (containPK(sqlColumnName)) {
                    pkIndexMap.put(getStandardPkColumnName(sqlColumnName), paramIdx);
                }
            }
            return pkIndexMap;
        }
        int pkIndex = -1;
        Map<String, ColumnMeta> allColumns = getTableMeta().getAllColumns();
        for (Map.Entry<String, ColumnMeta> entry : allColumns.entrySet()) {
            pkIndex++;
            if (containPK(entry.getValue().getColumnName())) {
                pkIndexMap.put(ColumnUtils.delEscape(entry.getValue().getColumnName(), getDbType()), pkIndex);
            }
        }
        return pkIndexMap;
    }


    /**
     * parse primary key value from statement.
     * @return
     */
    protected Map<String, List<Object>> parsePkValuesFromStatement() {
        // insert values including PK
        final Map<String, Integer> pkIndexMap = getPkIndex();
        if (pkIndexMap.isEmpty()) {
            throw new ShouldNeverHappenException("pkIndex is not found");
        }
        Map<String, List<Object>> pkValuesMap = new HashMap<>();
        boolean ps = true;
        if (statementProxy instanceof PreparedStatementProxy) {
            PreparedStatementProxy preparedStatementProxy = (PreparedStatementProxy) statementProxy;
            List<List<Object>> insertRows = getInsertRows(pkIndexMap.values());
            if (insertRows != null && !insertRows.isEmpty()) {
                Map<Integer, ArrayList<Object>> parameters = preparedStatementProxy.getParameters();
                final int rowSize = insertRows.size();
                int totalPlaceholderNum = -1;
                for (List<Object> row : insertRows) {
                    // oracle insert sql statement specify RETURN_GENERATED_KEYS will append :rowid on sql end
                    // insert parameter count will than the actual +1
                    if (row.isEmpty()) {
                        continue;
                    }
                    int currentRowPlaceholderNum = -1;
                    for (Object r : row) {
                        if (PLACEHOLDER.equals(r)) {
                            totalPlaceholderNum += 1;
                            currentRowPlaceholderNum += 1;
                        }
                    }
                    String pkKey;
                    int pkIndex;
                    List<Object> pkValues;
                    for (Map.Entry<String, Integer> entry : pkIndexMap.entrySet()) {
                        pkKey = entry.getKey();
                        pkValues = pkValuesMap.get(pkKey);
                        if (Objects.isNull(pkValues)) {
                            pkValues = new ArrayList<>(rowSize);
                        }
                        pkIndex = entry.getValue();
                        Object pkValue = row.get(pkIndex);
                        if (PLACEHOLDER.equals(pkValue)) {
                            int currentRowNotPlaceholderNumBeforePkIndex = 0;
                            for (int n = 0, len = row.size(); n < len; n++) {
                                Object r = row.get(n);
                                if (n < pkIndex && !PLACEHOLDER.equals(r)) {
                                    currentRowNotPlaceholderNumBeforePkIndex++;
                                }
                            }
                            int idx = totalPlaceholderNum - currentRowPlaceholderNum + pkIndex - currentRowNotPlaceholderNumBeforePkIndex;
                            ArrayList<Object> parameter = parameters.get(idx + 1);
                            pkValues.addAll(parameter);
                        } else {
                            pkValues.add(pkValue);
                        }
                        if (!pkValuesMap.containsKey(ColumnUtils.delEscape(pkKey, getDbType()))) {
                            pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), pkValues);
                        }
                    }
                }
            }
        } else {
            ps = false;
            List<List<Object>> insertRows = getInsertRows(pkIndexMap.values());
            for (List<Object> row : insertRows) {
                pkIndexMap.forEach((pkKey, pkIndex) -> {
                    List<Object> pkValues = pkValuesMap.get(pkKey);
                    if (Objects.isNull(pkValues)) {
                        pkValuesMap.put(ColumnUtils.delEscape(pkKey, getDbType()), Lists.newArrayList(row.get(pkIndex)));
                    } else {
                        pkValues.add(row.get(pkIndex));
                    }
                });
            }
        }
        if (pkValuesMap.isEmpty()) {
            throw new ShouldNeverHappenException("pkValuesMap is empty");
        }
        boolean b = this.checkPkValues(pkValuesMap, ps);
        if (!b) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        return pkValuesMap;
    }

    /**
     * user for insert select
     *
     * @param primaryKeyIndex the primary key index
     * @return the insert rows
     */
    protected List<List<Object>> getInsertRows(Collection<Integer> primaryKeyIndex) {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        return recognizer.getInsertRows(primaryKeyIndex);
    }

    /**
     * default get generated keys.
     * @return
     * @throws SQLException
     */
    @Deprecated
    public List<Object> getGeneratedKeys() throws SQLException {
        // PK is just auto generated
        ResultSet genKeys = statementProxy.getGeneratedKeys();
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = genKeys.getObject(1);
            pkValues.add(v);
        }
        if (pkValues.isEmpty()) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            LOGGER.warn("Fail to reset ResultSet cursor. can not get primary key value");
        }
        return pkValues;
    }

    /**
     * default get generated keys.
     * @param pkKey the pk key
     * @return
     * @throws SQLException
     */
    public List<Object> getGeneratedKeys(String pkKey) throws SQLException {
        // PK is just auto generated
        ResultSet genKeys = statementProxy.getGeneratedKeys();
        List<Object> pkValues = new ArrayList<>();
        while (genKeys.next()) {
            Object v = StringUtils.isEmpty(pkKey) ? genKeys.getObject(1) : genKeys.getObject(pkKey);
            pkValues.add(v);
        }
        if (pkValues.isEmpty()) {
            throw new NotSupportYetException(String.format("not support sql [%s]", sqlRecognizer.getOriginalSQL()));
        }
        try {
            genKeys.beforeFirst();
        } catch (SQLException e) {
            LOGGER.warn("Fail to reset ResultSet cursor. can not get primary key value");
        }
        return pkValues;
    }

    /**
     * the modify for test
     *
     * @param expr the expr
     * @return the pk values by sequence
     * @throws SQLException the sql exception
     */
    @Deprecated
    protected List<Object> getPkValuesBySequence(SqlSequenceExpr expr) throws SQLException {
        List<Object> pkValues = null;
        try {
            pkValues = getGeneratedKeys();
        } catch (NotSupportYetException | SQLException ignore) {
        }

        if (!CollectionUtils.isEmpty(pkValues)) {
            return pkValues;
        }

        Sequenceable sequenceable = (Sequenceable) this;
        final String sql = sequenceable.getSequenceSql(expr);
        LOGGER.warn("Fail to get auto-generated keys, use '{}' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);

        Connection conn = statementProxy.getConnection();
        try (Statement ps = conn.createStatement();
             ResultSet genKeys = ps.executeQuery(sql)) {

            pkValues = new ArrayList<>();
            while (genKeys.next()) {
                Object v = genKeys.getObject(1);
                pkValues.add(v);
            }
            return pkValues;
        }
    }

    /**
     * the modify for test
     *
     * @param expr  the expr
     * @param pkKey the pk key
     * @return the pk values by sequence
     * @throws SQLException the sql exception
     */
    protected List<Object> getPkValuesBySequence(SqlSequenceExpr expr, String pkKey) throws SQLException {
        List<Object> pkValues = null;
        try {
            pkValues = getGeneratedKeys(pkKey);
        } catch (NotSupportYetException | SQLException ignore) {
        }

        if (!CollectionUtils.isEmpty(pkValues)) {
            return pkValues;
        }

        Sequenceable sequenceable = (Sequenceable) this;
        final String sql = sequenceable.getSequenceSql(expr);
        LOGGER.warn("Fail to get auto-generated keys, use '{}' instead. Be cautious, statement could be polluted. Recommend you set the statement to return generated keys.", sql);

        Connection conn = statementProxy.getConnection();
        try (Statement ps = conn.createStatement();
             ResultSet genKeys = ps.executeQuery(sql)) {

            pkValues = new ArrayList<>();
            while (genKeys.next()) {
                Object v = genKeys.getObject(1);
                pkValues.add(v);
            }
            return pkValues;
        }
    }

    /**
     * check pk values for multi Pk
     * At most one null per row.
     * Method is not allowed.
     *
     * @param pkValues the pk values
     * @return boolean
     */
    protected boolean checkPkValuesForMultiPk(Map<String, List<Object>> pkValues) {
        Set<String> pkNames = pkValues.keySet();
        if (pkNames.isEmpty()) {
            throw new ShouldNeverHappenException("pkNames is empty");
        }
        int rowSize = pkValues.get(pkNames.iterator().next()).size();
        for (int i = 0; i < rowSize; i++) {
            int n = 0;
            int m = 0;
            for (String name : pkNames) {
                Object pkValue = pkValues.get(name).get(i);
                if (pkValue instanceof Null) {
                    n++;
                }
                if (pkValue instanceof SqlMethodExpr) {
                    m++;
                }
            }
            if (n > 1) {
                return false;
            }
            if (m > 0) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check pk values boolean.
     *
     * @param pkValues the pk values
     * @param ps       the ps
     * @return the boolean
     */
    protected boolean checkPkValues(Map<String, List<Object>> pkValues, boolean ps) {
        Set<String> pkNames = pkValues.keySet();
        if (pkNames.size() == 1) {
            return checkPkValuesForSinglePk(pkValues.get(pkNames.iterator().next()), ps);
        } else {
            return checkPkValuesForMultiPk(pkValues);
        }
    }

    /**
     * check pk values for single pk
     * @param pkValues pkValues
     * @param ps       true: is prepared statement. false: normal statement.
     * @return true: support. false: not support.
     */
    @SuppressWarnings("lgtm[java/constant-comparison]")
    protected boolean checkPkValuesForSinglePk(List<Object> pkValues, boolean ps) {
        /*
        ps = true
        -----------------------------------------------
                  one    more
        null       O      O
        value      O      O
        method     O      O
        sequence   O      O
        default    O      O
        -----------------------------------------------
        ps = false
        -----------------------------------------------
                  one    more
        null       O      X
        value      O      O
        method     X      X
        sequence   O      X
        default    O      X
        -----------------------------------------------
        */
        int n = 0, v = 0, m = 0, s = 0, d = 0;
        for (Object pkValue : pkValues) {
            if (pkValue instanceof Null) {
                n++;
                continue;
            }
            if (pkValue instanceof SqlMethodExpr) {
                m++;
                continue;
            }
            if (pkValue instanceof SqlSequenceExpr) {
                s++;
                continue;
            }
            if (pkValue instanceof SqlDefaultExpr) {
                d++;
                continue;
            }
            v++;
        }

        if (!ps) {
            if (m > 0) {
                return false;
            }
            if (n == 1 && v == 0 && m == 0 && s == 0 && d == 0) {
                return true;
            }
            if (n == 0 && v > 0 && m == 0 && s == 0 && d == 0) {
                return true;
            }
            if (n == 0 && v == 0 && m == 0 && s == 1 && d == 0) {
                return true;
            }
            if (n == 0 && v == 0 && m == 0 && s == 0 && d == 1) {
                return true;
            }
            return false;
        }

        if (n > 0 && v == 0 && m == 0 && s == 0 && d == 0) {
            return true;
        }
        if (n == 0 && v > 0 && m == 0 && s == 0 && d == 0) {
            return true;
        }
        if (n == 0 && v == 0 && m > 0 && s == 0 && d == 0) {
            return true;
        }
        if (n == 0 && v == 0 && m == 0 && s > 0 && d == 0) {
            return true;
        }
        if (n == 0 && v == 0 && m == 0 && s == 0 && d > 0) {
            return true;
        }
        return false;
    }

    /**
     * build image sql and the param to fill placeholder
     *
     * @param tableMeta table meta
     * @return image sql
     */
    public String buildImageSQL(TableMeta tableMeta) {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer) sqlRecognizer;
        int insertNum = getInsertParamsValue().size();
        Map<String, ArrayList<Object>> imageParamperterMap = buildImageParamperters(recognizer);
        if (Objects.isNull(paramAppenderMap)) {
            paramAppenderMap = new HashMap<>();
        }
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
                        if (imageParamperterMap.get(columnName) == null) {
                            if (m.getColumnDef() != null) {
                                columnList.add(columnName);
                                columnValue.add("DEFAULT(" + columnName + ")");
                                continue;
                            } else {
                                break;
                            }
                        }
                        if (imageParamperterMap.get(columnName).get(finalI) == null) {
                            break;
                        }
                        if (imageParamperterMap.get(columnName).get(finalI) instanceof Null) {
                            if (!IndexType.PRIMARY.equals(v.getIndextype())) {
                                columnList.add(columnName);
                                columnValue.add("NULL");
                                continue;
                            }
                            // break for the situation of composite primary key
                            break;
                        }
                        columnList.add(columnName);
                        columnValue.add(imageParamperterMap.get(columnName).get(finalI));
                    }
                    if (CollectionUtils.isNotEmpty(columnList)) {
                        CollectionUtils.computeIfAbsent(paramAppenderMap, columnList, e -> new ArrayList<>())
                                .addAll(columnValue);
                    }
                }
            });
        }
        suffix.append(" WHERE ");
        paramAppenderMap.forEach((columnsName, columnsValue) -> {
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
     * get image param from the sql
     *
     * @param recognizer recognizer
     * @return key: columnName value: what the user insert
     */
    protected Map<String, ArrayList<Object>> buildImageParamperters(SQLInsertRecognizer recognizer) {
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        //  VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)   VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
        List<String> insertParamsList = getInsertParamsValue();
        List<String> insertColumns = Optional.ofNullable(recognizer.getInsertColumns()).map(list -> list.stream()
                .map(column -> ColumnUtils.delEscape(column, getDbType())).collect(Collectors.toList())).orElse(null);
        if (CollectionUtils.isEmpty(insertColumns)) {
            insertColumns = getTableMeta(recognizer.getTableName()).getDefaultTableColumn();
        }
        Map<String, ArrayList<Object>> imageParamperterMap = new HashMap<>(insertColumns.size(), 1);
        int paramIndex = 1;
        for (String insertParams : insertParamsList) {
            String[] insertParamsArray = insertParams.split(",");
            for (int i = 0; i < insertColumns.size(); i++) {
                String m = ColumnUtils.delEscape(insertColumns.get(i), getDbType());
                String params = insertParamsArray[i];
                ArrayList<Object> imageListTemp = imageParamperterMap.computeIfAbsent(m, k -> new ArrayList<>());
                if ("?".equals(params.trim())) {
                    ArrayList<Object> objects = parameters.get(paramIndex);
                    imageListTemp.addAll(objects);
                    paramIndex++;
                } else if (params instanceof String) {
                    // params is characterstring constant
                    if (params.trim().startsWith("'") && params.trim().endsWith("'") || params.trim().startsWith("\"") && params.trim().endsWith("\"")) {
                        params = params.trim();
                        params = params.substring(1, params.length() - 1);
                    }
                    imageListTemp.add(params);
                } else {
                    imageListTemp.add(params);
                }
                imageParamperterMap.put(m, imageListTemp);
            }
        }
        return imageParamperterMap;
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

    /**
     * create the real insert recognizer
     *
     * @param querySQL the sql after insert
     * @throws SQLException
     */
    protected SQLInsertRecognizer doCreateInsertRecognizer(String querySQL) throws SQLException {
        Map<Integer, ArrayList<Object>> parameters = ((PreparedStatementProxy) statementProxy).getParameters();
        List<SQLRecognizer> sqlRecognizers = SQLVisitorFactory.get(querySQL + FOR_UPDATE, getDbType());
        SQLRecognizer selectRecognizer = sqlRecognizers.get(0);
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        TableMeta selectTableMeta = TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
                .getTableMeta(connectionProxy.getTargetConnection(), selectRecognizer.getTableName(), connectionProxy.getDataSourceProxy().getResourceId());
        // use query SQL to get values from database
        TableRecords tableRecords = buildTableRecords2(selectTableMeta, querySQL, new ArrayList<>(parameters.values()));
        if (CollectionUtils.isNotEmpty(tableRecords.getRows())) {
            StringBuilder valuesSQL = new StringBuilder();
            // build values sql
            valuesSQL.append(" VALUES");
            tableRecords.getRows().forEach(row -> {
                List<Object> values = row.getFields().stream().map(Field::getValue)
                        .map(value -> Objects.isNull(value) ? null : value).collect(Collectors.toList());
                valuesSQL.append("(");
                for (Object value : values) {
                    if (Objects.isNull(value)) {
                        valuesSQL.append((String) null);
                    } else {
                        valuesSQL.append(value);
                    }
                    valuesSQL.append(",");
                }
                valuesSQL.insert(valuesSQL.length() - 1, ")");
            });
            valuesSQL.deleteCharAt(valuesSQL.length() - 1);
            return (SQLInsertRecognizer) SQLVisitorFactory.get(formatOriginSQL(valuesSQL.toString()), getDbType()).get(0);
        }
        return null;
    }

    /**
     * format origin sql
     *
     * @param valueSQL the value after insert sql
     * @return eg: insert into test values(1,1)
     */
    private String formatOriginSQL(String valueSQL) {
        String tableName = this.sqlRecognizer.getTableName().toUpperCase();
        String originalSQL = this.sqlRecognizer.getOriginalSQL().toUpperCase();
        int index = originalSQL.indexOf(SELECT);
        if (tableName.equalsIgnoreCase(SELECT)) {
            // choose the next select
            index = originalSQL.indexOf(SELECT, index + SELECT.length());
        }
        if (index == -1) {
            throw new ShouldNeverHappenException("may be the query sql is not a select SQL");
        }
        return this.sqlRecognizer.getOriginalSQL().substring(0, index) + valueSQL;
    }

}
