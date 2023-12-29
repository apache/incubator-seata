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

import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.*;
import io.seata.core.rpc.Disposable;
import io.seata.core.rpc.RemotingServer;
import io.seata.core.rpc.RpcContext;
import io.seata.core.rpc.TransactionMessageHandler;
import io.seata.mockserver.call.CallRm;
import io.seata.server.AbstractTCInboundHandler;
import io.seata.server.UUIDGenerator;
import io.seata.server.coordinator.DefaultCoordinator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

/**
 * Mock Coordinator
 **/
public class MockCoordinator extends AbstractTCInboundHandler implements TransactionMessageHandler, Disposable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockCoordinator.class);

    RemotingServer remotingServer;

    private static MockCoordinator coordinator;

    private Map<String, ExpectTransactionResult> expectTransactionResultMap;
    private Map<String, Integer> expectRetryTimesMap;
    private Map<String, List<BranchSession>> branchMap;

    private MockCoordinator() {
    }


    public static MockCoordinator getInstance() {
        if (coordinator == null) {
            synchronized (MockCoordinator.class) {
                if (coordinator == null) {
                    coordinator = new MockCoordinator();
                    coordinator.expectTransactionResultMap = new ConcurrentHashMap<>();
                    coordinator.expectRetryTimesMap = new ConcurrentHashMap<>();
                    coordinator.branchMap = new ConcurrentHashMap<>();
                }
            }
        }
        return coordinator;
    }


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
        expectTransactionResultMap.putIfAbsent(session.getXid(), ExpectTransactionResult.AllCommitted);
        response.setXid(session.getXid());
        response.setResultCode(ResultCode.Success);
    }

    @Override
    protected void doGlobalCommit(GlobalCommitRequest request, GlobalCommitResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(GlobalStatus.Committed);
        response.setResultCode(ResultCode.Success);

        int retry = expectRetryTimesMap.getOrDefault(request.getXid(),0);
        List<BranchSession> branchSessions = branchMap.get(request.getXid());
        if(CollectionUtils.isEmpty(branchSessions)){
            LOGGER.info("branchSessions is empty,XID=" + request.getXid());
        }
        branchSessions.forEach(branch -> {
            CallRm.branchCommit(remotingServer, branch.getResourceId(), branch.getClientId());
            IntStream.range(0, retry).forEach(i ->
                    CallRm.branchCommit(remotingServer, branch.getResourceId(), branch.getClientId()));
        });
    }

    @Override
    protected void doGlobalRollback(GlobalRollbackRequest request, GlobalRollbackResponse response, RpcContext rpcContext) throws TransactionException {
        response.setGlobalStatus(GlobalStatus.Rollbacked);
        response.setResultCode(ResultCode.Success);


        int retry = expectRetryTimesMap.getOrDefault(request.getXid(),0);
        List<BranchSession> branchSessions = branchMap.get(request.getXid());
        if(CollectionUtils.isEmpty(branchSessions)){
            LOGGER.info("branchSessions is empty,XID=" + request.getXid());
        }
        branchSessions.forEach(branch -> {
            CallRm.branchRollback(remotingServer, branch.getResourceId(), branch.getClientId());
            IntStream.range(0, retry).forEach(i ->
                    CallRm.branchRollback(remotingServer, branch.getResourceId(), branch.getClientId()));
        });
    }

    @Override
    protected void doBranchRegister(BranchRegisterRequest request, BranchRegisterResponse response, RpcContext rpcContext) throws TransactionException {

        BranchSession branchSession = new BranchSession(request.getBranchType());

        String xid = request.getXid();
        branchSession.setXid(xid);
//        branchSession.setTransactionId(request.getTransactionId());
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setResourceId(request.getResourceId());
        branchSession.setLockKey(request.getLockKey());
        branchSession.setClientId(rpcContext.getClientId());
        branchSession.setApplicationData(request.getApplicationData());
        branchSession.setStatus(BranchStatus.Registered);
        branchMap.compute(xid, (key, val) -> {
            if (val == null) {
                val = new ArrayList<>();
            }
            val.add(branchSession);
            return val;
        });

        response.setBranchId(branchSession.getBranchId());
        response.setResultCode(ResultCode.Success);

//        Thread thread = new Thread(() -> {
//            try {
//                Thread.sleep(1000);
//                if (ProtocolConstants.VERSION_0 != Version.calcProtocolVersion(rpcContext.getVersion())) {
//                    CallRm.deleteUndoLog(remotingServer, resourceId, clientId);
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        });
//        thread.start();
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


    public void setExpectedResult(String xid, ExpectTransactionResult expected) {
        expectTransactionResultMap.put(xid, expected);
    }

    public void setExpectedRetry(String xid, int times) {
        expectRetryTimesMap.put(xid, times);
    }
}
