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
package com.alibaba.fescar.server.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.server.store.TransactionStoreManager;
import com.alibaba.fescar.server.store.TransactionStoreManager.LogOperation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Abstract session manager.
 */
public abstract class AbstractSessionManager implements SessionManager, SessionLifecycleListener {

    /**
     * The constant LOGGER.
     */
    protected static final Logger LOGGER = LoggerFactory.getLogger(AbstractSessionManager.class);

    /**
     * The Session map.
     */
    protected Map<Long, GlobalSession> sessionMap = new ConcurrentHashMap<>();

    /**
     * The Transaction store manager.
     */
    protected TransactionStoreManager transactionStoreManager;

    /**
     * The Name.
     */
    protected String name;

    /**
     * Instantiates a new Abstract session manager.
     *
     * @param name the name
     */
    public AbstractSessionManager(String name) {
        this.name = name;
    }

    @Override
    public void addGlobalSession(GlobalSession session) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + session + "] " + LogOperation.GLOBAL_ADD);
        }
        transactionStoreManager.writeSession(LogOperation.GLOBAL_ADD, session);
        sessionMap.put(session.getTransactionId(), session);

    }

    @Override
    public GlobalSession findGlobalSession(Long transactionId) throws TransactionException {
        return sessionMap.get(transactionId);
    }

    @Override
    public void updateGlobalSessionStatus(GlobalSession session, GlobalStatus status) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + session + "] " + LogOperation.GLOBAL_UPDATE);
        }
        transactionStoreManager.writeSession(LogOperation.GLOBAL_UPDATE, session);
    }

    @Override
    public void removeGlobalSession(GlobalSession session) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + session + "] " + LogOperation.GLOBAL_REMOVE);
        }
        transactionStoreManager.writeSession(LogOperation.GLOBAL_REMOVE, session);
        sessionMap.remove(session.getTransactionId());

    }

    @Override
    public void addBranchSession(GlobalSession session, BranchSession branchSession) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + branchSession + "] " + LogOperation.BRANCH_ADD);
        }
        transactionStoreManager.writeSession(LogOperation.BRANCH_ADD, branchSession);
    }

    @Override
    public void updateBranchSessionStatus(BranchSession branchSession, BranchStatus status)
        throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + branchSession + "] " + LogOperation.GLOBAL_ADD);
        }
        transactionStoreManager.writeSession(LogOperation.BRANCH_UPDATE, branchSession);

    }

    @Override
    public void removeBranchSession(GlobalSession globalSession, BranchSession branchSession)
        throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("MANAGER[" + name + "] SESSION[" + branchSession + "] " + LogOperation.GLOBAL_ADD);
        }
        transactionStoreManager.writeSession(LogOperation.BRANCH_REMOVE, branchSession);

    }

    @Override
    public Collection<GlobalSession> allSessions() {
        return sessionMap.values();
    }

    @Override
    public List<GlobalSession> findGlobalSessions(SessionCondition condition) {
        List<GlobalSession> found = new ArrayList<>();
        for (GlobalSession globalSession : sessionMap.values()) {
            if (globalSession.getStatus() == condition.getStatus()) {
                if (System.currentTimeMillis() - globalSession.getBeginTime() > condition.getOverTimeAliveMills()) {
                    found.add(globalSession);
                }
            }
        }
        return found;
    }

    @Override
    public void onBegin(GlobalSession globalSession) throws TransactionException {
        addGlobalSession(globalSession);
    }

    @Override
    public void onStatusChange(GlobalSession globalSession, GlobalStatus status) throws TransactionException {
        updateGlobalSessionStatus(globalSession, status);
    }

    @Override
    public void onBranchStatusChange(GlobalSession globalSession, BranchSession branchSession, BranchStatus status)
        throws TransactionException {
        updateBranchSessionStatus(branchSession, status);
    }

    @Override
    public void onAddBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        addBranchSession(globalSession, branchSession);
    }

    @Override
    public void onRemoveBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        removeBranchSession(globalSession, branchSession);
    }

    @Override
    public void onClose(GlobalSession globalSession) throws TransactionException {
        globalSession.setActive(false);

    }

    @Override
    public void onEnd(GlobalSession globalSession) throws TransactionException {
        removeGlobalSession(globalSession);

    }
}
