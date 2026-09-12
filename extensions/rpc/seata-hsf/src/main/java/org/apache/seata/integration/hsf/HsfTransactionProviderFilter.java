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
import com.taobao.hsf.invocation.filter.ServerFilter;
import com.taobao.hsf.util.concurrent.ListenableFuture;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.integration.rpc.core.TransactionPropagationHandler;

public class HsfTransactionProviderFilter implements ServerFilter {

    @Override
    public ListenableFuture<RPCResult> invoke(InvocationHandler nextHandler, Invocation invocation) throws Throwable {
        String rpcXid = getRpcXid();
        String rpcBranchType = getStringAttachment(RootContext.KEY_BRANCH_TYPE);
        boolean bound = TransactionPropagationHandler.bindProviderContext(rpcXid, rpcBranchType);
        try {
            return nextHandler.invoke(invocation);
        } finally {
            if (bound) {
                TransactionPropagationHandler.unbindProviderContext(rpcXid);
            }
            RPCContext.getServerContext().removeAttachment(RootContext.KEY_XID);
            RPCContext.getServerContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
        }
    }

    private String getRpcXid() {
        String rpcXid = getStringAttachment(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = getStringAttachment(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
    }

    private String getStringAttachment(String key) {
        Object value = RPCContext.getServerContext().getAttachment(key);
        return value == null ? null : value.toString();
    }

    @Override
    public void onResponse(Invocation invocation, RPCResult rpcResult) {}
}
