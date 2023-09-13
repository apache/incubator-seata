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
package io.seata.server.console.impl;

import io.seata.common.exception.FrameworkException;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.lock.LockManager;
import io.seata.server.lock.LockerManagerFactory;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public abstract class AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

    protected final LockManager lockManager = LockerManagerFactory.getLockManager();

    protected static final List<GlobalStatus> RETRY_COMMIT_STATUS = Arrays.asList(GlobalStatus.CommitRetrying,
            GlobalStatus.Committed);
    protected static final List<GlobalStatus> RETRY_ROLLBACK_STATUS = Arrays.asList(GlobalStatus.RollbackRetrying,
            GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.TimeoutRollbacking);

    protected static final List<GlobalStatus> RETRY_STATUS = Stream.concat(RETRY_COMMIT_STATUS.stream(),
            RETRY_ROLLBACK_STATUS.stream()).collect(Collectors.toList());

    protected static final List<GlobalStatus> FAIL_COMMIT_STATUS = Arrays.asList(GlobalStatus.CommitFailed,
            GlobalStatus.CommitRetryTimeout);

    protected static final List<GlobalStatus> FAIL_ROLLBACK_STATUS = Arrays.asList(GlobalStatus.TimeoutRollbacked,
            GlobalStatus.RollbackFailed, GlobalStatus.RollbackRetryTimeout);

    protected static final List<GlobalStatus> FAIL_STATUS = Stream.concat(FAIL_COMMIT_STATUS.stream(),
            FAIL_ROLLBACK_STATUS.stream()).collect(Collectors.toList());

    protected static final List<GlobalStatus> FINISH_STATUS = Arrays.asList(GlobalStatus.Committed,
            GlobalStatus.Finished, GlobalStatus.Rollbacked);

    protected void commonCheck(String xid, String branchId) {
        if (StringUtils.isBlank(xid)) {
            throw new IllegalArgumentException("Wrong parameter for xid");
        }
        if (StringUtils.isBlank(branchId)) {
            throw new IllegalArgumentException("Wrong parameter for branchId");
        }
        try {
            Long.parseLong(branchId);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Wrong parameter for branchId, branch Id is not number");
        }
    }

    protected GlobalSession checkGlobalSession(String xid) {
        if (StringUtils.isBlank(xid)) {
            throw new IllegalArgumentException("Wrong parameter for xid");
        }
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (Objects.isNull(globalSession)) {
            throw new IllegalArgumentException("Global session is not exist, may be finished");
        }
        return globalSession;
    }

    /**
     * check if exist global transaction and branch transaction
     *
     * @param xid      xid
     * @param branchId branchId
     * @return CheckResult, throw IllegalArgumentException if not exist
     */
    protected CheckResult commonCheckAndGetGlobalStatus(String xid, String branchId) {
        commonCheck(xid, branchId);
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (Objects.isNull(globalSession)) {
            throw new IllegalArgumentException("global session is not exist, may be finished");
        }
        List<BranchSession> branchSessions = globalSession.getBranchSessions();
        Long paramBranchId = Long.valueOf(branchId);
        BranchSession branchSession = branchSessions.stream()
                .filter(session -> paramBranchId.equals(session.getBranchId()))
                .findAny()
                .orElseThrow(() -> new IllegalArgumentException("branch session is not exist, may be finished"));
        return new CheckResult(globalSession, branchSession);
    }

    protected void doStartBranchRetry(GlobalSession globalSession, BranchSession branchSession) {
        BranchStatus branchStatus = branchSession.getStatus();
        if (!BranchStatus.STOP_RETRY.equals(branchStatus)) {
            throw new IllegalArgumentException("current branch transactions status is not support to start retry");
        }
        // BranchStatus.PhaseOne_Done and BranchStatus.Registered will become BranchStatus.Registered
        BranchStatus newStatus = BranchStatus.Registered;
        branchSession.setStatus(newStatus);
        try {
            globalSession.changeBranchStatus(branchSession, newStatus);
        } catch (TransactionException e) {
            LOGGER.error("Change branch session status fail, xid: {}, branchId:{}",
                    globalSession.getXid(), branchSession.getBranchId(), e);
            throw new FrameworkException(e);
        } catch (Exception e) {
            LOGGER.error("Change branch session status fail, xid: {}, branchId:{}",
                    globalSession.getXid(), branchSession.getBranchId(), e);
            throw e;
        }
    }

    protected boolean doDeleteBranch(GlobalSession globalSession, BranchSession branchSession) throws TimeoutException, TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Branch delete start, xid:{} branchId:{} branchType:{}",
                    branchSession.getXid(), branchSession.getBranchId(), branchSession.getBranchType());
        }
        if (branchSession.getStatus() == BranchStatus.PhaseOne_Failed) {
            globalSession.removeBranch(branchSession);
            return true;
        }
        boolean result = DefaultCoordinator.getInstance().getCore().branchDelete(globalSession, branchSession);
        if (result) {
            if (branchSession.isAT()) {
                result = lockManager.releaseLock(branchSession);
            }
            if (result) {
                globalSession.removeBranch(branchSession);
                return true;
            }
        }
        return false;
    }

    protected static class CheckResult {
        private GlobalSession globalSession;
        private BranchSession branchSession;

        public CheckResult(GlobalSession globalSession, BranchSession branchSession) {
            this.globalSession = globalSession;
            this.branchSession = branchSession;
        }

        public GlobalSession getGlobalSession() {
            return globalSession;
        }

        public void setGlobalSession(GlobalSession globalSession) {
            this.globalSession = globalSession;
        }

        public BranchSession getBranchSession() {
            return branchSession;
        }

        public void setBranchSession(BranchSession branchSession) {
            this.branchSession = branchSession;
        }
    }
}
