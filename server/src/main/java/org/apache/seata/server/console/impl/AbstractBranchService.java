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

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.console.exception.ConsoleException;
import org.apache.seata.server.console.service.BranchSessionService;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;

public abstract class AbstractBranchService extends AbstractService implements BranchSessionService {
    @Override
    public SingleResult<Void> stopBranchRetry(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch transactions because it have no determinative role");
        }
        BranchSession branchSession = checkResult.getBranchSession();
        BranchStatus branchStatus = branchSession.getStatus();
        if (branchStatus.getCode() == BranchStatus.STOP_RETRY.getCode()) {
            throw new IllegalArgumentException("current branch session is already stop");
        }
        // For BranchStatus.PhaseOne_Done is finished, will remove soon, thus no support
        if (branchStatus != BranchStatus.Unknown && branchStatus != BranchStatus.Registered &&
                branchStatus != BranchStatus.PhaseOne_Done) {
            throw new IllegalArgumentException("current branch session is not support to stop");
        }
        GlobalStatus status = globalSession.getStatus();
        BranchStatus newStatus = RETRY_STATUS.contains(status) || GlobalStatus.Rollbacking.equals(status) ||
                GlobalStatus.Committing.equals(status) || GlobalStatus.StopRollbackOrRollbackRetry.equals(status) ||
                GlobalStatus.StopCommitOrCommitRetry.equals(status) ? BranchStatus.STOP_RETRY : null;
        if (newStatus == null) {
            throw new IllegalArgumentException("wrong status for global status");
        }
        branchSession.setStatus(newStatus);
        try {
            globalSession.changeBranchStatus(branchSession, newStatus);
        } catch (Exception e) {
            throw new ConsoleException(e, String.format("stop branch session retry fail, xid:%s, branchId:%s", xid, branchId));
        }
        return SingleResult.success();
    }

    @Override
    public SingleResult<Void> startBranchRetry(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch transactions because it have no determinative role");
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
        } catch (Exception e) {
            throw new ConsoleException(e, String.format("start branch session retry fail, xid:%s, branchId:%s", xid, branchId));
        }
        return SingleResult.success();
    }

    @Override
    public SingleResult<Void> deleteBranchSession(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch transactions because it have no determinative role");
        }
        GlobalStatus globalStatus = globalSession.getStatus();
        BranchSession branchSession = checkResult.getBranchSession();
        if (FAIL_STATUS.contains(globalStatus) || RETRY_STATUS.contains(globalStatus)
                || FINISH_STATUS.contains(globalStatus) || GlobalStatus.StopRollbackOrRollbackRetry == globalStatus
                || GlobalStatus.StopCommitOrCommitRetry == globalStatus || GlobalStatus.Deleting == globalStatus) {
            try {
                boolean deleted = doDeleteBranch(globalSession, branchSession);
                return deleted ? SingleResult.success() :
                        SingleResult.failure("delete branch fail, please retry again later");
            } catch (Exception e) {
                throw new ConsoleException(e, String.format("delete branch session fail, xid:%s, branchId:%s", xid, branchId));
            }
        }
        throw new IllegalArgumentException("current global transaction is not support delete branch transaction");
    }

    @Override
    public SingleResult<Void> forceDeleteBranchSession(String xid, String branchId) {
        CheckResult checkResult = commonCheckAndGetGlobalStatus(xid, branchId);
        GlobalSession globalSession = checkResult.getGlobalSession();
        // saga is not support to operate
        if (globalSession.isSaga()) {
            throw new IllegalArgumentException("saga can not operate branch transactions because it have no determinative role");
        }
        BranchSession branchSession = checkResult.getBranchSession();
        try {
            boolean deleted = doForceDeleteBranch(globalSession, branchSession);
            return deleted ? SingleResult.success() :
                    SingleResult.failure("delete branch fail, please retry again later");
        } catch (Exception e) {
            throw new ConsoleException(e, String.format("delete branch session fail, xid:%s, branchId:%s", xid, branchId));
        }
    }
}
