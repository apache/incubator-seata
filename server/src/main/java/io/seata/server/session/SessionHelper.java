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
package io.seata.server.session;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.event.EventBus;
import io.seata.core.event.GlobalTransactionEvent;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.event.EventBusManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

/**
 * The type Session helper.
 *
 * @author sharajava
 */
public class SessionHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionHelper.class);

    /**
     * The constant CONFIG.
     */
    private static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final Boolean ENABLE_BRANCH_ASYNC_REMOVE = CONFIG.getBoolean(
            ConfigurationKeys.ENABLE_BRANCH_ASYNC_REMOVE, true);

    /**
     * The instance of DefaultCoordinator
     */
    private static final DefaultCoordinator COORDINATOR = DefaultCoordinator.getInstance();


    private SessionHelper() {}

    public static BranchSession newBranchByGlobal(GlobalSession globalSession, BranchType branchType, String resourceId, String lockKeys, String clientId) {
        return newBranchByGlobal(globalSession, branchType, resourceId, null, lockKeys, clientId);
    }

    /**
     * New branch by global branch session.
     *
     * @param globalSession the global session
     * @param branchType    the branch type
     * @param resourceId    the resource id
     * @param lockKeys      the lock keys
     * @param clientId      the client id
     * @return the branch session
     */
    public static BranchSession newBranchByGlobal(GlobalSession globalSession, BranchType branchType, String resourceId,
            String applicationData, String lockKeys, String clientId) {
        BranchSession branchSession = new BranchSession();

        branchSession.setXid(globalSession.getXid());
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setBranchType(branchType);
        branchSession.setResourceId(resourceId);
        branchSession.setLockKey(lockKeys);
        branchSession.setClientId(clientId);
        branchSession.setApplicationData(applicationData);

        return branchSession;
    }

    /**
     * New branch
     *
     * @param branchType      the branch type
     * @param xid             Transaction id.
     * @param branchId        Branch id.
     * @param resourceId      Resource id.
     * @param applicationData Application data bind with this branch.
     * @return the branch session
     */
    public static BranchSession newBranch(BranchType branchType, String xid, long branchId, String resourceId, String applicationData) {
        BranchSession branchSession = new BranchSession();
        branchSession.setXid(xid);
        branchSession.setBranchId(branchId);
        branchSession.setBranchType(branchType);
        branchSession.setResourceId(resourceId);
        branchSession.setApplicationData(applicationData);
        return branchSession;
    }

    /**
     * End committed.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endCommitted(GlobalSession globalSession) throws TransactionException {
        globalSession.changeStatus(GlobalStatus.Committed);
        globalSession.end();
        postTcSessionEndEvent(globalSession);
    }

    /**
     * End commit failed.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endCommitFailed(GlobalSession globalSession) throws TransactionException {
        LOGGER.error("The Global session {} is in failed status, please handle it manually.", globalSession.getXid());

        globalSession.changeStatus(GlobalStatus.CommitFailed);
        LOGGER.error("The Global session {} has changed the status to {}, need to be handled it manually.", globalSession.getXid(), globalSession.getStatus());

        globalSession.end();
        postTcSessionEndEvent(globalSession);
    }

    /**
     * End rollbacked.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endRollbacked(GlobalSession globalSession) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        if (isTimeoutGlobalStatus(currentStatus)) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbacked);
        } else {
            globalSession.changeStatus(GlobalStatus.Rollbacked);
        }
        globalSession.end();
        postTcSessionEndEvent(globalSession);
    }

    /**
     * End rollback failed.
     *
     * @param globalSession the global session
     * @throws TransactionException the transaction exception
     */
    public static void endRollbackFailed(GlobalSession globalSession) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        LOGGER.error("The Global session {} is in failed status, please handle it manually.", globalSession.getXid());

        if (isTimeoutGlobalStatus(currentStatus)) {
            globalSession.changeStatus(GlobalStatus.TimeoutRollbackFailed);
        } else {
            globalSession.changeStatus(GlobalStatus.RollbackFailed);
        }
        LOGGER.error("The Global session {} has changed the status to {}, need to be handled it manually.", globalSession.getXid(), globalSession.getStatus());

        globalSession.end();
        postTcSessionEndEvent(globalSession);
    }

    /**
     * post end event
     *
     * @param globalSession the global session
     */
    public static void postTcSessionEndEvent(GlobalSession globalSession) {
        postTcSessionEndEvent(globalSession, globalSession.getStatus());
    }

    /**
     * post end event (force specified state)
     *
     * @param globalSession the global session
     * @param status the global status
     */
    public static void postTcSessionEndEvent(GlobalSession globalSession, GlobalStatus status) {
        EventBus eventBus = EventBusManager.get();
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
            globalSession.getTransactionName(), globalSession.getApplicationId(),
            globalSession.getTransactionServiceGroup(), globalSession.getBeginTime(), System.currentTimeMillis(),
            status));
    }

    /**
     * post begin event
     *
     * @param globalSession the global session
     */
    public static void postTcSessionEvent(GlobalSession globalSession) {
        LOGGER.info("globalsession status: {},xid: {}",globalSession.getStatus(),globalSession.getXid());
        postTcSessionEvent(globalSession, globalSession.getStatus());
    }

    /**
     * post begin event(force specified state)
     *
     * @param globalSession the global session
     */
    public static void postTcSessionEvent(GlobalSession globalSession, GlobalStatus status) {
        EventBus eventBus = EventBusManager.get();
        eventBus.post(new GlobalTransactionEvent(globalSession.getTransactionId(), GlobalTransactionEvent.ROLE_TC,
            globalSession.getTransactionName(), globalSession.getApplicationId(),
            globalSession.getTransactionServiceGroup(), globalSession.getBeginTime(), null, status));
    }

    public static boolean isTimeoutGlobalStatus(GlobalStatus status) {
        return status == GlobalStatus.TimeoutRollbacked
                || status == GlobalStatus.TimeoutRollbackFailed
                || status == GlobalStatus.TimeoutRollbacking
                || status == GlobalStatus.TimeoutRollbackRetrying;
    }

    /**
     * Foreach global sessions.
     *
     * @param sessions the global sessions
     * @param handler  the handler
     * @since 1.5.0
     */
    public static void forEach(Collection<GlobalSession> sessions, GlobalSessionHandler handler) {
        sessions.parallelStream().forEach(globalSession -> {
            try {
                MDC.put(RootContext.MDC_KEY_XID, globalSession.getXid());
                handler.handle(globalSession);
            } catch (Throwable th) {
                LOGGER.error("handle global session failed: {}", globalSession.getXid(), th);
            } finally {
                MDC.remove(RootContext.MDC_KEY_XID);
            }
        });
    }

    /**
     * Foreach branch sessions.
     *
     * @param sessions the branch session
     * @param handler  the handler
     * @since 1.5.0
     */
    public static Boolean forEach(Collection<BranchSession> sessions, BranchSessionHandler handler) throws TransactionException {
        Boolean result;
        for (BranchSession branchSession : sessions) {
            try {
                MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(branchSession.getBranchId()));
                result = handler.handle(branchSession);
                if (result == null) {
                    continue;
                }
                return result;
            } finally {
                MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
            }
        }
        return null;
    }


    /**
     * remove branchSession from globalSession
     * @param globalSession the globalSession
     * @param branchSession the branchSession
     * @param isAsync if asynchronous remove
     */
    public static void removeBranch(GlobalSession globalSession, BranchSession branchSession, boolean isAsync)
            throws TransactionException {
        if (Objects.equals(Boolean.TRUE, ENABLE_BRANCH_ASYNC_REMOVE) && isAsync) {
            COORDINATOR.doBranchRemoveAsync(globalSession, branchSession);
        } else {
            globalSession.removeBranch(branchSession);
        }
    }

    /**
     * remove branchSession from globalSession
     * @param globalSession the globalSession
     * @param isAsync if asynchronous remove
     */
    public static void removeAllBranch(GlobalSession globalSession, boolean isAsync)
            throws TransactionException {
        List<BranchSession> branchSessions = globalSession.getSortedBranches();
        if (branchSessions == null || branchSessions.isEmpty()) {
            return;
        }
        if (Objects.equals(Boolean.TRUE, ENABLE_BRANCH_ASYNC_REMOVE) && isAsync) {
            COORDINATOR.doBranchRemoveAllAsync(globalSession);
        } else {
            for (BranchSession branchSession : branchSessions) {
                globalSession.removeBranch(branchSession);
            }
        }
    }
}
