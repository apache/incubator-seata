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
package io.seata.server.coordinator;

import io.seata.core.context.RootContext;
import io.seata.core.protocol.AbstractMessage;
import io.seata.core.protocol.AbstractResultMessage;
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
import io.seata.core.rpc.RpcContext;
import org.slf4j.MDC;

/**
 * The type Mdc wrapper coordinator.
 *
 * @author wang.liang
 */
public class MdcWrapperCoordinator implements Coordinator {

    private final Coordinator coordinator;

    public MdcWrapperCoordinator(Coordinator coordinator) {
        this.coordinator = coordinator;
    }

    @Override
    public GlobalBeginResponse handle(GlobalBeginRequest globalBegin, RpcContext rpcContext) {
        return coordinator.handle(globalBegin, rpcContext);
    }

    @Override
    public GlobalCommitResponse handle(GlobalCommitRequest globalCommit, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, globalCommit.getXid());
            return coordinator.handle(globalCommit, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
        }
    }

    @Override
    public GlobalRollbackResponse handle(GlobalRollbackRequest globalRollback, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, globalRollback.getXid());
            return coordinator.handle(globalRollback, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
        }
    }

    @Override
    public BranchRegisterResponse handle(BranchRegisterRequest branchRegister, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, branchRegister.getXid());
            return coordinator.handle(branchRegister, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
        }
    }

    @Override
    public BranchReportResponse handle(BranchReportRequest branchReport, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, branchReport.getXid());
            MDC.put(RootContext.MDC_KEY_BRANCH_ID, String.valueOf(branchReport.getBranchId()));
            return coordinator.handle(branchReport, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
            MDC.remove(RootContext.MDC_KEY_BRANCH_ID);
        }
    }

    @Override
    public GlobalLockQueryResponse handle(GlobalLockQueryRequest checkLock, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, checkLock.getXid());
            return coordinator.handle(checkLock, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
        }
    }

    @Override
    public GlobalStatusResponse handle(GlobalStatusRequest globalStatus, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, globalStatus.getXid());
            return coordinator.handle(globalStatus, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
        }
    }

    @Override
    public GlobalReportResponse handle(GlobalReportRequest globalReport, RpcContext rpcContext) {
        try {
            MDC.put(RootContext.MDC_KEY_XID, globalReport.getXid());
            return coordinator.handle(globalReport, rpcContext);
        } finally {
            MDC.remove(RootContext.MDC_KEY_XID);
        }
    }

    @Override
    public void init() {
        coordinator.init();
    }

    @Override
    public void destroy() {
        coordinator.destroy();
    }

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        return coordinator.onRequest(request, context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        coordinator.onResponse(response, context);
    }
}
