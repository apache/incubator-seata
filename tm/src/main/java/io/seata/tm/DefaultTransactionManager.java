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
package io.seata.tm;

import io.seata.core.context.RootContext;
import io.seata.core.exception.TmTransactionException;
import io.seata.core.exception.TransactionException;
import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.model.TransactionManager;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.AbstractTransactionRequest;
import io.seata.core.protocol.transaction.AbstractTransactionResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.netty.TmNettyRemotingClient;
import io.seata.metrics.service.MetricsPublisher;
import io.seata.tm.api.DefaultGlobalTransaction;
import io.seata.tm.api.transaction.TransactionInfo;

import java.util.concurrent.TimeoutException;

/**
 * The type Default transaction manager.
 *
 * @author sharajava
 */
public class DefaultTransactionManager implements TransactionManager {

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
        throws TransactionException {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName(name);
        request.setTimeout(timeout);
        long beginTime = System.currentTimeMillis();
        GlobalBeginResponse response = (GlobalBeginResponse) syncCall(request);
        if (response.getResultCode() == ResultCode.Failed) {
            MetricsPublisher.postGlobalTransaction(name, beginTime, System.currentTimeMillis(), GlobalStatus.BeginFailed.name());
            throw new TmTransactionException(TransactionExceptionCode.BeginFailed, response.getMsg());
        }
        MetricsPublisher.postGlobalTransaction(name, beginTime, System.currentTimeMillis(), GlobalStatus.BeginSuccess.name());
        return response.getXid();
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalCommitRequest globalCommit = new GlobalCommitRequest();
        globalCommit.setXid(xid);
        long beginTime = System.currentTimeMillis();
        GlobalCommitResponse response = (GlobalCommitResponse) syncCall(globalCommit);
        if (response.getResultCode() == ResultCode.Failed) {
            MetricsPublisher.postGlobalTransaction(RootContext.getTxName(), beginTime, System.currentTimeMillis(), GlobalStatus.Committed.name());
        }
        MetricsPublisher.postGlobalTransaction(RootContext.getTxName(), beginTime, System.currentTimeMillis(), GlobalStatus.CommitFailed.name());
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setXid(xid);
        long beginTime = System.currentTimeMillis();
        GlobalRollbackResponse response = (GlobalRollbackResponse) syncCall(globalRollback);
        if (response.getResultCode() == ResultCode.Failed) {
            MetricsPublisher.postGlobalTransaction(RootContext.getTxName(), beginTime, System.currentTimeMillis(), GlobalStatus.Rollbacked.name());
        }
        MetricsPublisher.postGlobalTransaction(RootContext.getTxName(), beginTime, System.currentTimeMillis(), GlobalStatus.RollbackFailed.name());
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        GlobalStatusRequest queryGlobalStatus = new GlobalStatusRequest();
        queryGlobalStatus.setXid(xid);
        GlobalStatusResponse response = (GlobalStatusResponse) syncCall(queryGlobalStatus);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus globalReport(String xid, GlobalStatus globalStatus) throws TransactionException {
        GlobalReportRequest globalReport = new GlobalReportRequest();
        globalReport.setXid(xid);
        globalReport.setGlobalStatus(globalStatus);
        long beginTime = System.currentTimeMillis();
        GlobalReportResponse response = (GlobalReportResponse) syncCall(globalReport);
        if (response.getResultCode() == ResultCode.Failed) {
            MetricsPublisher.postGlobalTransaction(RootContext.getTxName(), beginTime, System.currentTimeMillis(), GlobalStatus.ReportFailed.name());
        }
        MetricsPublisher.postGlobalTransaction(RootContext.getTxName(), beginTime, System.currentTimeMillis(), GlobalStatus.ReportSuccess.name());
        return response.getGlobalStatus();
    }

    private AbstractTransactionResponse syncCall(AbstractTransactionRequest request) throws TransactionException {
        try {
            return (AbstractTransactionResponse) TmNettyRemotingClient.getInstance().sendSyncRequest(request);
        } catch (TimeoutException toe) {
            throw new TmTransactionException(TransactionExceptionCode.IO, "RPC timeout", toe);
        }
    }
}
