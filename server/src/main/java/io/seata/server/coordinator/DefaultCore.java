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
package io.seata.server.coordinator;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.common.exception.NotSupportYetException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.CollectionUtils;
import io.seata.core.event.EventBus;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.rpc.ServerMessageSender;
import io.seata.server.event.EventBusManager;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHelper;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Default core.
 *
 * @author sharajava
 */
public class DefaultCore implements Core {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCore.class);

    private EventBus eventBus = EventBusManager.get();

    private static Map<BranchType, AbstractCore> coreMap = new ConcurrentHashMap<>();

    /**
     * get the Default core.
     *
     * @param messageSender the message sender
     */
    public DefaultCore(ServerMessageSender messageSender) {
        List<AbstractCore> allCore = EnhancedServiceLoader.loadAll(AbstractCore.class,
                new Class[] {ServerMessageSender.class}, new Object[] {messageSender});
        if (CollectionUtils.isNotEmpty(allCore)) {
            for (AbstractCore core : allCore) {
                coreMap.put(core.getHandleBranchType(), core);
            }
        }
    }

    /**
     * get core
     *
     * @param branchType the branchType
     * @return the core
     */
    public AbstractCore getCore(BranchType branchType) {
        AbstractCore core = coreMap.get(branchType);
        if (core == null) {
            throw new NotSupportYetException("unsupported type:" + branchType.name());
        }
        return core;
    }

    /**
     * only for mock
     *
     * @param branchType the branchType
     * @param core the core
     */
    public void mockCore(BranchType branchType, AbstractCore core) {
        coreMap.put(branchType, core);
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                               String applicationData, String lockKeys) throws TransactionException {
        return getCore(branchType).branchRegister(branchType, resourceId, clientId, xid,
                applicationData, lockKeys);
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        getCore(branchType).branchReport(branchType, xid, branchId, status, applicationData);
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
            throws TransactionException {
        return getCore(branchType).lockQuery(branchType, resourceId, xid, lockKeys);
    }

    @Override
    public BranchStatus branchCommit(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        return getCore(branchSession.getBranchType()).branchCommit(globalSession, branchSession);
    }

    @Override
    public BranchStatus branchRollback(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        return getCore(branchSession.getBranchType()).branchRollback(globalSession, branchSession);
    }

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
            throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession(applicationId, transactionServiceGroup, name,
                timeout);
        session.addSessionLifecycleListener(SessionHolder.getRootSessionManager());

        session.begin();

        // transaction start event
        eventBus.post(new GlobalTransactionEvent(session.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                session.getTransactionName(), session.getBeginTime(), null, session.getStatus()));

        LOGGER.info("Successfully begin global transaction xid = {}", session.getXid());
        return session.getXid();
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        // just lock changeStatus

        boolean shouldCommit = globalSession.lockAndExecute(() -> {
            // the lock should release after branch commit
            // Highlight: Firstly, close the session, then no more branch can be registered.
            globalSession.closeAndClean();
            if (globalSession.getStatus() == GlobalStatus.Begin) {
                globalSession.changeStatus(GlobalStatus.Committing);
                return true;
            }
            return false;
        });
        if (!shouldCommit) {
            return globalSession.getStatus();
        }
        if (globalSession.canBeCommittedAsync()) {
            globalSession.asyncCommit();
            return GlobalStatus.Committed;
        } else {
            doGlobalCommit(globalSession, false);
        }
        return globalSession.getStatus();
    }

    @Override
    public boolean doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        boolean success = true;
        // start committing event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), null, globalSession.getStatus()));

        if (globalSession.isSaga()) {
            success = getCore(BranchType.SAGA).doGlobalCommit(globalSession, retrying);
        } else {
            for (BranchSession branchSession : globalSession.getSortedBranches()) {
                BranchStatus currentStatus = branchSession.getStatus();
                if (currentStatus == BranchStatus.PhaseOne_Failed) {
                    globalSession.removeBranch(branchSession);
                    continue;
                }
                try {
                    BranchStatus branchStatus = getCore(branchSession.getBranchType()).branchCommit(globalSession, branchSession);

                    switch (branchStatus) {
                        case PhaseTwo_Committed:
                            globalSession.removeBranch(branchSession);
                            continue;
                        case PhaseTwo_CommitFailed_Unretryable:
                            if (globalSession.canBeCommittedAsync()) {
                                LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                                continue;
                            } else {
                                SessionHelper.endCommitFailed(globalSession);
                                LOGGER.error("Finally, failed to commit global[{}] since branch[{}] commit failed",
                                        globalSession.getXid(), branchSession.getBranchId());
                                return false;
                            }
                        default:
                            if (!retrying) {
                                globalSession.queueToRetryCommit();
                                return false;
                            }
                            if (globalSession.canBeCommittedAsync()) {
                                LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                                continue;
                            } else {
                                LOGGER.error(
                                        "Failed to commit global[{}] since branch[{}] commit failed, will retry later.",
                                        globalSession.getXid(), branchSession.getBranchId());
                                return false;
                            }
                    }
                } catch (Exception ex) {
                    LOGGER.error("Exception committing branch {}", branchSession, ex);
                    if (!retrying) {
                        globalSession.queueToRetryCommit();
                        throw new TransactionException(ex);
                    }
                }
            }
            if (globalSession.hasBranch()) {
                LOGGER.info("Global[{}] committing is NOT done.", globalSession.getXid());
                return false;
            }
        }
        if (success) {
            SessionHelper.endCommitted(globalSession);

            // committed event
            eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                    globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                    globalSession.getStatus()));

            LOGGER.info("Global[{}] committing is successfully done.", globalSession.getXid());
        }
        return success;
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        // just lock changeStatus
        boolean shouldRollBack = globalSession.lockAndExecute(() -> {
            globalSession.close(); // Highlight: Firstly, close the session, then no more branch can be registered.
            if (globalSession.getStatus() == GlobalStatus.Begin) {
                globalSession.changeStatus(GlobalStatus.Rollbacking);
                return true;
            }
            return false;
        });
        if (!shouldRollBack) {
            return globalSession.getStatus();
        }

        doGlobalRollback(globalSession, false);
        return globalSession.getStatus();
    }

    @Override
    public boolean doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        boolean success = true;
        // start rollback event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), null, globalSession.getStatus()));

        if (globalSession.isSaga()) {
            success = getCore(BranchType.SAGA).doGlobalRollback(globalSession, retrying);
        } else {
            for (BranchSession branchSession : globalSession.getReverseSortedBranches()) {
                BranchStatus currentBranchStatus = branchSession.getStatus();
                if (currentBranchStatus == BranchStatus.PhaseOne_Failed) {
                    globalSession.removeBranch(branchSession);
                    continue;
                }
                try {
                    BranchStatus branchStatus = branchRollback(globalSession, branchSession);
                    switch (branchStatus) {
                        case PhaseTwo_Rollbacked:
                            globalSession.removeBranch(branchSession);
                            LOGGER.info("Successfully rollback branch xid={} branchId={}", globalSession.getXid(),
                                    branchSession.getBranchId());
                            continue;
                        case PhaseTwo_RollbackFailed_Unretryable:
                            SessionHelper.endRollbackFailed(globalSession);
                            LOGGER.info("Failed to rollback branch and stop retry xid={} branchId={}",
                                    globalSession.getXid(), branchSession.getBranchId());
                            return false;
                        default:
                            LOGGER.info("Failed to rollback branch xid={} branchId={}", globalSession.getXid(),
                                    branchSession.getBranchId());
                            if (!retrying) {
                                globalSession.queueToRetryRollback();
                            }
                            return false;
                    }
                } catch (Exception ex) {
                    LOGGER.error("Exception rollbacking branch xid={} branchId={}", globalSession.getXid(),
                            branchSession.getBranchId(), ex);
                    if (!retrying) {
                        globalSession.queueToRetryRollback();
                    }
                    throw new TransactionException(ex);
                }
            }

            // In db mode, there is a problem of inconsistent data in multiple copies, resulting in new branch
            // transaction registration when rolling back.
            // 1. New branch transaction and rollback branch transaction have no data association
            // 2. New branch transaction has data association with rollback branch transaction
            // The second query can solve the first problem, and if it is the second problem, it may cause a rollback
            // failure due to data changes.
            GlobalSession globalSessionTwice = SessionHolder.findGlobalSession(globalSession.getXid());
            if (globalSessionTwice != null && globalSessionTwice.hasBranch()) {
                LOGGER.info("Global[{}] rollbacking is NOT done.", globalSession.getXid());
                return false;
            }
        }
        if (success) {
            SessionHelper.endRollbacked(globalSession);

            // rollbacked event
            eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                    globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                    globalSession.getStatus()));

            LOGGER.info("Successfully rollback global, xid = {}", globalSession.getXid());
        }
        return success;
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid, false);
        if (null == globalSession) {
            return GlobalStatus.Finished;
        } else {
            return globalSession.getStatus();
        }
    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return globalStatus;
        }
        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        doGlobalReport(globalSession, xid, globalStatus);
        return globalSession.getStatus();
    }

    @Override
    public void doGlobalReport(GlobalSession globalSession, String xid, GlobalStatus globalStatus) throws TransactionException {
        if (globalSession.isSaga()) {
            getCore(BranchType.SAGA).doGlobalReport(globalSession, xid, globalStatus);
        }
    }
}
