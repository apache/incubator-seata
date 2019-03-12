/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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
package com.alibaba.fescar.rm.datasource.exec;

import java.sql.SQLException;
import java.sql.Statement;

import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.SQLVisitorFactory;

/**
 * The type Execute template.
 */
public class ExecuteTemplate {

    /**
     * Execute t.
     *
     * @param <T>               the type parameter
     * @param <S>               the type parameter
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param args              the args
     * @return the t
     * @throws SQLException the sql exception
     */
    public static <T, S extends Statement> T execute(StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {
        return execute(null, statementProxy, statementCallback, args);
    }

    /**
     * Execute t.
     *
     * @param <T>               the type parameter
     * @param <S>               the type parameter
     * @param sqlRecognizer     the sql recognizer
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param args              the args
     * @return the t
     * @throws SQLException the sql exception
     */
    public static <T, S extends Statement> T execute(SQLRecognizer sqlRecognizer,
                                                     StatementProxy<S> statementProxy,
                                                     StatementCallback<T, S> statementCallback,
                                                     Object... args) throws SQLException {

        if (!RootContext.inGlobalTransaction() && !RootContext.requireGlobalLock()) {
            // Just work as original statement
            return statementCallback.execute(statementProxy.getTargetStatement(), args);
        }

        if (sqlRecognizer == null) {
            sqlRecognizer = SQLVisitorFactory.get(
                statementProxy.getTargetSQL(),
                statementProxy.getConnectionProxy().getDbType());
        }
        Executor<T> executor = null;
        if (sqlRecognizer == null) {
            executor = new PlainExecutor<T, S>(statementProxy, statementCallback);
        } else {
            switch (sqlRecognizer.getSQLType()) {
                case INSERT:
                    executor = new InsertExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
                    break;
                case UPDATE:
                    executor = new UpdateExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
                    break;
                case DELETE:
                    executor = new DeleteExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
                    break;
                case SELECT_FOR_UPDATE:
                    executor = new SelectForUpdateExecutor(statementProxy, statementCallback, sqlRecognizer);
                    break;
                default:
                    executor = new PlainExecutor<T, S>(statementProxy, statementCallback);
                    break;
            }
        }
        T rs = null;
        try {
            rs = executor.execute(args);

        } catch (Throwable ex) {
            if (ex instanceof SQLException) {
                throw (SQLException)ex;
            } else {
                // Turn everything into SQLException
                new SQLException(ex);
            }
        }
        return rs;
    }
}
