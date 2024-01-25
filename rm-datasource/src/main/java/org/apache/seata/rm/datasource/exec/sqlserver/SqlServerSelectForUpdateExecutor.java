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
package org.apache.seata.rm.datasource.exec.sqlserver;

import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.SelectForUpdateExecutor;
import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLSelectRecognizer;

/**
 * The type SqlServer Select for update executor.
 *
 * @param <S> the type parameter
 */
public class SqlServerSelectForUpdateExecutor<T, S extends Statement> extends SelectForUpdateExecutor<T, S> {
    /**
     * Instantiates a new Sqlserver Select for update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SqlServerSelectForUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected String buildSelectSQL(ArrayList<List<Object>> paramAppenderList) {
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer) sqlRecognizer;
        StringBuilder selectSQLAppender = new StringBuilder("SELECT ");
        selectSQLAppender.append(getColumnNamesInSQL(getTableMeta().getEscapePkNameList(getDbType())));
        selectSQLAppender.append(" FROM ")
                .append(getFromTableInSQL())
                .append(" WITH(UPDLOCK) ");
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        String orderByCondition = buildOrderCondition(recognizer, paramAppenderList);
        String limitCondition = buildLimitCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            selectSQLAppender.append(" WHERE ").append(whereCondition);
        }
        if (StringUtils.isNotBlank(orderByCondition)) {
            selectSQLAppender.append(" ").append(orderByCondition);
        }
        if (StringUtils.isNotBlank(limitCondition)) {
            selectSQLAppender.append(" ").append(limitCondition);
        }
        return selectSQLAppender.toString();
    }
}
