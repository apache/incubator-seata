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
package io.seata.server.transaction.xa;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import io.seata.common.DefaultValues;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
import io.seata.core.rpc.RemotingServer;
import io.seata.server.coordinator.AbstractCore;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The type XA core.
 *
 * @author sharajava
 */
public class XACore extends AbstractCore {

    private static final int XAER_NOTA_RETRY_TIMEOUT = DefaultValues.DEFAULT_XAER_NOTA_RETRY_TIMEOUT;

    private static final Cache<Long, Long> BRANCH_FINISHED_BEGIN_TIME_CACHE =
            CacheBuilder.newBuilder().maximumSize(2048).expireAfterAccess(1, TimeUnit.MINUTES).build();

    public XACore(RemotingServer remotingServer) {
        super(remotingServer);
    }

    @Override
    public BranchType getHandleBranchType() {
        return BranchType.XA;
    }

    @Override
    public void branchReport(BranchType branchType, String xid, long branchId, BranchStatus status,
                             String applicationData) throws TransactionException {
        super.branchReport(branchType, xid, branchId, status, applicationData);
        if (BranchStatus.PhaseOne_Failed == status) {

        }
    }

    @Override
    protected BranchStatus branchCommitSend(BranchCommitRequest request, GlobalSession globalSession,
                                            BranchSession branchSession) throws IOException, TimeoutException {
        BranchCommitResponse response = (BranchCommitResponse) remotingServer.sendSyncRequest(
                branchSession.getResourceId(), branchSession.getClientId(), request);
        if (BranchStatus.PhaseTwo_CommitFailed_XAER_NOTA_Retryable.equals(response.getBranchStatus())) {
            long now = System.currentTimeMillis();
            Long beginTime = BRANCH_FINISHED_BEGIN_TIME_CACHE.getIfPresent(branchSession.getBranchId());
            if (beginTime == null) {
                BRANCH_FINISHED_BEGIN_TIME_CACHE.put(branchSession.getBranchId(), now);
            } else if (now - beginTime > Math.max(XAER_NOTA_RETRY_TIMEOUT, globalSession.getTimeout())) {
                LOGGER.info("Commit branch XAER_NOTA retry timeout, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                BRANCH_FINISHED_BEGIN_TIME_CACHE.invalidate(branchSession.getBranchId());
                return BranchStatus.PhaseTwo_Committed;
            }
        }
        return response.getBranchStatus();
    }

    @Override
    protected BranchStatus branchRollbackSend(BranchRollbackRequest request, GlobalSession globalSession,
                                              BranchSession branchSession) throws IOException, TimeoutException {
        BranchRollbackResponse response = (BranchRollbackResponse) remotingServer.sendSyncRequest(
                branchSession.getResourceId(), branchSession.getClientId(), request);
        if (BranchStatus.PhaseTwo_RollbackFailed_XAER_NOTA_Retryable.equals(response.getBranchStatus())) {
            long now = System.currentTimeMillis();
            Long beginTime = BRANCH_FINISHED_BEGIN_TIME_CACHE.getIfPresent(branchSession.getBranchId());
            if (beginTime == null) {
                BRANCH_FINISHED_BEGIN_TIME_CACHE.put(branchSession.getBranchId(), now);
            } else if (now - beginTime > Math.max(XAER_NOTA_RETRY_TIMEOUT, globalSession.getTimeout())) {
                LOGGER.info("Rollback branch XAER_NOTA retry timeout, xid = {} branchId = {}", globalSession.getXid(), branchSession.getBranchId());
                BRANCH_FINISHED_BEGIN_TIME_CACHE.invalidate(branchSession.getBranchId());
                return BranchStatus.PhaseTwo_Rollbacked;
            }
        }
        return response.getBranchStatus();
    }

}
