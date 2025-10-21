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
package org.apache.seata.integration.hsf;

import com.taobao.hsf.context.RPCContext;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.invocation.InvocationHandler;
import com.taobao.hsf.invocation.RPCResult;
import com.taobao.hsf.invocation.filter.ClientFilter;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Hsf transaction consumer filter.
 */
public class HsfTransactionConsumerFilter implements ClientFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsfTransactionConsumerFilter.class);

    @Override
    public ListenableFuture<RPCResult> invoke(InvocationHandler nextHandler, Invocation invocation) throws Throwable {
        return doInvoke(nextHandler, invocation);
    }

    private ListenableFuture<RPCResult> doInvoke(InvocationHandler nextHandler, Invocation invocation)
            throws Throwable {
        TransactionContext context = extractTransactionContext();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[{}], branchType in RootContext[{}]", context.xid, context.branchType);
        }

        try {
            propagateTransactionContext(context);
            return nextHandler.invoke(invocation);
        } finally {
            clearTransactionContext();
        }
    }

    private TransactionContext extractTransactionContext() {
        TransactionContext context = new TransactionContext();
        context.xid = RootContext.getXID();
        context.branchType = RootContext.getBranchType();
        return context;
    }

    private void propagateTransactionContext(TransactionContext context) {
        if (context.xid != null) {
            RPCContext.getClientContext().putAttachment(RootContext.KEY_XID, context.xid);
            RPCContext.getClientContext().putAttachment(RootContext.KEY_BRANCH_TYPE, context.branchType.name());

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("transaction context propagated: xid={}, branchType={}", context.xid, context.branchType);
            }
        }
    }

    private void clearTransactionContext() {
        RPCContext.getClientContext().removeAttachment(RootContext.KEY_XID);
        RPCContext.getClientContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("transaction context cleared");
        }
    }

    @Override
    public void onResponse(Invocation invocation, RPCResult rpcResult) {
        // No operation needed
    }

    private static class TransactionContext {
        /**
         * The Xid.
         */
        String xid;
        /**
         * The Branch type.
         */
        BranchType branchType;
    }
}
