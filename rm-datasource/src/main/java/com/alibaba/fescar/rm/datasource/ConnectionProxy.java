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

package com.alibaba.fescar.rm.datasource;

import java.sql.Connection;
import java.sql.SQLException;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.rm.datasource.exec.LockConflictException;
import com.alibaba.fescar.rm.datasource.undo.SQLUndoLog;
import com.alibaba.fescar.rm.datasource.undo.UndoLogManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Connection proxy.
 */
public class ConnectionProxy extends AbstractConnectionProxy {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxy.class);

    private ConnectionContext context = new ConnectionContext();

    /**
     * Instantiates a new Connection proxy.
     *
     * @param dataSourceProxy  the data source proxy
     * @param targetConnection the target connection
     * @param dbType           the db type
     */
    public ConnectionProxy(DataSourceProxy dataSourceProxy, Connection targetConnection, String dbType) {
        super(dataSourceProxy, targetConnection, dbType);
    }

    /**
     * Gets context.
     *
     * @return the context
     */
    public ConnectionContext getContext() {
        return context;
    }

    /**
     * Bind.
     *
     * @param xid the xid
     */
    public void bind(String xid) {
        context.bind(xid);
    }

    /**
     * set global lock requires flag
     *
     * @param isLock whether to lock
     */
    public void setGlobalLockRequire(boolean isLock) {
        context.setGlobalLockRequire(isLock);
    }

    /**
     * get global lock requires flag
     */
    public boolean isGlobalLockRequire() {
        return context.isGlobalLockRequire();
    }

    /**
     * Check lock.
     *
     * @param lockKeys the lockKeys
     * @throws SQLException the sql exception
     */
    public void checkLock(String lockKeys) throws SQLException {
        // Just check lock without requiring lock by now.
        try {
            boolean lockable = DataSourceManager.get().lockQuery(BranchType.AT, getDataSourceProxy().getResourceId(),
                context.getXid(), lockKeys);
            if (!lockable) {
                throw new LockConflictException();
            }
        } catch (TransactionException e) {
            recognizeLockKeyConflictException(e);
        }
    }

    /**
     * Register.
     *
     * @param lockKeys the lockKeys
     * @throws SQLException the sql exception
     */
    public void register(String lockKeys) throws SQLException {
        // Just check lock without requiring lock by now.
        try {
            DataSourceManager.get().branchRegister(BranchType.AT, getDataSourceProxy().getResourceId(), null,
                context.getXid(), lockKeys);
        } catch (TransactionException e) {
            recognizeLockKeyConflictException(e);
        }
    }

    private void recognizeLockKeyConflictException(TransactionException te) throws SQLException {
        if (te.getCode() == TransactionExceptionCode.LockKeyConflict) {
            throw new LockConflictException();
        } else {
            throw new SQLException(te);
        }

    }

    /**
     * append sqlUndoLog
     *
     * @param sqlUndoLog the sql undo log
     */
    public void appendUndoLog(SQLUndoLog sqlUndoLog) {
        context.appendUndoItem(sqlUndoLog);
    }

    /**
     * append lockKey
     *
     * @param lockKey the lock key
     */
    public void appendLockKey(String lockKey) {
        context.appendLockKey(lockKey);
    }

    @Override
    public void commit() throws SQLException {
        if (context.inGlobalTransaction()) {
            processGlobalTransactionCommit();
        } else if (context.isGlobalLockRequire()) {
            processLocalCommitWithGlobalLocks();
        } else {
            targetConnection.commit();
        }
    }

    private void processLocalCommitWithGlobalLocks() throws SQLException {

        checkLock(context.buildLockKeys());
        try {
            targetConnection.commit();
        } catch (Throwable ex) {
            throw new SQLException(ex);
        }
        context.reset();
    }

    private void processGlobalTransactionCommit() throws SQLException {
        try {
            register();
        } catch (TransactionException e) {
            recognizeLockKeyConflictException(e);
        }

        try {
            if (context.hasUndoLog()) {
                UndoLogManager.flushUndoLogs(this);
            }
            targetConnection.commit();
        } catch (Throwable ex) {
            report(false);
            if (ex instanceof SQLException) {
                throw new SQLException(ex);
            }
        }
        report(true);
        context.reset();
    }

    private void register() throws TransactionException {
        Long branchId = DataSourceManager.get().branchRegister(BranchType.AT, getDataSourceProxy().getResourceId(),
            null, context.getXid(), context.buildLockKeys());
        context.setBranchId(branchId);
    }

    @Override
    public void rollback() throws SQLException {
        targetConnection.rollback();
        if (context.inGlobalTransaction()) {
            if (context.isBranchRegistered()) {
                report(false);
            }
        }
        context.reset();
    }

    @Override
    public void setAutoCommit(boolean autoCommit) throws SQLException {
        if ((autoCommit) && !getAutoCommit()) {
            // change autocommit from false to true, we should commit() first according to JDBC spec.
            commit();
        }
        targetConnection.setAutoCommit(autoCommit);
    }

    private void report(boolean commitDone) throws SQLException {
        int retry = 5; // TODO: configure
        while (retry > 0) {
            try {
                DataSourceManager.get().branchReport(context.getXid(), context.getBranchId(),
                    (commitDone ? BranchStatus.PhaseOne_Done : BranchStatus.PhaseOne_Failed), null);
                return;
            } catch (Throwable ex) {
                LOGGER.error("Failed to report [" + context.getBranchId() + "/" + context.getXid() + "] commit done ["
                    + commitDone + "] Retry Countdown: " + retry);
                retry--;

                if (retry == 0) {
                    throw new SQLException("Failed to report branch status " + commitDone, ex);
                }
            }
        }
    }
}
