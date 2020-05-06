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

import io.seata.common.util.IOUtil;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.struct.Field;
import io.seata.rm.datasource.sql.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.SQLUpdateRecognizer;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * The type MultiSql executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author wangwei-ying
 */
public class MultiUpdateExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {
    public MultiUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        if (sqlRecognizers.size() == 1) {
            UpdateExecutor executor = new UpdateExecutor<>(statementProxy, statementCallback, sqlRecognizers.get(0));
            return executor.beforeImage();
        }
        final TableMeta tmeta = getTableMeta(sqlRecognizers.get(0).getTableName());

        final ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        Set<String> updateColumnsSet = new HashSet<>();
        StringBuilder whereCondition = new StringBuilder();
        boolean noWhereCondition = false;
        for (SQLRecognizer recognizer : sqlRecognizers) {
            sqlRecognizer = recognizer;
            SQLUpdateRecognizer sqlUpdateRecognizer = (SQLUpdateRecognizer) recognizer;
            List<String> updateColumns = sqlUpdateRecognizer.getUpdateColumns();
            updateColumnsSet.addAll(updateColumns);
            if (noWhereCondition) {
                continue;
            }
            String whereConditionStr = buildWhereCondition(sqlUpdateRecognizer, paramAppenderList);
            if (StringUtils.isBlank(whereConditionStr)) {
                noWhereCondition = true;
            } else {
                if (whereCondition.length() > 0) {
                    whereCondition.append(" OR ");
                }
                whereCondition.append(whereConditionStr);
            }
        }
        StringBuilder prefix = new StringBuilder("SELECT ");
        if (!containsPK(new ArrayList<>(updateColumnsSet))) {
            prefix.append(getColumnNamesInSQL(tmeta.getEscapePkNameList(getDbType())));
        }
        final StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        if (noWhereCondition) {
            //select all rows
            paramAppenderList.clear();
        } else {
            suffix.append(" WHERE ").append(whereCondition);
        }
        suffix.append(" FOR UPDATE");
        final StringJoiner selectSQLAppender = new StringJoiner(", ", prefix, suffix.toString());
        for (String updateCol : updateColumnsSet) {
            selectSQLAppender.add(updateCol);
        }
        return buildTableRecords(tmeta, selectSQLAppender.toString(), paramAppenderList);
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        if (sqlRecognizers.size() == 1) {
            UpdateExecutor executor = new UpdateExecutor<>(statementProxy, statementCallback, sqlRecognizers.get(0));
            return executor.afterImage(beforeImage);
        }
        if (beforeImage == null || beforeImage.size() == 0) {
            return TableRecords.empty(getTableMeta(sqlRecognizers.get(0).getTableName()));
        }
        TableMeta tmeta = getTableMeta(sqlRecognizers.get(0).getTableName());
        String selectSQL = buildAfterImageSQL(tmeta, beforeImage);
        ResultSet rs = null;
        try (PreparedStatement pst = statementProxy.getConnection().prepareStatement(selectSQL);) {
            List<Map<String,Field>> pkRowsList = beforeImage.pkRows();
            List<String> pkColumnNameList = getTableMeta().getPrimaryKeyOnlyName();
            int paramIndex = 1;
            for (int i = 0;i < pkColumnNameList.size(); i++) {
                String pkKey = pkColumnNameList.get(i);
                List<Field> fieldList = pkRowsList.stream()
                        .map(e -> e.get(pkKey))
                        .filter(e -> Objects.nonNull(e))
                        .collect(Collectors.toList());
                for (Field pkField:fieldList) {
                    pst.setObject(paramIndex, pkField.getValue(), pkField.getType());
                    paramIndex++;
                }
            }
            rs = pst.executeQuery();
            return TableRecords.buildRecords(tmeta, rs);
        } finally {
            IOUtil.close(rs);
        }
    }

    private String buildAfterImageSQL(TableMeta tableMeta, TableRecords beforeImage) throws SQLException {

        Set<String> updateColumnsSet = new HashSet<>();
        for (SQLRecognizer recognizer : sqlRecognizers) {
            sqlRecognizer = recognizer;
            SQLUpdateRecognizer sqlUpdateRecognizer = (SQLUpdateRecognizer) sqlRecognizer;
            updateColumnsSet.addAll(sqlUpdateRecognizer.getUpdateColumns());
        }
        StringBuilder prefix = new StringBuilder("SELECT ");
        if (!containsPK(new ArrayList<>(updateColumnsSet))) {
            // PK should be included.
            prefix.append(getColumnNamesInSQL(tableMeta.getEscapePkNameList(getDbType())));
        }
        String suffix = " FROM " + getFromTableInSQL() + " WHERE " + buildWhereConditionByPKs(beforeImage.pkRows());
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix.toString(), suffix);
        for (String column : updateColumnsSet) {
            selectSQLJoiner.add(column);
        }
        return selectSQLJoiner.toString();
    }
}
