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
package io.seata.integration.hsf;

import com.taobao.hsf.context.RPCContext;
import com.taobao.hsf.invocation.Invocation;
import com.taobao.hsf.invocation.InvocationHandler;
import com.taobao.hsf.invocation.RPCResult;
import com.taobao.hsf.invocation.filter.ClientFilter;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Hsf transaction consumer filter.
 */
public class HsfTransactionConsumerFilter implements ClientFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsfTransactionConsumerFilter.class);

    @Override
    public ListenableFuture<RPCResult> invoke(InvocationHandler nextHandler, Invocation invocation) throws Throwable {
        String xid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[{}], branchType in RootContext[{}]", xid, branchType);
        }
        if (xid != null) {
            RPCContext.getClientContext().putAttachment(RootContext.KEY_XID, xid);
            RPCContext.getClientContext().putAttachment(RootContext.KEY_BRANCH_TYPE, branchType.name());
        }
        try {
            return nextHandler.invoke(invocation);
        } finally {
            RPCContext.getClientContext().removeAttachment(RootContext.KEY_XID);
            RPCContext.getClientContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
        }
    }

    @Override
    public void onResponse(Invocation invocation, RPCResult rpcResult) {

    }
}
