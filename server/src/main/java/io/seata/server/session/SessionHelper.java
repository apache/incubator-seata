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

import io.seata.common.util.CollectionUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.context.RootContext;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.metrics.IdConstants;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.metrics.MetricsPublisher;
import io.seata.server.store.StoreConfig;
import io.seata.server.store.StoreConfig.SessionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static io.seata.common.DefaultValues.DEFAULT_ENABLE_BRANCH_ASYNC_REMOVE;

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
            ConfigurationKeys.ENABLE_BRANCH_ASYNC_REMOVE, DEFAULT_ENABLE_BRANCH_ASYNC_REMOVE);

    /**
     * The instance of DefaultCoordinator
     */
    private static final DefaultCoordinator COORDINATOR = DefaultCoordinator.getInstance();

    private static final boolean DELAY_HANDLE_SESSION = StoreConfig.getSessionMode() != SessionMode.FILE;

    private SessionHelper() {
    }

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
     * @param retryGlobal   the retry global
     * @throws TransactionException the transaction exception
     */
    public static void endCommitted(GlobalSession globalSession, boolean retryGlobal) throws TransactionException {
        if (retryGlobal || !DELAY_HANDLE_SESSION) {
            long beginTime = System.currentTimeMillis();
            boolean retryBranch = globalSession.getStatus() == GlobalStatus.CommitRetrying;
            globalSession.changeGlobalStatus(GlobalStatus.Committed);
            globalSession.end();
            if (!DELAY_HANDLE_SESSION) {
                MetricsPublisher.postSessionDoneEvent(globalSession, retryGlobal, false);
            }
            MetricsPublisher.postSessionDoneEvent(globalSession, IdConstants.STATUS_VALUE_AFTER_COMMITTED_KEY, true,
                beginTime, retryBranch);
        } else {
            if (globalSession.isSaga()) {
                globalSession.setStatus(GlobalStatus.Committed);
                globalSession.end();
            }
            MetricsPublisher.postSessionDoneEvent(globalSession, false, false);
        }
    }

    /**
     * End commit failed.
     *
     * @param globalSession the global session
     * @param retryGlobal   the retry global
     * @throws TransactionException the transaction exception
     */
    public static void endCommitFailed(GlobalSession globalSession, boolean retryGlobal) throws TransactionException {
        endCommitFailed(globalSession, retryGlobal, false);
    }

    /**
     * End commit failed.
     *
     * @param globalSession  the global session
     * @param retryGlobal    the retry global
     * @param isRetryTimeout is retry timeout
     * @throws TransactionException the transaction exception
     */
    public static void endCommitFailed(GlobalSession globalSession, boolean retryGlobal, boolean isRetryTimeout)
        throws TransactionException {
        if (isRetryTimeout) {
            globalSession.changeGlobalStatus(GlobalStatus.CommitRetryTimeout);
        } else {
            globalSession.changeGlobalStatus(GlobalStatus.CommitFailed);
        }
        LOGGER.error("The Global session {} has changed the status to {}, need to be handled it manually.",
            globalSession.getXid(), globalSession.getStatus());

        globalSession.end();
        MetricsPublisher.postSessionDoneEvent(globalSession, retryGlobal, false);
    }

    /**
     * End rollbacked.
     *
     * @param globalSession the global session
     * @param retryGlobal   the retry global
     * @throws TransactionException the transaction exception
     */
    public static void endRollbacked(GlobalSession globalSession, boolean retryGlobal) throws TransactionException {
        if (retryGlobal || !DELAY_HANDLE_SESSION) {
            long beginTime = System.currentTimeMillis();
            boolean timeoutDone = false;
            GlobalStatus currentStatus = globalSession.getStatus();
            if (currentStatus == GlobalStatus.TimeoutRollbacking) {
                MetricsPublisher.postSessionDoneEvent(globalSession, GlobalStatus.TimeoutRollbacked, false, false);
                timeoutDone = true;
            }
            boolean retryBranch =
                    currentStatus == GlobalStatus.TimeoutRollbackRetrying || currentStatus == GlobalStatus.RollbackRetrying;
            if (!currentStatus.equals(GlobalStatus.TimeoutRollbacked)
                && SessionStatusValidator.isTimeoutRollbacking(currentStatus)) {
                globalSession.changeGlobalStatus(GlobalStatus.TimeoutRollbacked);
            } else {
                globalSession.changeGlobalStatus(GlobalStatus.Rollbacked);
            }
            globalSession.end();
            if (!DELAY_HANDLE_SESSION && !timeoutDone) {
                MetricsPublisher.postSessionDoneEvent(globalSession, retryGlobal, false);
            }
            MetricsPublisher.postSessionDoneEvent(globalSession, IdConstants.STATUS_VALUE_AFTER_ROLLBACKED_KEY, true,
                    beginTime, retryBranch);
        } else {
            if (globalSession.isSaga()) {
                globalSession.setStatus(GlobalStatus.Rollbacked);
                globalSession.end();
            }
            MetricsPublisher.postSessionDoneEvent(globalSession, GlobalStatus.Rollbacked, false, false);
        }
    }

    /**
     * End rollback failed.
     *
     * @param globalSession the global session
     * @param retryGlobal   the retry global
     * @throws TransactionException the transaction exception
     */
    public static void endRollbackFailed(GlobalSession globalSession, boolean retryGlobal) throws TransactionException {
        endRollbackFailed(globalSession, retryGlobal, false);
    }

    /**
     * End rollback failed.
     *
     * @param globalSession the global session
     * @param retryGlobal   the retry global
     * @param isRetryTimeout   is retry timeout
     * @throws TransactionException the transaction exception
     */
    public static void endRollbackFailed(GlobalSession globalSession, boolean retryGlobal, boolean isRetryTimeout) throws TransactionException {
        GlobalStatus currentStatus = globalSession.getStatus();
        if (isRetryTimeout) {
            globalSession.changeGlobalStatus(GlobalStatus.RollbackRetryTimeout);
        } else if (SessionStatusValidator.isTimeoutRollbacking(currentStatus)) {
            globalSession.changeGlobalStatus(GlobalStatus.TimeoutRollbackFailed);
        } else {
            globalSession.changeGlobalStatus(GlobalStatus.RollbackFailed);
        }
        LOGGER.error("The Global session {} has changed the status to {}, need to be handled it manually.", globalSession.getXid(), globalSession.getStatus());
        globalSession.end();
        MetricsPublisher.postSessionDoneEvent(globalSession, retryGlobal, false);
    }

    /**
     * Foreach global sessions.
     *
     * @param sessions the global sessions
     * @param handler  the handler
     * @since 1.5.0
     */
    public static void forEach(Collection<GlobalSession> sessions, GlobalSessionHandler handler) {
        if (CollectionUtils.isEmpty(sessions)) {
            return;
        }
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
        globalSession.unlockBranch(branchSession);
        if (isEnableBranchRemoveAsync() && isAsync) {
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
        boolean isAsyncRemove = isEnableBranchRemoveAsync() && isAsync;
        for (BranchSession branchSession : branchSessions) {
            if (isAsyncRemove) {
                globalSession.unlockBranch(branchSession);
            } else {
                globalSession.removeAndUnlockBranch(branchSession);
            }
        }
        if (isAsyncRemove) {
            COORDINATOR.doBranchRemoveAllAsync(globalSession);
        }
    }

    /**
     * if true, enable delete the branch asynchronously
     *
     * @return the boolean
     */
    private static boolean isEnableBranchRemoveAsync() {
        return Objects.equals(Boolean.TRUE, DELAY_HANDLE_SESSION)
                && Objects.equals(Boolean.TRUE, ENABLE_BRANCH_ASYNC_REMOVE);
    }
}
