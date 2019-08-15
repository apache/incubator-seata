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

import java.sql.Connection;
import java.sql.Savepoint;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import io.seata.common.util.StringUtils;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.SQLSelectRecognizer;
import io.seata.rm.datasource.sql.struct.TableRecords;

/**
 * The type Select for update executor.
 *
 * @author sharajava
 *
 * @param <S> the type parameter
 */
public class SelectForUpdateExecutor<T, S extends Statement> extends BaseTransactionalExecutor<T, S> {

    /**
     * Instantiates a new Select for update executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public SelectForUpdateExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback,
                                   SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public T doExecute(Object... args) throws Throwable {
        Connection conn = statementProxy.getConnection();
        T rs = null;
        Savepoint sp = null;
        LockRetryController lockRetryController = new LockRetryController();
        boolean originalAutoCommit = conn.getAutoCommit();
        ArrayList<List<Object>> paramAppenderList = new ArrayList<>();
        String selectPKSQL = buildSelectSQL(paramAppenderList);
        try {
            if (originalAutoCommit) {
                conn.setAutoCommit(false);
            }
            sp = conn.setSavepoint();

            while (true) {
                try {
                    // #870
                    // execute return Boolean
                    // executeQuery return ResultSet
                    rs = statementCallback.execute(statementProxy.getTargetStatement(), args);

                    // Try to get global lock of those rows selected
                    TableRecords selectPKRows = buildTableRecords(getTableMeta(), selectPKSQL, paramAppenderList);
                    String lockKeys = buildLockKey(selectPKRows);
                    if (StringUtils.isNullOrEmpty(lockKeys)) {
                        break;
                    }

                    if (RootContext.inGlobalTransaction()) {
                        //do as usual
                        statementProxy.getConnectionProxy().checkLock(lockKeys);
                    } else if (RootContext.requireGlobalLock()) {
                        //check lock key before commit just like DML to avoid reentrant lock problem(no xid thus can
                        // not reentrant)
                        statementProxy.getConnectionProxy().appendLockKey(lockKeys);
                    } else {
                        throw new RuntimeException("Unknown situation!");
                    }
                    break;
                } catch (LockConflictException lce) {
                    conn.rollback(sp);
                    lockRetryController.sleep(lce);
                }
            }
        } finally {
            if (sp != null) {
                conn.releaseSavepoint(sp);
            }
            if (originalAutoCommit) {
                conn.setAutoCommit(true);
            }
        }
        return rs;
    }

    private String buildSelectSQL(ArrayList<List<Object>> paramAppenderList){
        SQLSelectRecognizer recognizer = (SQLSelectRecognizer)sqlRecognizer;
        StringBuilder selectSQLAppender = new StringBuilder("SELECT ");
        selectSQLAppender.append(getColumnNameInSQL(getTableMeta().getPkName()));
        selectSQLAppender.append(" FROM " + getFromTableInSQL());
        String whereCondition = buildWhereCondition(recognizer, paramAppenderList);
        if (StringUtils.isNotBlank(whereCondition)) {
            selectSQLAppender.append(" WHERE " + whereCondition);
        }
        selectSQLAppender.append(" FOR UPDATE");
        return selectSQLAppender.toString();
    }
}
