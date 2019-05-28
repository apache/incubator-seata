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
package io.seata.server.session.file;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import io.seata.common.exception.ShouldNeverHappenException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.loader.LoadLevel;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.StoreMode;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.DefaultSessionManager;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.Reloadable;
import io.seata.server.session.SessionManager;
import io.seata.server.store.ReloadableStore;
import io.seata.server.store.SessionStorable;
import io.seata.server.store.TransactionStoreManager;
import io.seata.server.store.TransactionWriteStore;

/**
 * The type File based session manager.
 *
 * @author jimin.jm @alibaba-inc.com
 */
@LoadLevel(name = "file")
public class FileBasedSessionManager extends DefaultSessionManager implements Reloadable {

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
        transactionStoreManager = EnhancedServiceLoader.load(TransactionStoreManager.class, StoreMode.FILE.name(),
            new Class[] {String.class, SessionManager.class},
            new Object[] {sessionStoreFilePath + File.separator + name, this});
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
                String xid = branchSession.getXid();
                long bid = branchSession.getBranchId();
                GlobalSession found = sessionMap.get(xid);
                if (found == null) {
                    // Ignore
                    if (LOGGER.isInfoEnabled()) {
                        LOGGER.info("GlobalSession Does Not Exists For BranchSession [" + bid + "/" + xid + "]");
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
            Iterator<Map.Entry<String, GlobalSession>> iterator = sessionMap.entrySet().iterator();
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
        if (!(transactionStoreManager instanceof ReloadableStore)) {
            return;
        }
        while (((ReloadableStore)transactionStoreManager).hasRemaining(isHistory)) {
            List<TransactionWriteStore> stores = ((ReloadableStore)transactionStoreManager).readWriteStore(READ_SIZE,
                isHistory);
            restore(stores, unhandledBranchBuffer);
        }
    }

    private void restore(List<TransactionWriteStore> stores, Map<Long, BranchSession> unhandledBranchSessions) {
        long maxRecoverId = UUIDGenerator.getCurrentUUID();
        for (TransactionWriteStore store : stores) {
            TransactionStoreManager.LogOperation logOperation = store.getOperate();
            SessionStorable sessionStorable = store.getSessionRequest();
            maxRecoverId = getMaxId(maxRecoverId, sessionStorable);
            switch (logOperation) {
                case GLOBAL_ADD:
                case GLOBAL_UPDATE: {
                    GlobalSession globalSession = (GlobalSession)sessionStorable;
                    if (globalSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore globalSession from file failed, the transactionId is zero , xid:" + globalSession
                                .getXid());
                        break;
                    }
                    GlobalSession foundGlobalSession = sessionMap.get(globalSession.getXid());
                    if (foundGlobalSession == null) {
                        sessionMap.put(globalSession.getXid(), globalSession);
                    } else {
                        foundGlobalSession.setStatus(globalSession.getStatus());
                    }
                    break;
                }
                case GLOBAL_REMOVE: {
                    GlobalSession globalSession = (GlobalSession)sessionStorable;
                    if (globalSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore globalSession from file failed, the transactionId is zero , xid:" + globalSession
                                .getXid());
                        break;
                    }
                    if (sessionMap.remove(globalSession.getXid()) == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info("GlobalSession To Be Removed Does Not Exists [" + globalSession.getXid() + "]");
                        }
                    }
                    break;
                }
                case BRANCH_ADD:
                case BRANCH_UPDATE: {
                    BranchSession branchSession = (BranchSession)sessionStorable;
                    if (branchSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore branchSession from file failed, the transactionId is zero , xid:" + branchSession
                                .getXid());
                        break;
                    }
                    GlobalSession foundGlobalSession = sessionMap.get(branchSession.getXid());
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
                    String xid = branchSession.getXid();
                    long bid = branchSession.getBranchId();
                    if (branchSession.getTransactionId() == 0) {
                        LOGGER.error(
                            "Restore branchSession from file failed, the transactionId is zero , xid:" + branchSession
                                .getXid());
                        break;
                    }
                    GlobalSession found = sessionMap.get(xid);
                    if (found == null) {
                        if (LOGGER.isInfoEnabled()) {
                            LOGGER.info(
                                "GlobalSession To Be Updated (Remove Branch) Does Not Exists [" + bid + "/" + xid
                                    + "]");
                        }
                    } else {
                        BranchSession theBranch = found.getBranch(bid);
                        if (theBranch == null) {
                            if (LOGGER.isInfoEnabled()) {
                                LOGGER.info("BranchSession To Be Updated Does Not Exists [" + bid + "/" + xid + "]");
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
        setMaxId(maxRecoverId);

    }

    private long getMaxId(long maxRecoverId, SessionStorable sessionStorable) {
        long currentId = 0;
        if (sessionStorable instanceof GlobalSession) {
            currentId = ((GlobalSession)sessionStorable).getTransactionId();
        } else if (sessionStorable instanceof BranchSession) {
            currentId = ((BranchSession)sessionStorable).getBranchId();
        }

        return maxRecoverId > currentId ? maxRecoverId : currentId;
    }

    private void setMaxId(long maxRecoverId) {
        long currentId;
        // will be recover multi-thread later
        while ((currentId = UUIDGenerator.getCurrentUUID()) < maxRecoverId) {
            if (UUIDGenerator.setUUID(currentId, maxRecoverId)) {
                break;
            }
        }
    }

    private void restore(TransactionWriteStore store) {
    }

    @Override
    public void destroy() {
        transactionStoreManager.shutdown();
    }
}
