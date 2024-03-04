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

import org.apache.seata.common.exception.NotSupportYetException;
import org.apache.seata.common.util.StringUtils;


import org.apache.seata.sqlparser.util.ColumnUtils;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.sqlparser.SQLDeleteRecognizer;
import org.apache.seata.sqlparser.SQLRecognizer;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * The type MultiSql executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public class MultiDeleteExecutor<T, S extends Statement> extends AbstractDMLBaseExecutor<T, S> {
    public MultiDeleteExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
        if (sqlRecognizers.size() == 1) {
            DeleteExecutor executor = new DeleteExecutor(statementProxy, statementCallback, sqlRecognizers.get(0));
            return executor.beforeImage();
        }
        final TableMeta tmeta = getTableMeta(sqlRecognizers.get(0).getTableName());
        final ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        StringBuilder whereCondition = new StringBuilder();
        for (SQLRecognizer recognizer : sqlRecognizers) {
            sqlRecognizer = recognizer;
            SQLDeleteRecognizer visitor = (SQLDeleteRecognizer) recognizer;

            if (StringUtils.isNotBlank(visitor.getLimitCondition())) {
                throw new NotSupportYetException("Multi delete SQL with limit condition is not support yet !");
            }
            if (StringUtils.isNotBlank(visitor.getOrderByCondition())) {
                throw new NotSupportYetException("Multi delete SQL with orderBy condition is not support yet !");
            }

            String whereConditionStr = buildWhereCondition(visitor, paramAppenderList);
            if (StringUtils.isBlank(whereConditionStr)) {
                whereCondition = new StringBuilder();
                paramAppenderList.clear();
                break;
            }
            if (whereCondition.length() > 0) {
                whereCondition.append(" OR ");
            }
            whereCondition.append(whereConditionStr);
        }
        StringBuilder suffix = new StringBuilder(" FROM ").append(getFromTableInSQL());
        if (whereCondition.length() > 0) {
            suffix.append(" WHERE ").append(whereCondition);
        }
        suffix.append(" FOR UPDATE");
        final StringJoiner selectSQLAppender = new StringJoiner(", ", "SELECT ", suffix.toString());
        for (String column : tmeta.getAllColumns().keySet()) {
            selectSQLAppender.add(getColumnNameInSQL(ColumnUtils.addEscape(column, getDbType())));
        }
        return buildTableRecords(tmeta, selectSQLAppender.toString(), paramAppenderList);
    }

    @Override
    protected TableRecords afterImage(TableRecords beforeImage) throws SQLException {
        return TableRecords.empty(getTableMeta(sqlRecognizers.get(0).getTableName()));
    }
}
