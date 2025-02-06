/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.server.console.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.coordinator.DefaultCoordinator;
import org.apache.seata.server.lock.LockManager;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * The abstract service.
 */
public abstract class AbstractService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractService.class);

    protected final LockManager lockManager = LockerManagerFactory.getLockManager();

    protected static final List<GlobalStatus> RETRY_COMMIT_STATUS = Arrays.asList(GlobalStatus.CommitRetrying);

    protected static final List<GlobalStatus> RETRY_ROLLBACK_STATUS = Arrays.asList(GlobalStatus.RollbackRetrying,
            GlobalStatus.TimeoutRollbackRetrying, GlobalStatus.TimeoutRollbacking);

    protected static final List<GlobalStatus> COMMIT_ING_STATUS = Stream.concat(RETRY_COMMIT_STATUS.stream(),
            Collections.singletonList(GlobalStatus.Committing).stream()).collect(Collectors.toList());

    protected static final List<GlobalStatus> ROLLBACK_ING_STATUS = Stream.concat(RETRY_ROLLBACK_STATUS.stream(),
            Collections.singletonList(GlobalStatus.Rollbacking).stream()).collect(Collectors.toList());

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

    protected boolean doDeleteBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Branch delete start, xid:{} branchId:{} branchType:{}",
                    branchSession.getXid(), branchSession.getBranchId(), branchSession.getBranchType());
        }
        // local transaction failed, not need to do branch del for phase two
        if (branchSession.getStatus() == BranchStatus.PhaseOne_Failed) {
            globalSession.removeBranch(branchSession);
            return true;
        }
        boolean result = DefaultCoordinator.getInstance().doBranchDelete(globalSession, branchSession);
        if (result) {
            result = branchSession.unlock();
            if (result) {
                globalSession.removeBranch(branchSession);
                return true;
            }
        }
        return false;
    }

    protected boolean doForceDeleteBranch(GlobalSession globalSession, BranchSession branchSession) throws TransactionException {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Branch force delete start, xid:{} branchId:{} branchType:{}",
                    branchSession.getXid(), branchSession.getBranchId(), branchSession.getBranchType());
        }
        globalSession.removeBranch(branchSession);
        return true;
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
