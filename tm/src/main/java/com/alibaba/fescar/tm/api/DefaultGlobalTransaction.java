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

package com.alibaba.fescar.tm.api;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.core.context.RootContext;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.TransactionManager;
import com.alibaba.fescar.tm.DefaultTransactionManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default global transaction.
 */
public class DefaultGlobalTransaction implements GlobalTransaction {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGlobalTransaction.class);

    private static final int DEFAULT_GLOBAL_TX_TIMEOUT = 60000;

    private static final String DEFAULT_GLOBAL_TX_NAME = "default";

    private TransactionManager transactionManager;

    private String xid;

    private GlobalStatus status;

    private GlobalTransactionRole role;

    /**
     * Instantiates a new Default global transaction.
     */
    DefaultGlobalTransaction() {
        this(null, GlobalStatus.UnKnown, GlobalTransactionRole.Launcher);
    }

    /**
     * Instantiates a new Default global transaction.
     *
     * @param xid the xid
     */
    DefaultGlobalTransaction(String xid, GlobalStatus status, GlobalTransactionRole role) {
        this.transactionManager = DefaultTransactionManager.get();
        this.xid = xid;
        this.status = status;
        this.role = role;
    }

    @Override
    public void begin() throws TransactionException {
        begin(DEFAULT_GLOBAL_TX_TIMEOUT);
    }

    @Override
    public void begin(int timeout) throws TransactionException {
        begin(timeout, DEFAULT_GLOBAL_TX_NAME);
    }

    @Override
    public void begin(int timeout, String name) throws TransactionException {
        if (role != GlobalTransactionRole.Launcher) {
            check();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Begin(): just involved in global transaction [" + xid + "]");
            }
            return;
        }
        if (xid != null) {
            throw new IllegalStateException();
        }
        if (RootContext.getXID() != null) {
            throw new IllegalStateException();
        }
        xid = transactionManager.begin(null, null, name, timeout);
        status = GlobalStatus.Begin;
        RootContext.bind(xid);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Begin a NEW global transaction [" + xid + "]");
        }

    }

    @Override
    public void commit() throws TransactionException {
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of committing
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Commit(): just involved in global transaction [" + xid + "]");
            }
            return;
        }
        if (xid == null) {
            throw new IllegalStateException();
        }

        status = transactionManager.commit(xid);
        if (RootContext.getXID() != null) {
            if (xid.equals(RootContext.getXID())) {
                RootContext.unbind();
            }
        }

    }

    @Override
    public void rollback() throws TransactionException {
        if (role == GlobalTransactionRole.Participant) {
            // Participant has no responsibility of committing
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Ignore Rollback(): just involved in global transaction [" + xid + "]");
            }
            return;
        }
        if (xid == null) {
            throw new IllegalStateException();
        }

        status = transactionManager.rollback(xid);
        if (RootContext.getXID() != null) {
            if (xid.equals(RootContext.getXID())) {
                RootContext.unbind();
            }
        }

    }

    @Override
    public GlobalStatus getStatus() throws TransactionException {
        if (xid == null) {
            return GlobalStatus.UnKnown;
        }
        status = transactionManager.getStatus(xid);
        return status;
    }

    @Override
    public String getXid() {
        return xid;
    }

    private void check() {
        if (xid == null) {
            throw new ShouldNeverHappenException();
        }

    }
}
