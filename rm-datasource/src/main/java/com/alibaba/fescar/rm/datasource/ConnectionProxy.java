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
import com.alibaba.fescar.rm.datasource.sql.SQLType;
import com.alibaba.fescar.rm.datasource.sql.struct.Field;
import com.alibaba.fescar.rm.datasource.sql.struct.TableRecords;
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
     * Check lock.
     *
     * @param records the records
     * @throws SQLException the sql exception
     */
    public void checkLock(TableRecords records) throws SQLException {
        // Just check lock without requiring lock by now.
        String lockKeys = buildLockKey(records);
        try {
            boolean lockable = DataSourceManager.get().lockQuery(BranchType.AT, getDataSourceProxy().getResourceId(), context.getXid(), lockKeys);
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
     * @param records the records
     * @throws SQLException the sql exception
     */
    public void register(TableRecords records) throws SQLException {
        // Just check lock without requiring lock by now.
        String lockKeys = buildLockKey(records);
        try {
            DataSourceManager.get().branchRegister(BranchType.AT, getDataSourceProxy().getResourceId(), null, context.getXid(), lockKeys);
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
     * Prepare undo log.
     *
     * @param sqlType     the sql type
     * @param tableName   the table name
     * @param beforeImage the before image
     * @param afterImage  the after image
     * @throws SQLException the sql exception
     */
    public void prepareUndoLog(SQLType sqlType, String tableName, TableRecords beforeImage, TableRecords afterImage) throws SQLException {
        if(beforeImage.getRows().size() == 0 && afterImage.getRows().size() == 0) {
            return;
        }

        TableRecords lockKeyRecords = afterImage;
        if (sqlType == SQLType.DELETE) {
            lockKeyRecords = beforeImage;
        }
        String lockKeys = buildLockKey(lockKeyRecords);
        context.appendLockKey(lockKeys);
        SQLUndoLog sqlUndoLog = buildUndoItem(sqlType, tableName, beforeImage, afterImage);
        context.appendUndoItem(sqlUndoLog);
    }

    private SQLUndoLog buildUndoItem(SQLType sqlType, String tableName, TableRecords beforeImage, TableRecords afterImage) {
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        sqlUndoLog.setSqlType(sqlType);
        sqlUndoLog.setTableName(tableName);
        sqlUndoLog.setBeforeImage(beforeImage);
        sqlUndoLog.setAfterImage(afterImage);
        return sqlUndoLog;
    }

    private String buildLockKey(TableRecords rowsIncludingPK) {
        if (rowsIncludingPK.size() == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        sb.append(rowsIncludingPK.getTableMeta().getTableName());
        sb.append(":");

        boolean flag = false;
        for (Field field : rowsIncludingPK.pkRows()) {
            if (flag) {
                sb.append(",");
            } else {
                flag = true;
            }
            sb.append(field.getValue());
        }
        return sb.toString();
    }

    @Override
    public void commit() throws SQLException {
        if (context.inGlobalTransaction()) {
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
                    throw (SQLException) ex;
                } else {
                    throw new SQLException(ex);
                }
            }
            report(true);
            context.reset();

        } else {
            targetConnection.commit();
        }
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
            }}
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
                LOGGER.error("Failed to report [" + context.getBranchId() + "/" + context.getXid() + "] commit done [" + commitDone + "] Retry Countdown: " + retry);
                retry--;

                if (retry == 0) {
                    throw new SQLException("Failed to report branch status " + commitDone, ex);
                }
            }
        }
    }
}
