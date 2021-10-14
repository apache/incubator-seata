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
package io.seata.server.ratelimit;

import java.util.HashMap;
import java.util.Map;

import io.seata.core.exception.TransactionExceptionCode;
import io.seata.core.model.GlobalStatus;
import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.ResultCode;
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

/**
 * The rate limited request type and corresponding response.
 */
public class RateLimitedResponseMap {

    private Map<Class, AbstractResultMessage> responseMap = new HashMap<>();

    private static volatile RateLimitedResponseMap instance;

    private RateLimitedResponseMap() {
        BranchReportResponse branchReportResponse = new BranchReportResponse();
        branchReportResponse.setTransactionExceptionCode(TransactionExceptionCode.BranchReportFailed);

        GlobalBeginResponse globalBeginResponse = new GlobalBeginResponse();
        globalBeginResponse.setTransactionExceptionCode(TransactionExceptionCode.BeginFailed);

        GlobalCommitResponse globalCommitResponse = new GlobalCommitResponse();
        globalCommitResponse.setTransactionExceptionCode(TransactionExceptionCode.CommitFailed);
        globalCommitResponse.setGlobalStatus(GlobalStatus.CommitFailed);

        GlobalLockQueryResponse globalLockQueryResponse = new GlobalLockQueryResponse();
        globalLockQueryResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalLockQueryFailed);

        GlobalReportResponse globalReportResponse = new GlobalReportResponse();
        globalReportResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalReportFailed);
        globalReportResponse.setGlobalStatus(GlobalStatus.UnKnown);

        GlobalRollbackResponse globalRollbackResponse = new GlobalRollbackResponse();
        globalRollbackResponse.setTransactionExceptionCode(TransactionExceptionCode.RollbackFailed);
        globalRollbackResponse.setGlobalStatus(GlobalStatus.RollbackFailed);

        GlobalStatusResponse globalStatusResponse = new GlobalStatusResponse();
        globalStatusResponse.setTransactionExceptionCode(TransactionExceptionCode.GlobalStatusFailed);
        globalStatusResponse.setGlobalStatus(GlobalStatus.UnKnown);

        responseMap.put(BranchReportRequest.class, branchReportResponse);
        responseMap.put(GlobalBeginRequest.class, globalBeginResponse);
        responseMap.put(GlobalCommitRequest.class, globalCommitResponse);
        responseMap.put(GlobalLockQueryRequest.class, globalLockQueryResponse);
        responseMap.put(GlobalReportRequest.class, globalReportResponse);
        responseMap.put(GlobalRollbackRequest.class, globalRollbackResponse);
        responseMap.put(GlobalStatusRequest.class, globalStatusResponse);
        for (Map.Entry<Class, AbstractResultMessage> entry : responseMap.entrySet()) {
            entry.getValue().setResultCode(ResultCode.Failed);
            entry.getValue().setMsg(entry.getKey().getSimpleName() + " rate limited.");
        }
    }

    public static RateLimitedResponseMap getInstance() {
        if (instance == null) {
            synchronized (RateLimitedResponseMap.class) {
                if (instance == null) {
                    instance = new RateLimitedResponseMap();
                }
            }
        }
        return instance;
    }

    public AbstractResultMessage get(Class clazz) {
        return responseMap.get(clazz);
    }
}