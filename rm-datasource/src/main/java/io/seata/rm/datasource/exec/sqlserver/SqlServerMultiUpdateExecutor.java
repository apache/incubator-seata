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
package io.seata.rm.datasource.exec.sqlserver;

import java.sql.Statement;
import java.util.List;

import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.MultiUpdateExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.sqlparser.SQLRecognizer;

/**
 * The type MultiSql executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author GoodBoyCoder
 */
public class SqlServerMultiUpdateExecutor<T, S extends Statement> extends MultiUpdateExecutor<T, S> {
    /**
     * Instantiates a new SqlServer Multi update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizers    the sql recognizers
     */
    public SqlServerMultiUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    @Override
    protected String buildSuffixSql(String whereCondition) {
        final StringBuilder suffix = new StringBuilder(" FROM ")
                .append(getFromTableInSQL())
                .append(" WITH(UPDLOCK) ");
        if (StringUtils.isNotBlank(whereCondition)) {
            suffix.append(" WHERE ").append(whereCondition);
        }
        return suffix.toString();
    }
}
