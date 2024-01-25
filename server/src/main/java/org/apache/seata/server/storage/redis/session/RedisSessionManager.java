/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.storage.redis.session;

import java.util.Collection;
import java.util.List;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.loader.Scope;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.AbstractSessionManager;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
@LoadLevel(name = "redis", scope = Scope.PROTOTYPE)
public class RedisSessionManager extends AbstractSessionManager
    implements Initialize {
    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(RedisSessionManager.class);
    

    /**
     * Instantiates a new Data base session manager.
     */
    public RedisSessionManager() {
        super();
    }
    

    @Override
    public void init() {
        transactionStoreManager = RedisTransactionStoreManagerFactory.getInstance();
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        boolean ret = transactionStoreManager.writeSession(LogOperation.GLOBAL_ADD, session);
        if (!ret) {
            throw new StoreException("addGlobalSession failed.");
        }
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {
        session.setStatus(status);
        boolean ret = transactionStoreManager.writeSession(LogOperation.GLOBAL_UPDATE, session);
        if (!ret) {
            throw new StoreException("updateGlobalSessionStatus failed.");
        }
    }

    /**
     * remove globalSession 1. rootSessionManager remove normal globalSession 2. retryCommitSessionManager and
     * retryRollbackSessionManager remove retry expired globalSession
     *
     * @param session
     *            the session
     * @throws TransactionException
     */
    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        boolean ret = transactionStoreManager.writeSession(LogOperation.GLOBAL_REMOVE, session);
        if (!ret) {
            throw new StoreException("removeGlobalSession failed.");
        }
    }

    @Override
    public void addBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException {
        boolean ret = transactionStoreManager.writeSession(LogOperation.BRANCH_ADD, session);
        if (!ret) {
            throw new StoreException("addBranchSession failed.");
        }
    }

    @Override
    public void updateBranchSessionStatus(BranchSession session, BranchStatus status) throws TransactionException {
        boolean ret = transactionStoreManager.writeSession(LogOperation.BRANCH_UPDATE, session);
        if (!ret) {
            throw new StoreException("updateBranchSessionStatus failed.");
        }
    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession session) throws TransactionException {
        boolean ret = transactionStoreManager.writeSession(LogOperation.BRANCH_REMOVE, session);
        if (!ret) {
            throw new StoreException("removeBranchSession failed.");
        }
    }

    @Override
    public GlobalSession findGlobalSession(String xid) {
        return this.findGlobalSession(xid, true);
    }

    @Override
    public GlobalSession findGlobalSession(String xid, boolean withBranchSessions) {
        return transactionStoreManager.readSession(xid, withBranchSessions);
    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return findGlobalSessions(
            new SessionCondition(GlobalStatus.UnKnown, GlobalStatus.Begin, GlobalStatus.Committing,
                GlobalStatus.CommitRetrying, GlobalStatus.Rollbacking, GlobalStatus.RollbackRetrying,
                GlobalStatus.TimeoutRollbacking, GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.AsyncCommitting));
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        // nothing need to do
        return transactionStoreManager.readSession(condition);
    }

    @Override
    public <T> T lockAndExecute(GlobalSession globalSession, GlobalSession.LockCallable<T> lockCallable)
        throws TransactionException {
        return lockCallable.call();
    }
}
