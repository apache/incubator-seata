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
package org.apache.seata.tm;

import org.apache.seata.core.exception.TmTransactionException;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.model.TransactionManager;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequest;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.rpc.netty.TmNettyRemotingClient;

import java.util.concurrent.TimeoutException;

/**
 * Default implementation of Transaction Manager (TM) in Seata distributed transaction system.
 *
 * <p>Acts as the primary client-side component for managing global transactions.
 * Forwards transaction management operations to Transaction Coordinator (TC) server
 * through network communication.</p>
 *
 * <p><b>Core Operations:</b></p>
 * <ul>
 *   <li><b>Begin</b>: Request new global transaction and receive XID</li>
 *   <li><b>Commit</b>: Initiate two-phase commit protocol</li>
 *   <li><b>Rollback</b>: Request global transaction rollback</li>
 *   <li><b>Status Query</b>: Retrieve current transaction status</li>
 *   <li><b>Status Report</b>: Report transaction status changes</li>
 * </ul>
 *
 * <p><b>Communication:</b> Uses Netty-based TCP communication with configurable
 * serialization, connection pooling, and automatic failover to available TC servers.</p>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * TransactionManager tm = new DefaultTransactionManager();
 * try {
 *     String xid = tm.begin("app-id", "service-group", "order-create", 30000);
 *     // Execute business logic...
 *     GlobalStatus status = tm.commit(xid);
 * } catch (TransactionException e) {
 *     tm.rollback(xid);
 *     throw e;
 * }
 * }</pre>
 *
 * <p>Thread-safe and typically used indirectly through {@link org.apache.seata.tm.api.DefaultGlobalTransaction}
 * or {@link org.apache.seata.tm.api.TransactionalTemplate}.</p>
 *
 * @author Seata Team
 * @see TransactionManager
 * @see TmNettyRemotingClient
 * @see org.apache.seata.tm.api.DefaultGlobalTransaction
 * @since 1.0.0
 */
public class DefaultTransactionManager implements TransactionManager {

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout)
            throws TransactionException {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName(name);
        request.setTimeout(timeout);
        GlobalBeginResponse response = (GlobalBeginResponse) syncCall(request);
        if (response.getResultCode() == ResultCode.Failed) {
            throw new TmTransactionException(TransactionExceptionCode.BeginFailed, response.getMsg());
        }
        return response.getXid();
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        GlobalCommitRequest globalCommit = new GlobalCommitRequest();
        globalCommit.setXid(xid);
        GlobalCommitResponse response = (GlobalCommitResponse) syncCall(globalCommit);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setXid(xid);
        GlobalRollbackResponse response = (GlobalRollbackResponse) syncCall(globalRollback);
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
        GlobalReportResponse response = (GlobalReportResponse) syncCall(globalReport);
        return response.getGlobalStatus();
    }

    private AbstractTransactionResponse syncCall(AbstractTransactionRequest request) throws TransactionException {
        try {
            return (AbstractTransactionResponse)
                    TmNettyRemotingClient.getInstance().sendSyncRequest(request);
        } catch (TimeoutException toe) {
            throw new TmTransactionException(TransactionExceptionCode.IO, "RPC timeout", toe);
        }
    }
}
