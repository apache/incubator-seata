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

package com.alibaba.fescar.tm;

import java.util.concurrent.TimeoutException;

import com.alibaba.fescar.common.XID;
import com.alibaba.fescar.core.exception.TransactionException;
import com.alibaba.fescar.core.exception.TransactionExceptionCode;
import com.alibaba.fescar.core.model.GlobalStatus;
import com.alibaba.fescar.core.model.TransactionManager;
import com.alibaba.fescar.core.protocol.transaction.AbstractTransactionRequest;
import com.alibaba.fescar.core.protocol.transaction.AbstractTransactionResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalBeginResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalCommitResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalRollbackResponse;
import com.alibaba.fescar.core.protocol.transaction.GlobalStatusRequest;
import com.alibaba.fescar.core.protocol.transaction.GlobalStatusResponse;
import com.alibaba.fescar.core.rpc.netty.TmRpcClient;

/**
 * The type Default transaction manager.
 */
public class DefaultTransactionManager implements TransactionManager {

    private static class SingletonHolder {
        private static final TransactionManager INSTANCE = new DefaultTransactionManager();
    }

    /**
     * Get transaction manager.
     *
     * @return the transaction manager
     */
    public static TransactionManager get() {
        return SingletonHolder.INSTANCE;
    }

    private DefaultTransactionManager() {

    }

    @Override
    public String begin(String applicationId, String transactionServiceGroup, String name, int timeout) throws TransactionException {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName(name);
        request.setTimeout(timeout);
        GlobalBeginResponse response = (GlobalBeginResponse) syncCall(request);
        return response.getXid();
    }

    @Override
    public GlobalStatus commit(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalCommitRequest globalCommit = new GlobalCommitRequest();
        globalCommit.setTransactionId(txId);
        GlobalCommitResponse response = (GlobalCommitResponse) syncCall(globalCommit);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus rollback(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalRollbackRequest globalRollback = new GlobalRollbackRequest();
        globalRollback.setTransactionId(txId);
        GlobalRollbackResponse response = (GlobalRollbackResponse) syncCall(globalRollback);
        return response.getGlobalStatus();
    }

    @Override
    public GlobalStatus getStatus(String xid) throws TransactionException {
        long txId = XID.getTransactionId(xid);
        GlobalStatusRequest queryGlobalStatus = new GlobalStatusRequest();
        queryGlobalStatus.setTransactionId(txId);
        GlobalStatusResponse response = (GlobalStatusResponse) syncCall(queryGlobalStatus);
        return response.getGlobalStatus();
    }

    private AbstractTransactionResponse syncCall(AbstractTransactionRequest request) throws TransactionException {
        try {
            return (AbstractTransactionResponse) TmRpcClient.getInstance().sendMsgWithResponse(request);
        } catch (TimeoutException toe) {
            throw new TransactionException(TransactionExceptionCode.IO, toe);
        }
    }
}
