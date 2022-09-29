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

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.SqlGenerateUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.rm.datasource.exec.UpdateExecutor;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLUpdateRecognizer;

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

/**
 * @author renliangyu857
 */
public class MySQLUpdateJoinExecutor<T, S extends Statement> extends UpdateExecutor<T, S> {
    private final Map<String, TableRecords> beforeImagesMap = new LinkedHashMap<>(4);
    private final Map<String, TableRecords> afterImagesMap = new LinkedHashMap<>(4);

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
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        String tableNames = recognizer.getTableName();
        if (StringUtils.isEmpty(tableNames)) {
            return null;
        }
        // update join sql,like update t1 inner join t2 on t1.id = t2.id set t1.name = ?; tableItems = {"update t1 inner join t2","t1","t2"}
        String[] tableItems = tableNames.split(recognizer.MULTI_TABLE_NAME_SEPERATOR);
        String joinTable = tableItems[0];
        int itemTableIndex = 1;
        for (int i = itemTableIndex; i < tableItems.length; i++) {
            String selectSQL = buildBeforeImageSQL(joinTable, tableItems[i], paramAppenderList);
            TableRecords tableRecords = buildTableRecords(getTableMeta(tableItems[i]), selectSQL, paramAppenderList);
            beforeImagesMap.put(tableItems[i], tableRecords);
        }
        return null;
    }

    private String buildBeforeImageSQL(String joinTable, String itemTable, ArrayList<List<Object>> paramAppenderList) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        StringBuilder prefix = new StringBuilder("SELECT ");
        StringBuilder suffix = new StringBuilder(" FROM ").append(joinTable);
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
        suffix.append(" FOR UPDATE");
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix.toString(), suffix.toString());
        TableMeta itemTableMeta = this.getTableMeta(itemTable);
        List<String> itemTableUpdateColumns = getItemUpdateColumns(itemTableMeta.getAllColumns().keySet(), recognizer.getUpdateColumns());
        List<String> needUpdateColumns = getNeedUpdateColumns(itemTable, recognizer.getTableAlias(itemTable), itemTableUpdateColumns);
        for (String needUpdateColumn : needUpdateColumns) {
            selectSQLJoin.add(needUpdateColumn);
        }
        return selectSQLJoin.toString();
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        String tableNames = recognizer.getTableName();
        if (StringUtils.isEmpty(tableNames)) {
            return null;
        }
        String[] tableItems = tableNames.split(recognizer.MULTI_TABLE_NAME_SEPERATOR);
        String joinTable = tableItems[0];
        int itemTableIndex = 1;
        for (int i = itemTableIndex; i < tableItems.length; i++) {
            TableRecords tableBeforeImage = beforeImagesMap.get(tableItems[i]);
            String selectSQL = buildAfterImageSQL(joinTable, tableItems[i], tableBeforeImage);
            ResultSet rs = null;
            try (PreparedStatement pst = statementProxy.getConnection().prepareStatement(selectSQL)) {
                SqlGenerateUtils.setParamForPk(tableBeforeImage.pkRows(), getTableMeta(tableItems[i]).getPrimaryKeyOnlyName(), pst);
                rs = pst.executeQuery();
                TableRecords afterImage = TableRecords.buildRecords(getTableMeta(tableItems[i]), rs);
                afterImagesMap.put(tableItems[i], afterImage);
            } finally {
                IOUtil.close(rs);
            }
        }
        return null;
    }

    private String buildAfterImageSQL(String joinTable, String itemTable,
        TableRecords beforeImage) throws SQLException {
        TableMeta itemTableMeta = getTableMeta(itemTable);
        StringBuilder prefix = new StringBuilder("SELECT ");
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(itemTableMeta.getPrimaryKeyOnlyName(), beforeImage.pkRows().size(), getDbType());
        String suffix = " FROM " + joinTable + " WHERE " + whereSql;
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix.toString(), suffix);
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        List<String> itemTableUpdateColumns = getItemUpdateColumns(itemTableMeta.getAllColumns().keySet(), recognizer.getUpdateColumns());
        List<String> needUpdateColumns = getNeedUpdateColumns(itemTable, recognizer.getTableAlias(itemTable), itemTableUpdateColumns);
        for (String needUpdateColumn : needUpdateColumns) {
            selectSQLJoiner.add(needUpdateColumn);
        }
        return selectSQLJoiner.toString();
    }

    private List<String> getItemUpdateColumns(Set<String> itemAllColumns, List<String> updateColumns) {
        List<String> itemUpdateColumns = new ArrayList<>();
        for (String updateColumn : updateColumns) {
            if (itemAllColumns.contains(updateColumn)) {
                itemUpdateColumns.add(updateColumn);
            }
        }
        return itemUpdateColumns;
    }

    @Override
    protected void prepareUndoLog(TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if (beforeImagesMap == null || afterImagesMap == null) {
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
}
