/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.rm;

import com.alibaba.fescar.common.exception.ShouldNeverHappenException;
import com.alibaba.fescar.core.exception.AbstractExceptionHandler;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.protocol.AbstractMessage;
import com.alibaba.fescar.core.protocol.AbstractResultMessage;
import com.alibaba.fescar.core.protocol.transaction.AbstractTransactionRequestToRM;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.BranchRollbackResponse;
import com.alibaba.fescar.core.protocol.transaction.RMInboundHandler;
import com.alibaba.fescar.core.rpc.RpcContext;
import com.alibaba.fescar.core.rpc.TransactionMessageHandler;

/**
 * The type Abstract rm handler at.
 */
public abstract class AbstractRMHandlerAT extends AbstractExceptionHandler
    implements RMInboundHandler, TransactionMessageHandler {

    @Override
    public BranchCommitResponse handle(BranchCommitRequest request) {
        BranchCommitResponse response = new BranchCommitResponse();
        exceptionHandleTemplate(new Callback<BranchCommitRequest, BranchCommitResponse>() {
            @Override
            public void execute(BranchCommitRequest request, BranchCommitResponse response) throws TransactionException {
                doBranchCommit(request, response);
            }
        }, request, response);
        return response;
    }

    /**
     * Do branch commit.
     *
     * @param request  the request
     * @param response the response
     * @throws TransactionException the transaction exception
     */
    protected abstract void doBranchCommit(BranchCommitRequest request, BranchCommitResponse response) throws TransactionException;

    @Override
    public BranchRollbackResponse handle(BranchRollbackRequest request) {
        BranchRollbackResponse response = new BranchRollbackResponse();
        exceptionHandleTemplate(new Callback<BranchRollbackRequest, BranchRollbackResponse>() {
            @Override
            public void execute(BranchRollbackRequest request, BranchRollbackResponse response) throws TransactionException {
                doBranchRollback(request, response);
            }
        }, request, response);
        return response;
    }

    /**
     * Do branch rollback.
     *
     * @param request  the request
     * @param response the response
     * @throws TransactionException the transaction exception
     */
    protected abstract void doBranchRollback(BranchRollbackRequest request, BranchRollbackResponse response) throws TransactionException;

    @Override
    public AbstractResultMessage onRequest(AbstractMessage request, RpcContext context) {
        if (!(request instanceof AbstractTransactionRequestToRM)) {
            throw new IllegalArgumentException();
        }
        AbstractTransactionRequestToRM transactionRequest = (AbstractTransactionRequestToRM) request;
        transactionRequest.setRMInboundMessageHandler(this);

        return transactionRequest.handle(context);
    }

    @Override
    public void onResponse(AbstractResultMessage response, RpcContext context) {
        throw new ShouldNeverHappenException();

    }
}
