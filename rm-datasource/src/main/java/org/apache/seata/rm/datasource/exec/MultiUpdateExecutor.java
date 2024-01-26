/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.rm.datasource.exec;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.StringJoiner;

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.rm.datasource.SqlGenerateUtils;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLUpdateRecognizer;

/**
 * The type MultiSql executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public class MultiUpdateExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final boolean ONLY_CARE_UPDATE_COLUMNS = CONFIG.getBoolean(
        ConfigurationKeys.TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS, DefaultValues.DEFAULT_ONLY_CARE_UPDATE_COLUMNS);

    /**
     * Instantiates a new Multi update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the sql recognizers
     */
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

            if (StringUtils.isNotBlank(sqlUpdateRecognizer.getLimitCondition())) {
                throw new NotSupportYetException("Multi update SQL with limit condition is not support yet !");
            }
            if (StringUtils.isNotBlank(sqlUpdateRecognizer.getOrderByCondition())) {
                throw new NotSupportYetException("Multi update SQL with orderBy condition is not support yet !");
            }

            List<String> updateColumns = sqlUpdateRecognizer.getUpdateColumnsUnEscape();
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
        if (noWhereCondition) {
            //select all rows
            paramAppenderList.clear();
            whereCondition = new StringBuilder();
        }
        final StringJoiner selectSQLAppender = new StringJoiner(", ", prefix, buildSuffixSql(whereCondition.toString()));
        List<String> needColumns =
            getNeedColumns(tmeta.getTableName(), sqlRecognizer.getTableAlias(), new ArrayList<>(updateColumnsSet));
        needColumns.forEach(selectSQLAppender::add);
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
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = statementProxy.getConnection().prepareStatement(selectSQL);
            SqlGenerateUtils.setParamForPk(beforeImage.pkRows(), getTableMeta().getPrimaryKeyOnlyName(), pst);
            rs = pst.executeQuery();
            return TableRecords.buildRecords(tmeta, rs);
        } finally {
            IOUtil.close(rs, pst);
        }
    }

    private String buildAfterImageSQL(TableMeta tableMeta, TableRecords beforeImage) throws SQLException {

        Set<String> updateColumnsSet = new HashSet<>();
        for (SQLRecognizer recognizer : sqlRecognizers) {
            sqlRecognizer = recognizer;
            SQLUpdateRecognizer sqlUpdateRecognizer = (SQLUpdateRecognizer) sqlRecognizer;
            updateColumnsSet.addAll(sqlUpdateRecognizer.getUpdateColumnsUnEscape());
        }
        StringBuilder prefix = new StringBuilder("SELECT ");
        String suffix = " FROM " + getFromTableInSQL() + " WHERE " + SqlGenerateUtils.buildWhereConditionByPKs(tableMeta.getPrimaryKeyOnlyName(), beforeImage.pkRows().size(), getDbType());
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix.toString(), suffix);
        if (ONLY_CARE_UPDATE_COLUMNS) {
            if (!containsPK(new ArrayList<>(updateColumnsSet))) {
                selectSQLJoiner.add(getColumnNamesInSQL(tableMeta.getEscapePkNameList(getDbType())));
            }
            for (String updateCol : updateColumnsSet) {
                selectSQLJoiner.add(updateCol);
            }
        } else {
            for (String columnName : tableMeta.getAllColumns().keySet()) {
                selectSQLJoiner.add(ColumnUtils.addEscape(columnName, getDbType()));
            }
        }
        return selectSQLJoiner.toString();
    }

    protected String buildSuffixSql(String whereCondition) {
        final StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        if (StringUtils.isNotBlank(whereCondition)) {
            suffix.append(" WHERE ").append(whereCondition);
        }
        return suffix.append(" FOR UPDATE").toString();
    }
}
