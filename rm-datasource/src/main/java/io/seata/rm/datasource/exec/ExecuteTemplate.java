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

import java.sql.SQLException;
import java.sql.Statement;

import co.faao.plugin.starter.seata.util.ElasticsearchUtil;
import io.seata.core.context.RootContext;
import io.seata.rm.datasource.ConnectionContext;
import io.seata.rm.datasource.StatementProxy;
import io.seata.rm.datasource.exec.noseata.*;
import io.seata.rm.datasource.sql.SQLRecognizer;
import io.seata.rm.datasource.sql.SQLType;
import io.seata.rm.datasource.sql.SQLVisitorFactory;
import io.seata.rm.datasource.sql.struct.TableRecords;
import io.seata.rm.datasource.undo.SQLUndoLog;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * The type Execute template.
 *
 * @author sharajava
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
            sqlRecognizer = SQLVisitorFactory.get( statementProxy.getTargetSQL(),  statementProxy.getConnectionProxy().getDbType());
            if (sqlRecognizer != null) {
                NoSeata executor = null;
                switch (sqlRecognizer.getSQLType()) {
                    case INSERT:
                        executor = new InsertExecutorNoSeata<T, S>(statementProxy, statementCallback, sqlRecognizer);
                        break;
                    case UPDATE:
                        executor = new UpdateExecutorNoSeata<T, S>(statementProxy, statementCallback, sqlRecognizer);
                        break;
                    case DELETE:
                        executor = new DeleteExecutorNoSeata<T, S>(statementProxy, statementCallback, sqlRecognizer);
                        break;
                    default:
                        executor = null;
                        break;
                }
                if(executor!=null) {
                    //保存数据前后镜像
                    TableRecords beforeImage = null;
                    try {
                        beforeImage = executor.beforeImage();
                    } catch (Exception e) {

                    }
                    T result = statementCallback.execute(statementProxy.getTargetStatement(), args);

                    try {
                        TableRecords afterImage = executor.afterImage(beforeImage);
                        SQLUndoLog sQLUndoLog = buildUndoItem(sqlRecognizer, beforeImage, afterImage);
                        //业务数据操作前后插入到es数据库
                        ConnectionContext connectionContext = statementProxy.getConnectionProxy().getContext();
                        String xid = connectionContext.getXid();
                        ElasticsearchUtil.addData(xid,sQLUndoLog);
                    } catch (Exception e) {

                    }
                    return result;
                } else {
                    // Just work as original statement
                    return statementCallback.execute(statementProxy.getTargetStatement(), args);
                }
            } else {
                // Just work as original statement
                return statementCallback.execute(statementProxy.getTargetStatement(), args);
            }
        }

        if (sqlRecognizer == null) {
            sqlRecognizer = SQLVisitorFactory.get(  statementProxy.getTargetSQL(),  statementProxy.getConnectionProxy().getDbType());
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
                    executor = new SelectForUpdateExecutor<T, S>(statementProxy, statementCallback, sqlRecognizer);
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
            if (!(ex instanceof SQLException)) {
                // Turn other exception into SQLException
                ex = new SQLException(ex);
            }
            throw (SQLException)ex;
        }
        return rs;
    }

    protected static SQLUndoLog buildUndoItem(SQLRecognizer sqlRecognizer,TableRecords beforeImage, TableRecords afterImage) {
        SQLType sqlType = sqlRecognizer.getSQLType();
        String tableName = sqlRecognizer.getTableName();
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        sqlUndoLog.setExecuteDate(DateFormatUtils.format(new java.util.Date(),"yyyy-MM-dd HH:mm:ss"));
        return sqlUndoLog;
    }
}
