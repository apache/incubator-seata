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
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.DeleteExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.sqlparser.SQLDeleteRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.util.ColumnUtils;

/**
 * The type SqlServer Delete executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author GoodBoyCoder
 */
public class SqlServerDeleteExecutor<T, S extends Statement> extends DeleteExecutor<T, S> {
    /**
     * Instantiates a new sql Server Delete executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SqlServerDeleteExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    protected String buildBeforeImageSQL(SQLDeleteRecognizer visitor, TableMeta tableMeta, ArrayList<List<Object>> paramAppenderList) {
        String whereCondition = buildWhereCondition(visitor, paramAppenderList);
        StringBuilder suffix = new StringBuilder(" FROM ")
                .append(getFromTableInSQL())
                .append(" WITH(UPDLOCK) ");
        if (StringUtils.isNotBlank(whereCondition)) {
            suffix.append(WHERE).append(whereCondition);
        }
        StringJoiner selectSQLAppender = new StringJoiner(", ", "SELECT ", suffix.toString());
        for (String column : tableMeta.getAllColumns().keySet()) {
            selectSQLAppender.add(getColumnNameInSQL(ColumnUtils.addEscape(column, getDbType())));
        }
        return selectSQLAppender.toString();
    }
}
