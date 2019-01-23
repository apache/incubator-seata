package com.alibaba.fescar.rm.datasource;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.rm.datasource.exec.LockConflictException;
import com.alibaba.fescar.rm.datasource.undo.UndoLogManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Random;

public class ConnectionProxyForTest extends ConnectionProxy {

    static final Logger LOGGER = LoggerFactory.getLogger(ConnectionProxyForTest.class);

    public ConnectionProxyForTest(DataSourceProxy dataSourceProxy, Connection targetConnection, String dbType) {
        super(dataSourceProxy, targetConnection, dbType);
    }

    @Override
    public void checkLock(String lockKeys) throws SQLException {
        LOGGER.info("do nothing.lockKeys={}", lockKeys);
    }

    @Override
    public void register(String lockKeys) throws SQLException {
        LOGGER.info("do nothing.lockKeys={}", lockKeys);
    }

    @Override
    public void commit() throws SQLException {
        if (this.getContext().inGlobalTransaction()) {
            try {
                register();
            } catch (TransactionException e) {
                recognizeLockKeyConflictException(e);
            }

            try {
                if (this.getContext().hasUndoLog()) {
                    UndoLogManager.flushUndoLogs(this);
                }
                targetConnection.commit();
            } catch (Throwable ex) {

                if (ex instanceof SQLException) {
                    throw (SQLException) ex;
                } else {
                    throw new SQLException(ex);
                }
            }
            this.getContext().reset();

        } else {
            targetConnection.commit();
        }
    }

    private void register() throws TransactionException {
        Long branchId = System.currentTimeMillis();
        this.getContext().setBranchId(branchId);
        LOGGER.info("branchId={}", branchId);
    }

    private void recognizeLockKeyConflictException(TransactionException te) throws SQLException {
        if (te.getCode() == TransactionExceptionCode.LockKeyConflict) {
            throw new LockConflictException();
        } else {
            throw new SQLException(te);
        }
    }

}
