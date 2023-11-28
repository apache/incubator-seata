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
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

import io.seata.common.util.CollectionUtils;
import io.seata.core.protocol.Version;
import io.seata.rm.datasource.ConnectionProxy;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.TableMetaCacheFactory;
import io.seata.rm.datasource.undo.SQLUndoLog;
import io.seata.sqlparser.ParametersHolder;
import io.seata.sqlparser.SQLType;
import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.UpdateExecutor;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLUpdateRecognizer;
import io.seata.sqlparser.util.ColumnUtils;
import io.seata.sqlparser.JoinRecognizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author renliangyu857
 */
public class MySQLUpdateJoinExecutor<T, S extends Statement> extends UpdateExecutor<T, S> {
    protected final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String DOT = ".";
    private final Map<String, TableRecords> beforeImagesMap = new LinkedHashMap<>(4);
    private final Map<String, TableRecords> afterImagesMap = new LinkedHashMap<>(4);
    protected boolean isLowerSupportGroupByPksVersion;
    private String sqlMode = "";

    /**
     * Instantiates a new Update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public MySQLUpdateJoinExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
        SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
        this.isLowerSupportGroupByPksVersion = Version.convertVersionNotThrowException(getDbVersion()) < Version.convertVersionNotThrowException("5.7.5");
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        String tableNames = recognizer.getTableName();
        // update join sql,like update t1 inner join t2 on t1.id = t2.id set t1.name = ?; tableItems = {"update t1 inner join t2","t1","t2"}
        String[] tableItems = tableNames.split(SQLUpdateRecognizer.MULTI_TABLE_NAME_SEPERATOR);
        String joinTable = tableItems[0];
        final int itemTableIndex = 1;
        String suffixCommonCondition = buildBeforeImageSQLCommonConditionSuffix(paramAppenderList);
        for (int i = itemTableIndex; i < tableItems.length; i++) {
            List<String> itemTableUpdateColumns = getItemUpdateColumns(this.getTableMeta(tableItems[i]), recognizer.getUpdateColumns());
            if (CollectionUtils.isEmpty(itemTableUpdateColumns)) {
                continue;
            }
            String selectSQL = buildBeforeImageSQL(joinTable, tableItems[i], suffixCommonCondition, itemTableUpdateColumns);
            TableRecords tableRecords = buildTableRecords(getTableMeta(tableItems[i]), selectSQL, paramAppenderList);
            beforeImagesMap.put(tableItems[i], tableRecords);
        }
        return null;
    }

    private String buildBeforeImageSQLCommonConditionSuffix(ArrayList<List<Object>> paramAppenderList) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        StringBuilder suffix = new StringBuilder();
        buildJoinCondition(recognizer,paramAppenderList);
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        String orderByCondition = buildOrderCondition(recognizer, paramAppenderList);
        String limitCondition = buildLimitCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            suffix.append(WHERE).append(whereCondition);
        }
        if (StringUtils.isNotBlank(orderByCondition)) {
            suffix.append(" ").append(orderByCondition);
        }
        if (StringUtils.isNotBlank(limitCondition)) {
            suffix.append(" ").append(limitCondition);
        }
        return suffix.toString();
    }

    private void buildJoinCondition(SQLUpdateRecognizer recognizer, ArrayList<List<Object>> paramAppenderList) {
        if (statementProxy instanceof ParametersHolder) {
            ((JoinRecognizer)recognizer).getJoinCondition((ParametersHolder) statementProxy,paramAppenderList);
        }
    }

    private String buildBeforeImageSQL(String joinTable, String itemTable,String suffixCondition, List<String> itemTableUpdateColumns) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        TableMeta itemTableMeta = getTableMeta(itemTable);
        StringBuilder prefix = new StringBuilder("SELECT ");
        StringBuilder suffix = new StringBuilder(" FROM ").append(joinTable);
        suffix.append(suffixCondition);
        //maybe duplicate row for select join sql.remove duplicate row by 'group by' condition
        suffix.append(GROUP_BY);
        List<String> pkColumnNames = getColumnNamesWithTablePrefixList(itemTable, recognizer.getTableAlias(itemTable), itemTableMeta.getPrimaryKeyOnlyName());
        List<String> needUpdateColumns = getNeedColumns(itemTable, recognizer.getTableAlias(itemTable), itemTableUpdateColumns);
        suffix.append(buildGroupBy(pkColumnNames,needUpdateColumns));
        suffix.append(" FOR UPDATE");
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix.toString(), suffix.toString());
        needUpdateColumns.forEach(selectSQLJoin::add);
        return selectSQLJoin.toString();
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        String tableNames = recognizer.getTableName();
        String[] tableItems = tableNames.split(SQLUpdateRecognizer.MULTI_TABLE_NAME_SEPERATOR);
        String joinTable = tableItems[0];
        final int itemTableIndex = 1;
        ArrayList<List<Object>> joinConditionParams = new ArrayList<>();
        buildJoinCondition(recognizer,joinConditionParams);
        for (int i = itemTableIndex; i < tableItems.length; i++) {
            TableRecords tableBeforeImage = beforeImagesMap.get(tableItems[i]);
            if (tableBeforeImage == null || CollectionUtils.isEmpty(tableBeforeImage.getRows())) {
                continue;
            }
            String selectSQL = buildAfterImageSQL(joinTable, tableItems[i], tableBeforeImage);
            ResultSet rs = null;
            try (PreparedStatement pst = statementProxy.getConnection().prepareStatement(selectSQL)) {
                setAfterImageSQLPlaceHolderParams(joinConditionParams,tableBeforeImage.pkRows(), getTableMeta(tableItems[i]).getPrimaryKeyOnlyName(), pst);
                rs = pst.executeQuery();
                TableRecords afterImage = TableRecords.buildRecords(getTableMeta(tableItems[i]), rs);
                afterImagesMap.put(tableItems[i], afterImage);
            } finally {
                IOUtil.close(rs);
            }
        }
        return null;
    }

    private void setAfterImageSQLPlaceHolderParams(ArrayList<List<Object>> joinConditionParams,
                                        List<Map<String, Field>> pkRowsList, List<String> pkColumnNameList,
                                        PreparedStatement pst) throws SQLException {
        int paramIndex = 1;
        if (CollectionUtils.isNotEmpty(joinConditionParams)) {
            for (int i = 0, ts = joinConditionParams.size(); i < ts; i++) {
                List<Object> paramAppender = joinConditionParams.get(i);
                for (int j = 0, ds = paramAppender.size(); j < ds; j++) {
                    pst.setObject(paramIndex, paramAppender.get(j));
                    paramIndex++;
                }
            }
        }
        for (int i = 0; i < pkRowsList.size(); i++) {
            Map<String, Field> rowData = pkRowsList.get(i);
            for (String columnName : pkColumnNameList) {
                Field pkField = rowData.get(columnName);
                pst.setObject(paramIndex, pkField.getValue(), pkField.getType());
                paramIndex++;
            }
        }
    }

    private String buildAfterImageSQL(String joinTable, String itemTable,
        TableRecords beforeImage) throws SQLException {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        TableMeta itemTableMeta = getTableMeta(itemTable);
        StringBuilder prefix = new StringBuilder("SELECT ");
        List<String> pkColumns = getColumnNamesWithTablePrefixList(itemTable, recognizer.getTableAlias(itemTable), itemTableMeta.getPrimaryKeyOnlyName());
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(pkColumns, beforeImage.pkRows().size(), getDbType());
        String suffix = " FROM " + joinTable + " WHERE " + whereSql;
        //maybe duplicate row for select join sql.remove duplicate row by 'group by' condition
        suffix += GROUP_BY;
        List<String> itemTableUpdateColumns = getItemUpdateColumns(itemTableMeta, recognizer.getUpdateColumns());
        List<String> needUpdateColumns = getNeedColumns(itemTable, recognizer.getTableAlias(itemTable), itemTableUpdateColumns);
        suffix += buildGroupBy(pkColumns, needUpdateColumns);
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix.toString(), suffix);
        needUpdateColumns.forEach(selectSQLJoiner::add);
        return selectSQLJoiner.toString();
    }

    private List<String> getItemUpdateColumns(TableMeta itemTableMeta, List<String> updateColumns) {
        List<String> itemUpdateColumns = new ArrayList<>();
        Set<String> itemTableAllColumns = itemTableMeta.getAllColumns().keySet();
        String itemTableName = itemTableMeta.getTableName();
        String itemTableNameAlias = ((SQLUpdateRecognizer) sqlRecognizer).getTableAlias(itemTableName);
        for (String updateColumn : updateColumns) {
            if (updateColumn.contains(DOT)) {
                String[] specificTableColumn = updateColumn.split("\\.");
                String tableNamePrefix = specificTableColumn[0];
                String column = specificTableColumn[1];
                if ((tableNamePrefix.equals(itemTableName) || tableNamePrefix.equals(itemTableNameAlias)) && itemTableAllColumns.contains(column)) {
                    itemUpdateColumns.add(updateColumn);
                }
            } else if (itemTableAllColumns.contains(updateColumn)) {
                itemUpdateColumns.add(updateColumn);
            }
        }
        return itemUpdateColumns;
    }

    @Override
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if (CollectionUtils.isEmpty(beforeImagesMap) || CollectionUtils.isEmpty(afterImagesMap)) {
            throw new IllegalStateException("images can not be null");
        }
        for (Map.Entry<String, TableRecords> entry : beforeImagesMap.entrySet()) {
            String tableName = entry.getKey();
            TableRecords tableBeforeImage = entry.getValue();
            TableRecords tableAfterImage = afterImagesMap.get(tableName);
            if (tableBeforeImage.getRows().size() != tableAfterImage.getRows().size()) {
                throw new ShouldNeverHappenException("Before image size is not equaled to after image size, probably because you updated the primary keys.");
            }
            super.prepareUndoLog(tableBeforeImage, tableAfterImage);
        }
    }

    @Override
    protected TableMeta getTableMeta(String tableName) {
        ConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        return TableMetaCacheFactory.getTableMetaCache(connectionProxy.getDbType())
            .getTableMeta(connectionProxy.getTargetConnection(), tableName, connectionProxy.getDataSourceProxy().getResourceId());
    }

    /**
     * build a SQLUndoLog
     *
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @return sql undo log
     */
    @Override
    protected SQLUndoLog buildUndoItem(TableRecords beforeImage, TableRecords afterImage) {
        SQLType sqlType = sqlRecognizer.getSQLType();
        String tableName = beforeImage.getTableName();
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }

    /**
     * build group by condition which used for removing duplicate row in select join sql"
     *
     * @param pkColumns pkColumnsList
     * @param allSelectColumns allSelectColumns
     * @return return group by condition string.
     */
    private String buildGroupBy(List<String> pkColumns,List<String> allSelectColumns) {
        boolean groupByPks = true;
        //only pks group by is valid when db version >= 5.7.5
        try {
            if (isLowerSupportGroupByPksVersion) {
                if (StringUtils.isEmpty(sqlMode)) {
                    try (PreparedStatement preparedStatement = statementProxy.getConnection().prepareStatement("SELECT @@SQL_MODE");
                         ResultSet resultSet = preparedStatement.executeQuery()) {
                        if (resultSet.next()) {
                            sqlMode = resultSet.getString("@@SQL_MODE");
                        }
                    }
                }
                if (sqlMode.contains("ONLY_FULL_GROUP_BY")) {
                    groupByPks = false;
                }
            }
        } catch (Exception e) {
            groupByPks = false;
            logger.warn("determine group by pks or all columns error:{}",e.getMessage());
        }
        List<String> groupByColumns = groupByPks ? pkColumns : allSelectColumns;
        StringBuilder groupByStr = new StringBuilder();
        for (int i = 0; i < groupByColumns.size(); i++) {
            if (i > 0) {
                groupByStr.append(",");
            }
            groupByStr.append(ColumnUtils.addEscape(groupByColumns.get(i),getDbType()));
        }
        return groupByStr.toString();
    }

    private String getDbVersion() {
        return statementProxy.getConnectionProxy().getDataSourceProxy().getKernelVersion();
    }
}
