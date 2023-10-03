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
import io.seata.console.result.SingleResult;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.server.console.service.BranchSessionService;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeoutException;

public abstract class AbstractBranchService extends AbstractService implements BranchSessionService {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBranchService.class);

    @Override
    public SingleResult<Void> stopBranchRetry(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch session");
        }
        BranchSession branchSession = checkResult.getBranchSession();
        BranchStatus branchStatus = branchSession.getStatus();
        if (branchStatus.getCode() == BranchStatus.STOP_RETRY.getCode()) {
            return SingleResult.success();
        }
        // For BranchStatus.PhaseOne_Done is finished, will remove soon, thus no support
        if (branchStatus != BranchStatus.Unknown && branchStatus != BranchStatus.Registered &&
                branchStatus != BranchStatus.PhaseOne_Done) {
            throw new IllegalArgumentException("current branch session is not support to stop");
        }
        GlobalStatus status = globalSession.getStatus();
        BranchStatus newStatus = RETRY_STATUS.contains(status) || GlobalStatus.Rollbacking.equals(status) ||
                GlobalStatus.Committing.equals(status) || GlobalStatus.StopRollbackRetry.equals(status) ||
                GlobalStatus.StopCommitRetry.equals(status) ? BranchStatus.STOP_RETRY : null;
        if (newStatus == null) {
            throw new IllegalArgumentException("wrong status for global status");
        }
        branchSession.setStatus(newStatus);
        try {
            globalSession.changeBranchStatus(branchSession, newStatus);
        } catch (TransactionException e) {
            LOGGER.error("stop branch session retry fail, xid:{}, branchId:{}", xid, branchId, e);
            throw new FrameworkException(e);
        } catch (Exception e) {
            LOGGER.error("stop branch session retry fail, xid:{}, branchId:{}", xid, branchId, e);
            throw e;
        }
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
        BranchSession branchSession = checkResult.getBranchSession();
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
            LOGGER.error("start branch session retry fail, xid: {}, branchId:{}", xid, branchId, e);
            throw new FrameworkException(e);
        } catch (Exception e) {
            LOGGER.error("change branch session retry fail, xid: {}, branchId:{}", xid, branchId, e);
            throw e;
        }
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
        if (FAIL_STATUS.contains(globalStatus) || RETRY_STATUS.contains(globalStatus)
                || FINISH_STATUS.contains(globalStatus) || GlobalStatus.StopRollbackRetry == globalStatus
                || GlobalStatus.StopCommitRetry == globalStatus || GlobalStatus.Deleting == globalStatus) {
            try {
                boolean deleted = doDeleteBranch(globalSession, branchSession);
                return deleted ? SingleResult.success() :
                        SingleResult.failure("delete branch fail, please retry again later");
            } catch (TransactionException | TimeoutException | RuntimeException e) {
                LOGGER.error("delete branch session fail, xid:{}, branchId:{}", xid, branchId, e);
                throw new FrameworkException(e);
            } catch (Exception e) {
                LOGGER.error("delete branch session fail, xid:{}, branchId:{}", xid, branchId, e);
                throw e;
            }
        }
        throw new IllegalArgumentException("current global transaction is not support delete branch transaction");
    }
}
