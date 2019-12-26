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

import io.seata.common.exception.FrameworkException;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The type Default core.
 *
 * @author sharajava
 */
public class DefaultCore1 implements Core1 {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCore1.class);

    private ServerMessageSender messageSender;

    private EventBus eventBus = EventBusManager.get();

    private static volatile Map<BranchType, Core1> resourceManagers = new ConcurrentHashMap<>();

    private static final DefaultCore1 instance = new DefaultCore1();

    public static DefaultCore1 getInstance() {
        return instance;
    }

    /**
     * set the message sender
     *
     * @param messageSender the message sender
     */
    public void setMessageSender(ServerMessageSender messageSender) {
        this.messageSender = messageSender;
    }

    public Core1 getResourceManager(BranchType branchType) {
        if (messageSender == null) {
            throw new FrameworkException("No ResourceManager for BranchType:" + branchType.name());
        }
        if (resourceManagers == null) {
            synchronized (DefaultCore1.class) {
                if (resourceManagers == null) {
                    //init all resource managers
                    List<Core1> allResourceManagers = EnhancedServiceLoader.loadAll(Core1.class, new Class[] {ServerMessageSender.class}, new Object[] {messageSender});
                    if (CollectionUtils.isNotEmpty(allResourceManagers)) {
                        for (Core1 rm : allResourceManagers) {
                            resourceManagers.put(rm.getBranchType(), rm);
                        }
                    }
                }
            }
        }
        Core1 rm = resourceManagers.get(branchType);
        if (rm == null) {
            throw new FrameworkException("No ResourceManager for BranchType:" + branchType.name());
        }
        return rm;
    }

    @Override
    public BranchType getBranchType() {
        throw new FrameworkException("DefaultCore1 isn't a real Core");
    }

    @Override
    public Long branchRegister(BranchType branchType, String resourceId, String clientId, String xid,
                               String applicationData, String lockKeys) throws TransactionException {
        return getResourceManager(branchType).branchRegister(branchType, resourceId, clientId, xid,
                applicationData, lockKeys);
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        getResourceManager(branchType).branchReport(branchType, xid, branchId, status, applicationData);
    }

    @Override
    public boolean lockQuery(BranchType branchType, String resourceId, String xid, String lockKeys)
        throws TransactionException {
        return getResourceManager(branchType).lockQuery(branchType, resourceId, xid, lockKeys);
    }

    @Override
    public BranchStatus branchCommit(BranchType branchType, String xid, long branchId, String resourceId,
                                     String applicationData) throws TransactionException {
        return getResourceManager(branchType).branchCommit(branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public BranchStatus branchRollback(BranchType branchType, String xid, long branchId, String resourceId,
                                       String applicationData) throws TransactionException {
        return getResourceManager(branchType).branchRollback(branchType, xid, branchId, resourceId, applicationData);
    }

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
        throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession(applicationId, transactionServiceGroup, name,
            timeout);
        session.addSessionLifecycleListener(SessionHolder.getRootSessionManager());

        session.begin();

        //transaction start event
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
        boolean shouldCommit = globalSession.lockAndExcute(() -> {
            //the lock should release after branch commit
            globalSession
                .closeAndClean(); // Highlight: Firstly, close the session, then no more branch can be registered.
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
    public void doGlobalCommit(GlobalSession globalSession, boolean retrying) throws TransactionException {
        // start committing event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                globalSession.getTransactionName(), globalSession.getBeginTime(), null, globalSession.getStatus()));

        if (isSaga(globalSession)) {
            getResourceManager(BranchType.SAGA).doGlobalCommit(globalSession, retrying);
        } else {
            for (BranchSession branchSession : globalSession.getSortedBranches()) {
                BranchStatus currentStatus = branchSession.getStatus();
                if (currentStatus == BranchStatus.PhaseOne_Failed) {
                    globalSession.removeBranch(branchSession);
                    continue;
                }
                try {
                    BranchStatus branchStatus = getResourceManager(branchSession.getBranchType()).branchCommit(branchSession.getBranchType(),
                        branchSession.getXid(), branchSession.getBranchId(), branchSession.getResourceId(),
                        branchSession.getApplicationData());

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
                                return;
                            }
                        default:
                            if (!retrying) {
                                globalSession.queueToRetryCommit();
                                return;
                            }
                            if (globalSession.canBeCommittedAsync()) {
                                LOGGER.error("By [{}], failed to commit branch {}", branchStatus, branchSession);
                                continue;
                            } else {
                                LOGGER.error(
                                    "Failed to commit global[{}] since branch[{}] commit failed, will retry later.",
                                    globalSession.getXid(), branchSession.getBranchId());
                                return;
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
                return;
            }
            SessionHelper.endCommitted(globalSession);

            //committed event
            eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                    globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                    globalSession.getStatus()));

            LOGGER.info("Global[{}] committing is successfully done.", globalSession.getXid());
        }
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (globalSession == null) {
            return GlobalStatus.Finished;
        }
        globalSession.addSessionLifecycleListener(SessionHolder.getRootSessionManager());
        // just lock changeStatus
        boolean shouldRollBack = globalSession.lockAndExcute(() -> {
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
    public void doGlobalRollback(GlobalSession globalSession, boolean retrying) throws TransactionException {
        //start rollback event
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
            globalSession.getTransactionName(), globalSession.getBeginTime(), null, globalSession.getStatus()));

        if (isSaga(globalSession)) {
            getResourceManager(BranchType.SAGA).doGlobalRollback(globalSession, retrying);
        } else {
            for (BranchSession branchSession : globalSession.getReverseSortedBranches()) {
                BranchStatus currentBranchStatus = branchSession.getStatus();
                if (currentBranchStatus == BranchStatus.PhaseOne_Failed) {
                    globalSession.removeBranch(branchSession);
                    continue;
                }
                try {
                    BranchStatus branchStatus = branchRollback(branchSession.getBranchType(),
                            branchSession.getXid(), branchSession.getBranchId(), branchSession.getResourceId(),
                            branchSession.getApplicationData());

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
                            return;
                        default:
                            LOGGER.info("Failed to rollback branch xid={} branchId={}", globalSession.getXid(),
                                    branchSession.getBranchId());
                            if (!retrying) {
                                globalSession.queueToRetryRollback();
                            }
                            return;

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

            //In db mode, there is a problem of inconsistent data in multiple copies, resulting in new branch
            // transaction registration when rolling back.
            //1. New branch transaction and rollback branch transaction have no data association
            //2. New branch transaction has data association with rollback branch transaction
            //The second query can solve the first problem, and if it is the second problem, it may cause a rollback
            // failure due to data changes.
            GlobalSession globalSessionTwice = SessionHolder.findGlobalSession(globalSession.getXid());
            if (globalSessionTwice != null && globalSessionTwice.hasBranch()) {
                LOGGER.info("Global[{}] rollbacking is NOT done.", globalSession.getXid());
                return;
            }

            SessionHelper.endRollbacked(globalSession);

            //rollbacked event
            eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
                    globalSession.getTransactionName(), globalSession.getBeginTime(), System.currentTimeMillis(),
                    globalSession.getStatus()));

            LOGGER.info("Successfully rollback global, xid = {}", globalSession.getXid());
        }
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
        if (isSaga(globalSession)) {
            getResourceManager(BranchType.SAGA).doGlobalReport(globalSession, xid, globalStatus);
        }
    }

    /**
     * Is saga type transaction
     *
     * @param globalSession
     * @return
     */
    private boolean isSaga(GlobalSession globalSession) {
        ArrayList<BranchSession> branchSessions = globalSession.getSortedBranches();
        if (branchSessions != null && branchSessions.size() > 0) {
            return BranchType.SAGA.equals(branchSessions.get(0).getBranchType());
        }
        return false;
    }
}
