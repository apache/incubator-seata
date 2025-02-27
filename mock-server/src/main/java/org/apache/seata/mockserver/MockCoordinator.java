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
package org.apache.seata.mockserver;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.IntStream;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.UUIDGenerator;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.AbstractGlobalEndResponse;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportRequest;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.protocol.transaction.TCInboundHandler;
import org.apache.seata.core.rpc.Disposable;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.mockserver.call.CallRm;
import org.apache.seata.mockserver.model.MockBranchSession;
import org.apache.seata.mockserver.model.MockGlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock Coordinator
 **/
public class MockCoordinator implements TCInboundHandler, TransactionMessageHandler, Disposable {

    protected static final Logger LOGGER = LoggerFactory.getLogger(MockCoordinator.class);

    RemotingServer remotingServer;

    private static MockCoordinator coordinator;

    private static String AllBeginFailXid = "0";

    private Map<String, GlobalStatus> globalStatusMap;
    private Map<String, ResultCode> expectedResultMap;
    private Map<String, Integer> expectRetryTimesMap;
    private Map<String, List<MockBranchSession>> branchMap;

    private MockCoordinator() {
    }

    public static MockCoordinator getInstance() {
        if (coordinator == null) {
            synchronized (MockCoordinator.class) {
                if (coordinator == null) {
                    coordinator = new MockCoordinator();
                    coordinator.expectedResultMap = new ConcurrentHashMap<>();
                    coordinator.globalStatusMap = new ConcurrentHashMap<>();
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
        AbstractTransactionRequestToTC transactionRequest = (AbstractTransactionRequestToTC)request;
        transactionRequest.setTCInboundHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        response.setResultCode(ResultCode.Success);
    }

    public void setRemotingServer(RemotingServer remotingServer) {
        this.remotingServer = remotingServer;
    }

    public void setExpectedResult(String xid, ResultCode expected) {
        expectedResultMap.put(xid, expected);
    }

    public void setExpectedRetry(String xid, int times) {
        expectRetryTimesMap.put(xid, times);
    }

    private void checkMockActionFail(String xid) throws TransactionException {
        if (ResultCode.Failed == expectedResultMap.get(xid)) {
            throw new TransactionException(TransactionExceptionCode.Broken, "mock action expect fail");
        }
    }

    private <T extends AbstractTransactionResponse> T handleException(TransactionException e, T response,
                                                                      ResultCode resultCode, String messagePrefix,
                                                                      GlobalStatus... globalStatus) {
        response.setTransactionExceptionCode(e.getCode());
        response.setResultCode(resultCode);
        response.setMsg(messagePrefix + "[" + e.getMessage() + "]");
        if (response instanceof AbstractGlobalEndResponse) {
            if (globalStatus != null && globalStatus.length > 0) {
                ((AbstractGlobalEndResponse)response).setGlobalStatus(globalStatus[0]);
            }
        }
        return response;
    }

    @Override
    public GlobalBeginResponse handle(GlobalBeginRequest request, RpcContext rpcContext) {
        GlobalBeginResponse response = new GlobalBeginResponse();
        try {
            checkMockActionFail(AllBeginFailXid);
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockBeginException");
        }
        MockGlobalSession session = new MockGlobalSession(rpcContext.getApplicationId(),
            rpcContext.getTransactionServiceGroup(), request.getTransactionName(), request.getTimeout());
        globalStatusMap.putIfAbsent(session.getXid(), GlobalStatus.Begin);
        response.setXid(session.getXid());
        response.setResultCode(ResultCode.Success);
        return response;
    }

    @Override
    public GlobalCommitResponse handle(GlobalCommitRequest request, RpcContext rpcContext) {
        GlobalCommitResponse response = new GlobalCommitResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockCommitException", GlobalStatus.CommitFailed);
        }
        response.setGlobalStatus(GlobalStatus.Committed);
        response.setResultCode(ResultCode.Success);
        globalStatusMap.put(request.getXid(), GlobalStatus.Committed);

        int retry = expectRetryTimesMap.getOrDefault(request.getXid(), 0);
        List<MockBranchSession> branchSessions = branchMap.get(request.getXid());
        if (CollectionUtils.isEmpty(branchSessions)) {
            LOGGER.warn("[doGlobalCommit]branchSessions is empty,XID=" + request.getXid());
            return response;
        }
        branchSessions.forEach(branch -> {
            CallRm.branchCommit(remotingServer, branch);
            IntStream.range(0, retry).forEach(i -> CallRm.branchCommit(remotingServer, branch));
        });
        branchMap.remove(request.getXid());
        globalStatusMap.remove(request.getXid());
        return response;
    }

    @Override
    public GlobalRollbackResponse handle(GlobalRollbackRequest request, RpcContext rpcContext) {
        GlobalRollbackResponse response = new GlobalRollbackResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockRollbackException",
                GlobalStatus.RollbackFailed);
        }
        response.setGlobalStatus(GlobalStatus.Rollbacked);
        response.setResultCode(ResultCode.Success);
        globalStatusMap.put(request.getXid(), GlobalStatus.Rollbacked);

        int retry = expectRetryTimesMap.getOrDefault(request.getXid(), 0);
        List<MockBranchSession> branchSessions = branchMap.get(request.getXid());
        if (CollectionUtils.isEmpty(branchSessions)) {
            LOGGER.warn("[doGlobalRollback]branchSessions is empty,XID=" + request.getXid());
            return response;
        }
        branchSessions.forEach(branch -> {
            CallRm.branchRollback(remotingServer, branch);
            IntStream.range(0, retry).forEach(i -> CallRm.branchRollback(remotingServer, branch));
        });
        branchMap.remove(request.getXid());
        globalStatusMap.remove(request.getXid());
        return response;
    }

    @Override
    public BranchRegisterResponse handle(BranchRegisterRequest request, RpcContext rpcContext) {
        BranchRegisterResponse response = new BranchRegisterResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockBranchRegisterException");
        }
        MockBranchSession branchSession = new MockBranchSession(request.getBranchType());
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
        return response;
    }

    @Override
    public BranchReportResponse handle(BranchReportRequest request, RpcContext rpcContext) {
        BranchReportResponse response = new BranchReportResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockBranchReportException");
        }
        String xid = request.getXid();
        branchMap.compute(xid, (key, val) -> {
            if (val != null) {
                val.stream().filter(branch -> branch.getBranchId() == request.getBranchId()).forEach(branch -> {
                    branch.setApplicationData(request.getApplicationData());
                });
            }
            return val;
        });
        response.setResultCode(ResultCode.Success);
        return response;
    }

    @Override
    public GlobalLockQueryResponse handle(GlobalLockQueryRequest request, RpcContext rpcContext) {
        GlobalLockQueryResponse response = new GlobalLockQueryResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockLockQueryException");
        }
        response.setLockable(true);
        response.setResultCode(ResultCode.Success);
        return response;
    }

    @Override
    public GlobalStatusResponse handle(GlobalStatusRequest request, RpcContext rpcContext) {
        GlobalStatusResponse response = new GlobalStatusResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockStatusException", GlobalStatus.Finished);
        }
        GlobalStatus globalStatus = globalStatusMap.get(request.getXid());
        if (globalStatus == null) {
            globalStatus = GlobalStatus.Finished;
        }
        response.setGlobalStatus(globalStatus);
        response.setResultCode(ResultCode.Success);
        return response;
    }

    @Override
    public GlobalReportResponse handle(GlobalReportRequest request, RpcContext rpcContext) {
        GlobalReportResponse response = new GlobalReportResponse();
        try {
            checkMockActionFail(request.getXid());
        } catch (TransactionException e) {
            return handleException(e, response, ResultCode.Failed, "MockReportException", GlobalStatus.Finished);
        }
        GlobalStatus globalStatus = request.getGlobalStatus();
        globalStatusMap.put(request.getXid(), globalStatus);
        response.setGlobalStatus(globalStatus);
        response.setResultCode(ResultCode.Success);
        return response;
    }
}
