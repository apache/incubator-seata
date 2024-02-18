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
import java.util.StringJoiner;

import org.apache.seata.common.util.IOUtil;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.rm.datasource.SqlGenerateUtils;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLUpdateRecognizer;

/**
 * The type Update executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public class UpdateExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {

    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final boolean ONLY_CARE_UPDATE_COLUMNS = CONFIG.getBoolean(
        ConfigurationKeys.TRANSACTION_UNDO_ONLY_CARE_UPDATE_COLUMNS, DefaultValues.DEFAULT_ONLY_CARE_UPDATE_COLUMNS);

    /**
     * Instantiates a new Update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public UpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
        SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        TableMeta tmeta = getTableMeta();
        String selectSQL = buildBeforeImageSQL(tmeta, paramAppenderList);
        return buildTableRecords(tmeta, selectSQL, paramAppenderList);
    }

    protected String buildBeforeImageSQL(TableMeta tableMeta, ArrayList<List<Object>> paramAppenderList) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        StringBuilder prefix = new StringBuilder("SELECT ");
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
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
        List<String> needUpdateColumns = getNeedColumns(tableMeta.getTableName(), sqlRecognizer.getTableAlias(), recognizer.getUpdateColumnsUnEscape());
        needUpdateColumns.forEach(selectSQLJoin::add);
        return selectSQLJoin.toString();
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        if (beforeImage == null || beforeImage.size() == 0) {
            return TableRecords.empty(getTableMeta());
        }
        TableMeta tmeta = getTableMeta();
        PreparedStatement pst = null;
        ResultSet rs = null;
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer)sqlRecognizer;
        List<String> whereColumns = recognizer.getWhereColumns();
        boolean contain = tmeta.containsPK(whereColumns);
        if (contain) {
            String selectSQL = buildAfterImageSQL(tmeta, beforeImage);
            try {
                pst = statementProxy.getConnection().prepareStatement(selectSQL);
                SqlGenerateUtils.setParamForPk(beforeImage.pkRows(), getTableMeta().getPrimaryKeyOnlyName(), pst);
                rs = pst.executeQuery();
                return TableRecords.buildRecords(tmeta, rs);
            } finally {
                IOUtil.close(rs, pst);
            }
        } else {
            return beforeImage();
        }
    }

    private String buildAfterImageSQL(TableMeta tableMeta, TableRecords beforeImage) throws SQLException {
        String prefix = "SELECT ";
        String whereSql = SqlGenerateUtils.buildWhereConditionByPKs(tableMeta.getPrimaryKeyOnlyName(), beforeImage.pkRows().size(), getDbType());
        String suffix = " FROM " + getFromTableInSQL() + " WHERE " + whereSql;
        StringJoiner selectSQLJoiner = new StringJoiner(", ", prefix, suffix);
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        List<String> needUpdateColumns = getNeedColumns(tableMeta.getTableName(), sqlRecognizer.getTableAlias(), recognizer.getUpdateColumnsUnEscape());
        needUpdateColumns.forEach(selectSQLJoiner::add);
        return selectSQLJoiner.toString();
    }

}
