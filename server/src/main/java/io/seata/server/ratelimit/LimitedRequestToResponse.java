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


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.seata.core.protocol.AbstractResultMessage;
import io.seata.core.protocol.ResultCode;
import io.seata.core.protocol.transaction.BranchCommitRequest;
import io.seata.core.protocol.transaction.BranchCommitResponse;
import io.seata.core.protocol.transaction.BranchRegisterRequest;
import io.seata.core.protocol.transaction.BranchRegisterResponse;
import io.seata.core.protocol.transaction.BranchReportRequest;
import io.seata.core.protocol.transaction.BranchReportResponse;
import io.seata.core.protocol.transaction.BranchRollbackRequest;
import io.seata.core.protocol.transaction.BranchRollbackResponse;
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
 * The ratelimited request and corresponding response.
 */
public class LimitedRequestToResponse {
    private Map<Class, AbstractResultMessage> requestToResponseMap = new ConcurrentHashMap<>();

    public LimitedRequestToResponse() {
        requestToResponseMap.put(GlobalBeginRequest.class, new GlobalBeginResponse());
        requestToResponseMap.put(GlobalCommitRequest.class, new GlobalCommitResponse());
        requestToResponseMap.put(GlobalRollbackRequest.class, new GlobalRollbackResponse());
        requestToResponseMap.put(GlobalStatusRequest.class, new GlobalStatusResponse());
        requestToResponseMap.put(GlobalLockQueryRequest.class, new GlobalLockQueryResponse());
        requestToResponseMap.put(GlobalReportRequest.class, new GlobalReportResponse());
        requestToResponseMap.put(BranchRegisterRequest.class, new BranchRegisterResponse());
        requestToResponseMap.put(BranchReportRequest.class, new BranchReportResponse());
        requestToResponseMap.put(BranchCommitRequest.class, new BranchCommitResponse());
        requestToResponseMap.put(BranchRollbackRequest.class, new BranchRollbackResponse());
        for (Map.Entry<Class, AbstractResultMessage> entry : requestToResponseMap.entrySet()) {
            entry.getValue().setResultCode(ResultCode.Failed);
            entry.getValue().setMsg(entry.getKey().getSimpleName() + " refused: rate limit.");
        }
    }

    public AbstractResultMessage get(Class clazz) {
        return requestToResponseMap.get(clazz);
    }
}
