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

import com.alibaba.fescar.rm.datasource.AbstractConnectionProxy;
import com.alibaba.fescar.rm.datasource.StatementProxy;
import com.alibaba.fescar.rm.datasource.sql.SQLRecognizer;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract dml base executor.
 *
 * @param <T> the type parameter
 * @param <S> the type parameter
 */
public abstract class AbstractDMLBaseExecutor<T, S extends Statement> extends BaseTransactionalExecutor<T, S> {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractDMLBaseExecutor.class);

    /**
     * Instantiates a new Abstract dml base executor.
     *
     * @param statementProxy    the statement proxy
     * @param statementCallback the statement callback
     * @param sqlRecognizer     the sql recognizer
     */
    public AbstractDMLBaseExecutor(StatementProxy<S> statementProxy, StatementCallback<T, S> statementCallback, SQLRecognizer sqlRecognizer) {
        super(statementProxy, statementCallback, sqlRecognizer);
    }

    @Override
    public T doExecute(Object... args) throws Throwable {
        AbstractConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        if (connectionProxy.getAutoCommit()) {
            return executeAutoCommitTrue(args);
        } else {
            return executeAutoCommitFalse(args);
        }
    }

    /**
     * Execute auto commit false t.
     *
     * @param args the args
     * @return the t
     * @throws Throwable the throwable
     */
    protected T executeAutoCommitFalse(Object[] args) throws Throwable {
        TableRecords beforeImage = beforeImage();
        T result = statementCallback.execute(statementProxy.getTargetStatement(), args);
        TableRecords afterImage = afterImage(beforeImage);
        prepareUndoLog(beforeImage, afterImage);
        return result;
    }

    /**
     * Execute auto commit true t.
     *
     * @param args the args
     * @return the t
     * @throws Throwable the throwable
     */
    protected T executeAutoCommitTrue(Object[] args) throws Throwable {
        T result = null;
        AbstractConnectionProxy connectionProxy = statementProxy.getConnectionProxy();
        LockRetryController lockRetryController = new LockRetryController();
        try {
            connectionProxy.setAutoCommit(false);
            while (true) {
                try {
                    result = executeAutoCommitFalse(args);
                    connectionProxy.commit();
                    break;
                } catch (LockConflictException lockConflict) {
                    connectionProxy.getTargetConnection().rollback();
                    lockRetryController.sleep(lockConflict);
                }
            }

        } catch (Exception e) {
            // when exception occur in finally,this exception will lost, so just print it here
            LOGGER.error("exception occur", e);
            throw e;
        } finally {
            connectionProxy.setAutoCommit(true);
        }
        return result;
    }

    /**
     * Before image table records.
     *
     * @return the table records
     * @throws SQLException the sql exception
     */
    protected abstract TableRecords beforeImage() throws SQLException;

    /**
     * After image table records.
     *
     * @param beforeImage the before image
     * @return the table records
     * @throws SQLException the sql exception
     */
    protected abstract TableRecords afterImage(TableRecords beforeImage) throws SQLException;

}
