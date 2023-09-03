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

import io.seata.common.ConfigurationKeys;
import io.seata.console.result.SingleResult;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.console.service.BranchSessionService;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.seata.common.DefaultValues.DEFAULT_AUTO_RESTART_TIME;

public abstract class AbstractBranchService extends AbstractService implements BranchSessionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBranchService.class);

    protected static final long TIMEOUT_RETRY_PERIOD = CONFIG.getLong(ConfigurationKeys.AUTO_RESTART_TIME,
            DEFAULT_AUTO_RESTART_TIME);

    @Override
    public SingleResult<Void> stopBranchRetry(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch session");
        }
        BranchSession branchSession = checkResult.getBranchSession();
        // For BranchStatus.PhaseOne_Done is finished, will remove soon, thus no support
        BranchStatus branchStatus = branchSession.getStatus();
        if (branchStatus != BranchStatus.Unknown && branchStatus != BranchStatus.Registered &&
                branchStatus != BranchStatus.PhaseOne_Done) {
            throw new IllegalArgumentException("current branch is not currently in progress");
        }
        BranchStatus newStatus = RETRY_STATUS.contains(globalSession.getStatus()) ? BranchStatus.STOP_RETRY : null;
        if (newStatus == null) {
            throw new IllegalArgumentException("wrong status for global status, only for" + RETRY_STATUS);
        }
        branchSession.setStatus(newStatus);
        try {
            globalSession.changeBranchStatus(branchSession, newStatus);
        } catch (TransactionException e) {
            LOGGER.error("change branch session status fail, xid:{}, branchId:{}", xid, branchId, e);
            throw new IllegalStateException("change branch session status fail, please try again");
        }
        autoRestartRetry.schedule(() -> autoRestart(xid, branchId), TIMEOUT_RETRY_PERIOD, TimeUnit.MILLISECONDS);
        return SingleResult.success();
    }

    @Override
    public SingleResult<Void> startBranchRetry(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch session");
        }
        doStartBranchRetry(globalSession, checkResult.getBranchSession());
        return SingleResult.success();
    }

    @Override
    public SingleResult<Void> deleteBranchSession(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch session");
        }
        GlobalStatus globalStatus = globalSession.getStatus();
        BranchSession branchSession = checkResult.getBranchSession();
        if (FAIL_STATUS.contains(globalStatus) || RETRY_STATUS.contains(globalStatus) ||
                FINISH_STATUS.contains(globalStatus)) {
            try {
                boolean deleted = doDeleteBranch(globalSession, branchSession);
                return deleted ? SingleResult.success() :
                        SingleResult.failure("delete branch fail, please retry again");
            } catch (TransactionException | TimeoutException e) {
                LOGGER.error("Delete lock fail, xid:{}, branchId:{}, reason: {}", xid, branchId, e.getMessage(), e);
                throw new RuntimeException(e);
            } catch (Exception e) {
                LOGGER.error("Delete lock fail, xid:{}, branchId:{}", xid, branchId, e);
                throw new RuntimeException(e);
            }
        }
        throw new IllegalArgumentException("current global transaction is not support delete branch transaction");
    }

    private void autoRestart(String xid, String branchId) {
        GlobalSession globalSession = SessionHolder.findGlobalSession(xid);
        if (Objects.nonNull(globalSession)) {
            List<BranchSession> branchSessions = globalSession.getBranchSessions();
            Long paramBranchId = Long.valueOf(branchId);
            branchSessions.forEach(branchSession -> {
                LOGGER.info("Auto restart the branch session to retry,xid: {}, branchId: {}", xid, branchId);
                if (paramBranchId.equals(branchSession.getBranchId())) {
                    doStartBranchRetry(globalSession, branchSession);
                }
            });
        }
    }

}
