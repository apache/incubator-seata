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

package com.alibaba.fescar.server.coordinator;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.BranchStatus;
import com.alibaba.fescar.core.model.BranchType;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.ResourceManagerInbound;
import com.alibaba.fescar.server.UUIDGenerator;
import com.alibaba.fescar.server.lock.LockManager;
import com.alibaba.fescar.server.lock.LockManagerFactory;
import com.alibaba.fescar.server.session.BranchSession;
import com.alibaba.fescar.server.session.GlobalSession;
import com.alibaba.fescar.server.session.SessionHolder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.alibaba.fescar.core.exception.TransactionExceptionCode.BranchTransactionNotExist;
import static com.alibaba.fescar.core.exception.TransactionExceptionCode.FailedToAddBranch;
import static com.alibaba.fescar.core.exception.TransactionExceptionCode.GlobalTransactionNotActive;
import static com.alibaba.fescar.core.exception.TransactionExceptionCode.GlobalTransactionStatusInvalid;
import static com.alibaba.fescar.core.exception.TransactionExceptionCode.LockKeyConflict;

public class DefaultCore implements Core {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCore.class);

    private LockManager lockManager = LockManagerFactory.get();

    private ResourceManagerInbound resourceManagerInbound;

    @Override
    public void setResourceManagerInbound(ResourceManagerInbound resourceManagerInbound) {
        this.resourceManagerInbound = resourceManagerInbound;
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid, String lockKeys) throws TransactionException {
        GlobalSession globalSession = assertGlobalSession(XID.getTransactionId(xid), GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(XID.getTransactionId(xid));
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setApplicationId(globalSession.getApplicationId());
        branchSession.setTxServiceGroup(globalSession.getTransactionServiceGroup());
        branchSession.setBranchType(branchType);
        branchSession.setResourceId(resourceId);
        branchSession.setLockKey(lockKeys);
        branchSession.setClientId(clientId);

        if (!branchSession.lock()) {
            throw new TransactionException(LockKeyConflict);
        }
        try {
            globalSession.addBranch(branchSession);
        } catch (RuntimeException ex) {
            throw new TransactionException(FailedToAddBranch);

        }
        return branchSession.getBranchId();
    }

    private GlobalSession assertGlobalSession(long transactionId, GlobalStatus status) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(transactionId);
        if (globalSession == null) {
            throw new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist, "" + transactionId + "");
        }
        if (!globalSession.isActive()) {
            throw new TransactionException(GlobalTransactionNotActive, "Current Status: " + globalSession.getStatus());
        }
        if (globalSession.getStatus() != status) {
            throw new TransactionException(GlobalTransactionStatusInvalid, globalSession.getStatus() + " while expecting " + status);
        }
        return globalSession;
    }

    @Override
    public void branchReport(String xid, long branchId, BranchStatus status, String applicationData) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        if (globalSession == null) {
            throw new TransactionException(TransactionExceptionCode.GlobalTransactionNotExist, "" + XID.getTransactionId(xid) + "");
        }
        BranchSession branchSession = globalSession.getBranch(branchId);
        if (branchSession == null) {
            throw new TransactionException(BranchTransactionNotExist);
        }
        globalSession.changeBranchStatus(branchSession, status);
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys) throws TransactionException {
        if (branchType == BranchType.AT) {
            return lockManager.isLockable(XID.getTransactionId(xid), resourceId, lockKeys);
        } else {
            return true;
        }

    }

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession(
                applicationId, transactionServiceGroup, name, timeout);
        session.addSessionLifecycleListener(SessionHolder.getRootSessionManager());

        session.begin();

        return XID.generateXID(session.getTransactionId());
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        GlobalStatus status = globalSession.getStatus();

        globalSession.closeAndClean(); // Highlight: Firstly, close the session, then no more branch can be registered.

        if (status == GlobalStatus.Begin) {
            if (globalSession.canBeCommittedAsync()) {
                asyncCommit(globalSession);
            } else {
                doGlobalCommit(globalSession, false);
            }

        }
        return globalSession.getStatus();
    }

    @Override
    public void doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        for (BranchSession branchSession : globalSession.getSortedBranches()) {
            BranchStatus currentStatus = branchSession.getStatus();
            if (currentStatus == BranchStatus.PhaseOne_Failed) {
                continue;
            }
            try {
                BranchStatus branchStatus = resourceManagerInbound.branchCommit(XID.generateXID(branchSession.getTransactionId()), branchSession.getBranchId(),
                        branchSession.getResourceId(), branchSession.getApplicationData());

                switch (branchStatus) {
                    case PhaseTwo_Committed:
                        globalSession.removeBranch(branchSession);
                        continue;
                    case PhaseTwo_CommitFailed_Unretryable:
                        if (globalSession.canBeCommittedAsync()) {
                            LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                            continue;
                        } else {
                            globalSession.changeStatus(GlobalStatus.CommitFailed);
                            globalSession.end();
                            LOGGER.error("Finally, failed to commit global[{}] since branch[{}] commit failed",
                                globalSession.getTransactionId(), branchSession.getBranchId());
                            return;
                        }
                    default:
                        if (!retrying) {
                            queueToRetryCommit(globalSession);
                            return;
                        }
                        if (globalSession.canBeCommittedAsync()) {
                            LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                            continue;
                        } else {
                            LOGGER.error(
                                "Failed to commit global[{}] since branch[{}] commit failed, will retry later.",
                                globalSession.getTransactionId(), branchSession.getBranchId());
                            return;
                        }

                }

            } catch (Exception ex) {
                LOGGER.info("Exception committing branch {}", branchSession, ex);
                if (!retrying) {
                    queueToRetryCommit(globalSession);
                    if (ex instanceof TransactionException) {
                        throw (TransactionException) ex;
                    } else {
                        throw new TransactionException(ex);
                    }
                }

            }

        }
        if (globalSession.hasBranch()) {
            LOGGER.info("Global[{}] committing is NOT done.", globalSession.getTransactionId());
            return;
        }
        globalSession.changeStatus(GlobalStatus.Committed);
        globalSession.end();
        LOGGER.info("Global[{}] committing is successfully done.", globalSession.getTransactionId());
    }

    private void asyncCommit(GlobalSession globalSession) throws TransactionException {
        globalSession.addSessionLifecycleListener(SessionHolder.getAsyncCommittingSessionManager());
        SessionHolder.getAsyncCommittingSessionManager().addGlobalSession(globalSession);
    }

    private void queueToRetryCommit(GlobalSession globalSession) throws TransactionException {
        globalSession.addSessionLifecycleListener(SessionHolder.getRetryCommittingSessionManager());
        SessionHolder.getRetryCommittingSessionManager().addGlobalSession(globalSession);
        globalSession.changeStatus(GlobalStatus.CommitRetrying);
    }

    private void queueToRetryRollback(GlobalSession globalSession) throws TransactionException {
        globalSession.addSessionLifecycleListener(SessionHolder.getRetryRollbackingSessionManager());
        SessionHolder.getRetryRollbackingSessionManager().addGlobalSession(globalSession);
        GlobalStatus currentStatus = globalSession.getStatus();
        if (currentStatus.name().startsWith("Timeout")) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbackRetrying);
        } else {
            globalSession.changeStatus(GlobalStatus.RollbackRetrying);
        }
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        GlobalStatus status = globalSession.getStatus();

        globalSession.close(); // Highlight: Firstly, close the session, then no more branch can be registered.

        if (status == GlobalStatus.Begin) {
            globalSession.changeStatus(GlobalStatus.Rollbacking);
            doGlobalRollback(globalSession, false);

        }
        return globalSession.getStatus();
    }

    @Override
    public void doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        for (BranchSession branchSession : globalSession.getReverseSortedBranches()) {
            BranchStatus currentBranchStatus = branchSession.getStatus();
            if (currentBranchStatus == BranchStatus.PhaseOne_Failed) {
                continue;
            }
            try {
                BranchStatus branchStatus = resourceManagerInbound.branchRollback(XID.generateXID(branchSession.getTransactionId()), branchSession.getBranchId(),
                    branchSession.getResourceId(), branchSession.getApplicationData());

                switch (branchStatus) {
                    case PhaseTwo_Rollbacked:
                        globalSession.removeBranch(branchSession);
                        LOGGER.error("Successfully rolled back branch " + branchSession);
                        continue;
                    case PhaseTwo_RollbackFailed_Unretryable:
                        changeToRollbackFailedStatus(globalSession);
                        globalSession.end();
                        LOGGER.error("Failed to rollback global[" + globalSession.getTransactionId() + "] since branch[" + branchSession.getBranchId() + "] rollback failed");
                        return;
                    default:
                        LOGGER.info("Failed to rollback branch " + branchSession);
                        if (!retrying) {
                            queueToRetryRollback(globalSession);
                        }
                        return;

                }
            } catch (Exception ex) {
                LOGGER.info("Exception rollbacking branch " + branchSession, ex);
                if (!retrying) {
                    queueToRetryRollback(globalSession);
                    if (ex instanceof TransactionException) {
                        throw (TransactionException) ex;
                    } else {
                        throw new TransactionException(ex);
                    }
                }

            }

        }
        if (globalSession.hasBranch()) {
            changeToRollbackFailedStatus(globalSession);
        } else {
            changeToRollbackedStatus(globalSession);
        }
        globalSession.end();
    }

    private void changeToRollbackedStatus(GlobalSession globalSession) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        if (currentStatus.name().startsWith("Timeout")) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbacked);
        } else {
            globalSession.changeStatus(GlobalStatus.Rollbacked);
        }
    }

    private void changeToRollbackFailedStatus(GlobalSession globalSession) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        if (currentStatus.name().startsWith("Timeout")) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbackFailed);
        } else {
            globalSession.changeStatus(GlobalStatus.RollbackFailed);
        }
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(XID.getTransactionId(xid));
        return globalSession.getStatus();
    }
}
