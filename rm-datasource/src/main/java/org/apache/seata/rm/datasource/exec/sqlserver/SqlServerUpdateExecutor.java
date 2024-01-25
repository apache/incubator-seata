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
import java.util.StringJoiner;

import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.rm.datasource.StatementProxy;
import org.apache.seata.rm.datasource.exec.StatementCallback;
import org.apache.seata.rm.datasource.exec.UpdateExecutor;
import org.apache.seata.sqlparser.struct.TableMeta;
import org.apache.seata.sqlparser.SQLRecognizer;
import org.apache.seata.sqlparser.SQLUpdateRecognizer;
import org.apache.seata.sqlparser.util.JdbcConstants;

/**
 * @date 2021-07-21
 */
@LoadLevel(name = JdbcConstants.SQLSERVER, scope = Scope.PROTOTYPE)
public class SqlServerUpdateExecutor<T, S extends Statement> extends UpdateExecutor<T, S> {
    /**
     * Instantiates a new SqlServer Update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SqlServerUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }


    @Override
    protected String buildBeforeImageSQL(TableMeta tableMeta, ArrayList<List<Object>> paramAppenderList) {
        SQLUpdateRecognizer recognizer = (SQLUpdateRecognizer) sqlRecognizer;
        StringBuilder prefix = new StringBuilder("SELECT ");
        StringBuilder suffix = new StringBuilder(" FROM ")
                .append(getFromTableInSQL())
                .append(" WITH(UPDLOCK) ");
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            suffix.append(WHERE).append(whereCondition);
        }
        StringJoiner selectSQLJoin = new StringJoiner(", ", prefix.toString(), suffix.toString());
        List<String> needUpdateColumns = getNeedColumns(tableMeta.getTableName(), sqlRecognizer.getTableAlias(), recognizer.getUpdateColumnsUnEscape());
        for (String needUpdateColumn : needUpdateColumns) {
            selectSQLJoin.add(needUpdateColumn);
        }
        return selectSQLJoin.toString();
    }
}
