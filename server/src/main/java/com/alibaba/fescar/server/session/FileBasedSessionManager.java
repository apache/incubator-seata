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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.service.ConfigurationKeys;
import com.alibaba.fescar.server.store.FileTransactionStoreManager;
import com.alibaba.fescar.server.store.SessionStorable;
import com.alibaba.fescar.server.store.TransactionStoreManager;
import com.alibaba.fescar.server.store.TransactionWriteStore;

/**
 * The type File based session manager.
 */
public class FileBasedSessionManager extends AbstractSessionManager implements Reloadable {

    private static final int READ_SIZE = ConfigurationFactory.getInstance().getInt(
        ConfigurationKeys.SERVICE_SESSION_RELOAD_READ_SIZE, 100);

    /**
     * Instantiates a new File based session manager.
     *
     * @param name                 the name
     * @param sessionStoreFilePath the session store file path
     * @throws IOException the io exception
     */
    public FileBasedSessionManager(String name, String sessionStoreFilePath) throws IOException {
        super(name);
        transactionStoreManager = new FileTransactionStoreManager(sessionStoreFilePath + name, this);
    }

    @Override
    public void reload() {
        restoreSessions();
        washSessions();
    }

    private void restoreSessions() {
        Map<Long, BranchSession> unhandledBranchBuffer = new HashMap<>();

        restoreSessions(true, unhandledBranchBuffer);
        restoreSessions(false, unhandledBranchBuffer);

        if (!unhandledBranchBuffer.isEmpty()) {
            unhandledBranchBuffer.values().forEach(branchSession -> {
                long tid = branchSession.getTransactionId();
                long bid = branchSession.getBranchId();
                GlobalSession found = sessionMap.get(tid);
                if (found == null) {
                    // Ignore
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("GlobalSession Does Not Exists For BranchSession [" + bid + "/" + tid + "]");
                    }
                } else {
                    BranchSession existingBranch = found.getBranch(branchSession.getBranchId());
                    if (existingBranch == null) {
                        found.add(branchSession);
                    } else {
                        existingBranch.setStatus(branchSession.getStatus());
                    }
                }

            });
        }
    }

    private void washSessions() {
        if (sessionMap.size() > 0) {
            Iterator<Map.Entry<Long, GlobalSession>> iterator = sessionMap.entrySet().iterator();
            while (iterator.hasNext()) {
                GlobalSession globalSession = iterator.next().getValue();

                GlobalStatus globalStatus = globalSession.getStatus();
                switch (globalStatus) {
                    case UnKnown:
                    case Committed:
                    case CommitFailed:
                    case Rollbacked:
                    case RollbackFailed:
                    case TimeoutRollbacked:
                    case TimeoutRollbackFailed:
                    case Finished:
                        // Remove all sessions finished
                        iterator.remove();
                        break;
                    default:
                        break;
                }
            }
        }
    }

    private void restoreSessions(boolean isHistory, Map<Long, BranchSession> unhandledBranchBuffer) {
        while (transactionStoreManager.hasRemaining(isHistory)) {
            List<TransactionWriteStore> stores = transactionStoreManager.readWriteStoreFromFile(READ_SIZE, isHistory);
            restore(stores, unhandledBranchBuffer);
        }
    }

    private void restore(List<TransactionWriteStore> stores, Map<Long, BranchSession> unhandledBranchSessions) {
        for (TransactionWriteStore store : stores) {
            TransactionStoreManager.LogOperation logOperation = store.getOperate();
            SessionStorable sessionStorable = store.getSessionRequest();
            switch (logOperation) {
                case GLOBAL_ADD:
                case GLOBAL_UPDATE: {
                    GlobalSession globalSession = (GlobalSession)sessionStorable;
                    long tid = globalSession.getTransactionId();
                    GlobalSession foundGlobalSession = sessionMap.get(tid);
                    if (foundGlobalSession == null) {
                        sessionMap.put(globalSession.getTransactionId(), globalSession);
                    } else {
                        foundGlobalSession.setStatus(globalSession.getStatus());
                    }
                    break;
                }
                case GLOBAL_REMOVE: {
                    GlobalSession globalSession = (GlobalSession)sessionStorable;
                    long tid = globalSession.getTransactionId();
                    if (sessionMap.remove(tid) == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("GlobalSession To Be Removed Does Not Exists [" + tid + "]");
                        }
                    }
                    break;
                }
                case BRANCH_ADD:
                case BRANCH_UPDATE: {
                    BranchSession branchSession = (BranchSession)sessionStorable;
                    long tid = branchSession.getTransactionId();
                    GlobalSession foundGlobalSession = sessionMap.get(tid);
                    if (foundGlobalSession == null) {
                        unhandledBranchSessions.put(branchSession.getBranchId(), branchSession);
                    } else {
                        BranchSession existingBranch = foundGlobalSession.getBranch(branchSession.getBranchId());
                        if (existingBranch == null) {
                            foundGlobalSession.add(branchSession);
                        } else {
                            existingBranch.setStatus(branchSession.getStatus());
                        }
                    }
                    break;
                }
                case BRANCH_REMOVE: {
                    BranchSession branchSession = (BranchSession)sessionStorable;
                    long tid = branchSession.getTransactionId();
                    long bid = branchSession.getBranchId();
                    GlobalSession found = sessionMap.get(tid);
                    if (found == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(
                                "GlobalSession To Be Updated (Remove Branch) Does Not Exists [" + bid + "/" + tid
                                    + "]");
                        }
                    } else {
                        BranchSession theBranch = found.getBranch(bid);
                        if (theBranch == null) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("BranchSession To Be Updated Does Not Exists [" + bid + "/" + tid + "]");
                            }
                        } else {
                            found.remove(theBranch);
                        }
                    }
                    break;
                }

                default:
                    throw new ShouldNeverHappenException("Unknown Operation: " + logOperation);

            }
        }

    }

    private void restore(TransactionWriteStore store) {
    }
}
