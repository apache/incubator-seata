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
package io.seata.mockserver;

import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.GlobalBeginRequest;
import io.seata.core.protocol.transaction.GlobalBeginResponse;
import io.seata.core.protocol.transaction.GlobalCommitRequest;
import io.seata.core.protocol.transaction.GlobalCommitResponse;
import io.seata.core.protocol.transaction.GlobalLockQueryRequest;
import io.seata.core.protocol.transaction.GlobalLockQueryResponse;
import io.seata.core.protocol.transaction.GlobalReportRequest;
import io.seata.core.protocol.transaction.GlobalReportResponse;
import io.seata.core.protocol.transaction.GlobalRollbackRequest;
import io.seata.core.protocol.transaction.GlobalRollbackResponse;
import io.seata.core.protocol.transaction.GlobalStatusRequest;
import io.seata.core.protocol.transaction.GlobalStatusResponse;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.mockserver.call.CallRm;
import io.seata.server.AbstractTCInboundHandler;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.GlobalSession;

/**
 * Mock Coordinator
 **/
public class MockCoordinator extends AbstractTCInboundHandler implements TransactionMessageHandler, Disposable {

    RemotingServer remotingServer;

    @Override
    public void destroy() {

    }

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToTC)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToTC transactionRequest = (AbstractTransactionRequestToTC) request;
        transactionRequest.setTCInboundHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doGlobalBegin(GlobalBeginRequest request, GlobalBeginResponse response, RpcContext rpcContext) throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession(rpcContext.getApplicationId(),
                rpcContext.getTransactionServiceGroup(), request.getTransactionName(), request.getTimeout());
        response.setXid(session.getXid());
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doGlobalCommit(GlobalCommitRequest request, GlobalCommitResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(GlobalStatus.Committed);
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(GlobalStatus.Rollbacked);
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doBranchRegister(BranchRegisterRequest request, BranchRegisterResponse response, RpcContext rpcContext) throws TransactionException {
        response.setBranchId(UUIDGenerator.generateUUID());
        response.setResultCode(ResultCode.Success);

        String resourceId = request.getResourceId();
        String clientId = rpcContext.getClientId();

        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(1000);
                BranchStatus commit = CallRm.branchCommit(remotingServer, resourceId, clientId);
                BranchStatus rollback = CallRm.branchRollback(remotingServer, resourceId, clientId);
//                if (ProtocolConstants.VERSION_0 != Version.calcProtocolVersion(rpcContext.getVersion())) {
//                    CallRm.deleteUndoLog(remotingServer, resourceId, clientId);
//                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        thread.start();
    }

    @Override
    protected void doBranchReport(BranchReportRequest request, BranchReportResponse response, RpcContext rpcContext) throws TransactionException {
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doLockCheck(GlobalLockQueryRequest request, GlobalLockQueryResponse response, RpcContext rpcContext) throws TransactionException {
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doGlobalStatus(GlobalStatusRequest request, GlobalStatusResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(GlobalStatus.Committed);
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doGlobalReport(GlobalReportRequest request, GlobalReportResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(GlobalStatus.Committed);
        response.setResultCode(ResultCode.Success);
    }

    public void setRemotingServer(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }
}
