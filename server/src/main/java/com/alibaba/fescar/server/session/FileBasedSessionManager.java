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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.config.ConfigurationFactory;
import com.alibaba.fescar.core.exception.TransactionException;
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

    private static int READ_SIZE = ConfigurationFactory.getInstance().getInt(ConfigurationKeys.SERVICE_SESSION_RELOAD_READ_SIZE, 100);

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

    public void reload() {
        restoreSessions();
        washSessions();
    }

    private void restoreSessions() {
        restoreSessions(true);
        restoreSessions(false);
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

    private void restoreSessions(boolean isHistory) {
        while (transactionStoreManager.hasRemaining(isHistory)) {
            List<TransactionWriteStore> stores = transactionStoreManager.readWriteStoreFromFile(READ_SIZE, isHistory);
            restore(stores);
        }
    }

    private void restore(List<TransactionWriteStore> stores) {
        for (TransactionWriteStore store : stores) {
            restore(store);
        }

    }

    private void restore(TransactionWriteStore store) {
        TransactionStoreManager.LogOperation logOperation = store.getOperate();
        SessionStorable sessionStorable = store.getSessionRequest();
        switch (logOperation) {
            case GLOBAL_ADD: {
                GlobalSession globalSession = (GlobalSession)sessionStorable;
                sessionMap.put(globalSession.getTransactionId(), globalSession);
                break;
            }
            case GLOBAL_UPDATE: {
                GlobalSession globalSession = (GlobalSession)sessionStorable;
                long tid = globalSession.getTransactionId();
                GlobalSession found = sessionMap.get(tid);
                if (found == null) {
                    throw new ShouldNeverHappenException("GlobalSession To Be Updated Does Not Exists [" + tid + "]");
                }
                found.setStatus(globalSession.getStatus());
                break;
            }
            case GLOBAL_REMOVE:{
                GlobalSession globalSession = (GlobalSession)sessionStorable;
                long tid = globalSession.getTransactionId();
                if (sessionMap.remove(tid) == null) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("GlobalSession To Be Removed Does Not Exists [" + tid + "]");
                    }
                }
                break;
            }
            case BRANCH_ADD: {
                BranchSession branchSession = (BranchSession)sessionStorable;
                long tid = branchSession.getTransactionId();
                GlobalSession found = sessionMap.get(tid);
                if (found == null) {
                    throw new ShouldNeverHappenException("GlobalSession To Be Updated (Add Branch) Does Not Exists [" + tid + "]");
                }
                found.add(branchSession);
                break;
            }
            case BRANCH_UPDATE: {
                BranchSession branchSession = (BranchSession)sessionStorable;
                long tid = branchSession.getTransactionId();
                long bid = branchSession.getBranchId();
                GlobalSession found = sessionMap.get(tid);
                if (found == null) {
                    throw new ShouldNeverHappenException("GlobalSession To Be Updated (Update Branch) Does Not Exists [" + bid + "/" + tid + "]");
                }
                BranchSession theBranch = found.getBranch(bid);
                if (theBranch == null) {
                    throw new ShouldNeverHappenException("BranchSession To Be Updated Does Not Exists [" + bid + "/" + tid + "]");
                }
                theBranch.setStatus(branchSession.getStatus());
                break;
            }
            case BRANCH_REMOVE: {
                BranchSession branchSession = (BranchSession)sessionStorable;
                long tid = branchSession.getTransactionId();
                long bid = branchSession.getBranchId();
                GlobalSession found = sessionMap.get(tid);
                if (found == null) {
                    throw new ShouldNeverHappenException("GlobalSession To Be Updated (Remove Branch) Does Not Exists [" + bid + "/" + tid + "]");
                }
                BranchSession theBranch = found.getBranch(bid);
                if (theBranch == null) {
                    throw new ShouldNeverHappenException("BranchSession To Be Updated Does Not Exists [" + bid + "/" + tid + "]");
                }
                if (!found.remove(branchSession)) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("BranchSession To Be Removed Does Not Exists [" + bid + "/" + tid + "]");
                    }
                }
                break;
            }

            default:
                throw new ShouldNeverHappenException("Unknown Operation: " + logOperation);

        }
    }
}
