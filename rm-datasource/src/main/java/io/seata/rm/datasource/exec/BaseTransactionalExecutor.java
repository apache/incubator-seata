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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.seata.common.DefaultValues;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.CollectionUtils;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLInsertRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLType;
import io.seata.sqlparser.WhereRecognizer;


import static io.seata.rm.datasource.exec.AbstractDMLBaseExecutor.WHERE;

/**
 * The type Base transactional executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author sharajava
 */
public abstract class BaseTransactionalExecutor<T, S extends Statement> implements Executor<T> {

    private static final boolean ONLY_CARE_UPDATE_COLUMNS = ConfigurationFactory.getInstance().getBoolean(
            ConfigurationKeys.TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS, DefaultValues.DEFAULT_ONLY_CARE_UPDATE_COLUMNS);

    /**
     * The Statement proxy.
     */
    protected StatementProxy<S> statementProxy;

    /**
     * The Statement callback.
     */
    protected StatementCallback<T, S> statementCallback;

    /**
     * The Sql recognizer.
     */
    protected SQLRecognizer sqlRecognizer;

    /**
     * The Sql recognizer.
     */
    protected List<SQLRecognizer> sqlRecognizers;

    private TableMeta tableMeta;

    /**
     * Instantiates a new Base transactional executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public BaseTransactionalExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
        SQLRecognizer sqlRecognizer) {
        this.statementProxy = statementProxy;
        this.statementCallback = statementCallback;
        this.sqlRecognizer = sqlRecognizer;
    }

    /**
     * Instantiates a new Base transactional executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the multi sql recognizer
     */
    public BaseTransactionalExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
        List<SQLRecognizer> sqlRecognizers) {
        this.statementProxy = statementProxy;
        this.statementCallback = statementCallback;
        this.sqlRecognizers = sqlRecognizers;
    }

    @Override
    public T execute(Object... args) throws Throwable {
        String xid = RootContext.getXID();
        if (xid != null) {
            statementProxy.getConnectionProxy().bind(xid);
        }

        statementProxy.getConnectionProxy().setGlobalLockRequire(RootContext.requireGlobalLock());
        return doExecute(args);
    }

    /**
     * Do execute object.
     *
     * @param args the args
     * @return the object
     * @throws Throwable the throwable
     */
    protected abstract T doExecute(Object... args) throws Throwable;


    /**
     * build buildWhereCondition
     *
     * @param recognizer        the recognizer
     * @param paramAppenderList the param paramAppender list
     * @return the string
     */
    protected String buildWhereCondition(WhereRecognizer recognizer, ArrayList<List<Object>> paramAppenderList) {
        String whereCondition = null;
        if (statementProxy instanceof ParametersHolder) {
            whereCondition = recognizer.getWhereCondition((ParametersHolder) statementProxy, paramAppenderList);
        } else {
            whereCondition = recognizer.getWhereCondition();
        }
        //process batch operation
        if (StringUtils.isNotBlank(whereCondition) && CollectionUtils.isNotEmpty(paramAppenderList) && paramAppenderList.size() > 1) {
            StringBuilder whereConditionSb = new StringBuilder();
            whereConditionSb.append(" ( ").append(whereCondition).append(" ) ");
            for (int i = 1; i < paramAppenderList.size(); i++) {
                whereConditionSb.append(" or ( ").append(whereCondition).append(" ) ");
            }
            whereCondition = whereConditionSb.toString();
        }
        return whereCondition;
    }

    /**
     * build buildOrderCondition
     * @param recognizer
     * @param paramAppenderList
     * @return the string
     */
    protected String buildOrderCondition(WhereRecognizer recognizer, ArrayList<List<Object>> paramAppenderList) {
        String orderByCondition = null;
        if (statementProxy instanceof ParametersHolder) {
            orderByCondition = recognizer.getOrderByCondition((ParametersHolder) statementProxy, paramAppenderList);
        } else {
            orderByCondition = recognizer.getOrderByCondition();
        }
        return orderByCondition;
    }

    /**
     * build buildLimitCondition
     * @param recognizer
     * @param paramAppenderList
     * @return the string
     */
    protected String buildLimitCondition(WhereRecognizer recognizer, ArrayList<List<Object>> paramAppenderList) {
        String limitCondition = null;
        if (statementProxy instanceof ParametersHolder) {
            limitCondition = recognizer.getLimitCondition((ParametersHolder) statementProxy, paramAppenderList);
        } else {
            limitCondition = recognizer.getLimitCondition();
        }
        return limitCondition;
    }

    /**
     * Gets column name with table prefix
     *
     * @param table      the table name
     * @param tableAlias the tableAlias
     * @param columnName the column name
     * @return
     */
    protected String getColumnNameWithTablePrefix(String table, String tableAlias, String columnName) {
        return tableAlias == null ? (table == null ? columnName : table + "." + columnName) : (tableAlias + "." + columnName);
    }

    /**
     * Gets column name with table prefix
     *
     * @param table      the table name
     * @param tableAlias the tableAlias
     * @param columnNames the column names
     * @return
     */
    protected List<String> getColumnNamesWithTablePrefixList(String table,String tableAlias,List<String> columnNames) {
        List<String> columnNameWithTablePrefix = new ArrayList<>();
        for (String columnName : columnNames) {
            columnNameWithTablePrefix.add(this.getColumnNameWithTablePrefix(table,tableAlias,columnName));
        }
        return columnNameWithTablePrefix;
    }

    /**
     * Gets several column name in sql.
     *
     * @param table          the table
     * @param tableAlias     the table alias
     * @param columnNameList the column name
     * @return the column name in sql
     */
    protected String getColumnNamesWithTablePrefix(String table,String tableAlias, List<String> columnNameList) {
        if (CollectionUtils.isEmpty(columnNameList)) {
            return null;
        }
        StringBuilder columnNamesStr = new StringBuilder();
        for (int i = 0; i < columnNameList.size(); i++) {
            if (i > 0) {
                columnNamesStr.append(" , ");
            }
            columnNamesStr.append(getColumnNameWithTablePrefix(table, tableAlias, columnNameList.get(i)));
        }
        return columnNamesStr.toString();
    }

    /**
     * Gets column name in sql.
     *
     * @param columnName the column name
     * @return the column name in sql
     */
    protected String getColumnNameInSQL(String columnName) {
        String tableAlias = sqlRecognizer.getTableAlias();
        return tableAlias == null ? columnName : tableAlias + "." + columnName;
    }

    /**
     * Gets column names in sql.
     *
     * @param columnNames the column names
     * @return
     */
    protected List<String> getColumnNamesInSQLList(List<String> columnNames) {
        List<String> columnNameWithTableAlias = new ArrayList<>();
        for (String columnName : columnNames) {
            columnNameWithTableAlias.add(this.getColumnNameInSQL(columnName));
        }
        return columnNameWithTableAlias;
    }

    /**
     * Gets several column name in sql.
     *
     * @param columnNameList the column name
     * @return the column name in sql
     */
    protected String getColumnNamesInSQL(List<String> columnNameList) {
        if (CollectionUtils.isEmpty(columnNameList)) {
            return null;
        }
        StringBuilder columnNamesStr = new StringBuilder();
        for (int i = 0; i < columnNameList.size(); i++) {
            if (i > 0) {
                columnNamesStr.append(" , ");
            }
            columnNamesStr.append(getColumnNameInSQL(columnNameList.get(i)));
        }
        return columnNamesStr.toString();
    }

    /**
     * Gets from table in sql.
     *
     * @return the from table in sql
     */
    protected String getFromTableInSQL() {
        String tableName = sqlRecognizer.getTableName();
        String tableAlias = sqlRecognizer.getTableAlias();
        return tableAlias == null ? tableName : tableName + " " + tableAlias;
    }

    /**
     * Gets table meta.
     *
     * @return the table meta
     */
    protected TableMeta getTableMeta() {
        return getTableMeta(sqlRecognizer.getTableName());
    }

    /**
     * Gets table meta.
     *
     * @param tableName the table name
     * @return the table meta
     */
    protected TableMeta getTableMeta(String tableName) {
        if (tableMeta != null) {
            return tableMeta;
        }
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        tableMeta = TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
            .getTableMeta(connectionProxy.getTargetConnection(), tableName, connectionProxy.getDataSourceProxy().getResourceId());
        return tableMeta;
    }

    /**
     * the columns contains the targetColumn
     * @param columns the column name list
     * @param targetColumn target column
     * @return true: contains targetColumn false: not contains targetColumn
     */
    protected boolean containsColumn(List<String> columns, String targetColumn) {
        if (CollectionUtils.isEmpty(columns)) {
            return false;
        }
        return CollectionUtils.toUpperList(columns).contains(targetColumn.toUpperCase());
    }

    /**
     * the columns contains table meta pk
     *
     * @param columns the column name list
     * @return true: contains pk false: not contains pk
     */
    protected boolean containsPK(List<String> columns) {
        if (CollectionUtils.isEmpty(columns)) {
            return false;
        }
        List<String> newColumns = ColumnUtils.delEscape(columns, getDbType());
        return getTableMeta().containsPK(newColumns);
    }

    /**
     * the columns contains table meta pk
     *
     * @param tableName the tableName
     * @param columns the column name list
     * @return true: contains pk false: not contains pk
     */
    protected boolean containsPK(String tableName,List<String> columns) {
        if (CollectionUtils.isEmpty(columns)) {
            return false;
        }
        List<String> newColumns = ColumnUtils.delEscape(columns, getDbType());
        return getTableMeta(tableName).containsPK(newColumns);
    }


    /**
     * compare column name and primary key name
     *
     * @param columnName the primary key column name
     * @return true: contain false: not contain
     */
    protected boolean containPK(String columnName) {
        String newColumnName = ColumnUtils.delEscape(columnName, getDbType());
        return CollectionUtils.toUpperList(getTableMeta().getPrimaryKeyOnlyName()).contains(newColumnName.toUpperCase());
    }

    /**
     * get standard pk column name from user sql column name
     *
     * @param userColumnName the user column name
     * @return standard pk column name
     */
    protected String getStandardPkColumnName(String userColumnName) {
        String newUserColumnName = ColumnUtils.delEscape(userColumnName, getDbType());
        for (String cn : getTableMeta().getPrimaryKeyOnlyName()) {
            if (cn.equalsIgnoreCase(newUserColumnName)) {
                return cn;
            }
        }
        return null;
    }

    /**
     * prepare undo log.
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @throws SQLException the sql exception
     */
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if (beforeImage.getRows().isEmpty() && afterImage.getRows().isEmpty()) {
            return;
        }
        if (SQLType.UPDATE == sqlRecognizer.getSQLType()) {
            if (beforeImage.getRows().size() != afterImage.getRows().size()) {
                throw new ShouldNeverHappenException("Before image size is not equaled to after image size, probably because you updated the primary keys.");
            }
        }
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();

        TableRecords lockKeyRecords = sqlRecognizer.getSQLType() == SQLType.DELETE ? beforeImage : afterImage;
        String lockKeys = buildLockKey(lockKeyRecords);
        if (null != lockKeys) {
            connectionProxy.appendLockKey(lockKeys);

            SQLUndoLog sqlUndoLog = buildUndoItem(beforeImage, afterImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
        }
    }

    /**
     * build lockKey
     *
     * @param rowsIncludingPK the records
     * @return the string as local key. the local key example(multi pk): "t_user:1_a,2_b"
     */
    protected String buildLockKey(TableRecords rowsIncludingPK) {
        if (rowsIncludingPK.size() == 0) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(rowsIncludingPK.getTableMeta().getTableName());
        sb.append(":");
        int rowSequence = 0;
        List<Map<String, Field>> pksRows = rowsIncludingPK.pkRows();
        List<String> primaryKeysOnlyName = rowsIncludingPK.getTableMeta().getPrimaryKeyOnlyName();
        for (Map<String, Field> rowMap : pksRows) {
            int pkSplitIndex = 0;
            for (String pkName : primaryKeysOnlyName) {
                if (pkSplitIndex > 0) {
                    sb.append("_");
                }
                sb.append(rowMap.get(pkName).getValue());
                pkSplitIndex++;
            }
            rowSequence++;
            if (rowSequence < pksRows.size()) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @return sql undo log
     */
    protected SQLUndoLog buildUndoItem(TableRecords beforeImage, TableRecords afterImage) {
        SQLType sqlType = sqlRecognizer.getSQLType();
        String tableName = sqlRecognizer.getTableName();

        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }

    /**
     * build a BeforeImage
     *
     * @param tableMeta         the tableMeta
     * @param selectSQL         the selectSQL
     * @param paramAppenderList the paramAppender list
     * @return a tableRecords
     * @throws SQLException the sql exception
     */
    protected TableRecords buildTableRecords(TableMeta tableMeta, String selectSQL, ArrayList<List<Object>> paramAppenderList) throws SQLException {
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(selectSQL)) {
            if (CollectionUtils.isNotEmpty(paramAppenderList)) {
                for (int i = 0, ts = paramAppenderList.size(); i < ts; i++) {
                    List<Object> paramAppender = paramAppenderList.get(i);
                    for (int j = 0, ds = paramAppender.size(); j < ds; j++) {
                        ps.setObject(i * ds + j + 1, paramAppender.get(j));
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
     * build TableRecords
     *
     * @param pkValuesMap the pkValuesMap
     * @return return TableRecords;
     * @throws SQLException the sql exception
     */
    protected TableRecords buildTableRecords(Map<String, List<Object>> pkValuesMap) throws SQLException {
        SQLInsertRecognizer recognizer = (SQLInsertRecognizer)sqlRecognizer;
        List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
        String prefix = "SELECT ";
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        // build check sql
        String firstKey = pkValuesMap.keySet().stream().findFirst().get();
        int rowSize = pkValuesMap.get(firstKey).size();
        suffix.append(WHERE).append(SqlGenerateUtils.buildWhereConditionByPKs(pkColumnNameList, rowSize, getDbType()));
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix, suffix.toString());
        List<String> insertColumnsUnEscape = recognizer.getInsertColumnsUnEscape();
        List<String> needColumns =
            getNeedColumns(tableMeta.getTableName(), sqlRecognizer.getTableAlias(), insertColumnsUnEscape);
        needColumns.forEach(selectSQLJoin::add);
        ResultSet rs = null;
        try (PreparedStatement ps = statementProxy.getConnection().prepareStatement(selectSQLJoin.toString())) {

            int paramIndex = 1;
            for (int r = 0; r < rowSize; r++) {
                for (int c = 0; c < pkColumnNameList.size(); c++) {
                    List<Object> pkColumnValueList = pkValuesMap.get(pkColumnNameList.get(c));
                    int dataType = tableMeta.getColumnMeta(pkColumnNameList.get(c)).getDataType();
                    ps.setObject(paramIndex, pkColumnValueList.get(r), dataType);
                    paramIndex++;
                }
            }
            rs = ps.executeQuery();
            return TableRecords.buildRecords(getTableMeta(), rs);
        } finally {
            IOUtil.close(rs);
        }
    }

    protected List<String> getNeedColumns(String table, String tableAlias, List<String> unescapeColumns) {
        List<String> needUpdateColumns = new ArrayList<>();
        TableMeta tableMeta = getTableMeta(table);
        if (ONLY_CARE_UPDATE_COLUMNS && CollectionUtils.isNotEmpty(unescapeColumns)) {
            if (!containsPK(table, unescapeColumns)) {
                List<String> pkNameList = tableMeta.getEscapePkNameList(getDbType());
                if (CollectionUtils.isNotEmpty(pkNameList)) {
                    if (StringUtils.isNotBlank(tableAlias)) {
                        needUpdateColumns.addAll(
                                ColumnUtils.delEscape(getColumnNamesWithTablePrefixList(table, tableAlias, pkNameList), getDbType())
                        );
                    } else {
                        needUpdateColumns.addAll(
                                ColumnUtils.delEscape(getColumnNamesInSQLList(pkNameList), getDbType())
                        );
                    }
                }
            }

            needUpdateColumns.addAll(unescapeColumns.stream().filter(unescapeColumn -> !containsColumn(needUpdateColumns, unescapeColumn)).collect(Collectors.toList()));

            // The on update xxx columns will be auto update by db, so it's also the actually updated columns
            List<String> onUpdateColumns = tableMeta.getOnUpdateColumnsOnlyName();
            if (StringUtils.isNotBlank(tableAlias)) {
                onUpdateColumns = onUpdateColumns.stream()
                        .map(onUpdateColumn -> getColumnNameWithTablePrefix(table, tableAlias, onUpdateColumn))
                        .collect(Collectors.toList());
            }
            onUpdateColumns.removeAll(unescapeColumns);
            needUpdateColumns.addAll(onUpdateColumns.stream()
                .map(onUpdateColumn -> ColumnUtils.addEscape(onUpdateColumn, getDbType(), tableMeta))
                .collect(Collectors.toList()));
        } else {
            Stream<String> allColumns = tableMeta.getAllColumns().keySet().stream();
            if (StringUtils.isNotBlank(tableAlias)) {
                allColumns = allColumns.map(columnName -> getColumnNameWithTablePrefix(table, tableAlias, columnName));
            }
            allColumns = allColumns.map(columnName -> ColumnUtils.addEscape(columnName, getDbType(), tableMeta));
            allColumns.forEach(needUpdateColumns::add);
        }
        return needUpdateColumns;
    }

    /**
     * get db type
     *
     * @return db type
     */
    protected String getDbType() {
        return statementProxy.getConnectionProxy().getDbType();
    }

}
