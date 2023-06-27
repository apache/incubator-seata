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

import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.util.StringUtils;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.MultiDeleteExecutor;
import io.seata.rm.datasource.exec.StatementCallback;
import io.seata.sqlparser.struct.TableMeta;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.sqlparser.SQLDeleteRecognizer;
import io.seata.sqlparser.SQLRecognizer;
import io.seata.sqlparser.util.ColumnUtils;

/**
 * The type SqlServer MultiSql executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 * @author GoodBoyCoder
 */
public class SqlServerMultiDeleteExecutor<T, S extends Statement> extends MultiDeleteExecutor<T, S> {
    public SqlServerMultiDeleteExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, List<SQLRecognizer> sqlRecognizers) {
        super(statementProxy, statementCallback, sqlRecognizers);
    }

    @Override
    protected TableRecords beforeImage() throws SQLException {
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
            if (whereCondition.length() > 0 && sqlRecognizers.size() > 1) {
                whereCondition.append(" OR ");
            }
            whereCondition.append(whereConditionStr);
        }
        StringBuilder suffix = new StringBuilder(" FROM ")
                .append(getFromTableInSQL())
                .append(" WITH(UPDLOCK) ");
        if (whereCondition.length() > 0) {
            suffix.append(" WHERE ").append(whereCondition);
        }
        final StringJoiner selectSQLAppender = new StringJoiner(", ", "SELECT ", suffix.toString());
        for (String column : tmeta.getAllColumns().keySet()) {
            selectSQLAppender.add(getColumnNameInSQL(ColumnUtils.addEscape(column, getDbType())));
        }
        return buildTableRecords(tmeta, selectSQLAppender.toString(), paramAppenderList);
    }
}
