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
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Hsf transaction provider filter.
 */
public class HsfTransactionProviderFilter implements ServerFilter {

    private static final Logger LOGGER = LoggerFactory.getLogger(HsfTransactionProviderFilter.class);

    @Override
    public ListenableFuture<RPCResult> invoke(InvocationHandler nextHandler, Invocation invocation) throws Throwable {
        return doInvoke(nextHandler, invocation);
    }

    private ListenableFuture<RPCResult> doInvoke(InvocationHandler nextHandler, Invocation invocation)
            throws Throwable {
        RpcTransactionContext rpcContext = extractRpcTransactionContext();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(
                    "xid in RpcContext[{}], branchType in RpcContext[{}]", rpcContext.rpcXid, rpcContext.rpcBranchType);
        }

        TransactionContextBinding binding = bindTransactionContext(rpcContext);

        try {
            return nextHandler.invoke(invocation);
        } finally {
            unbindTransactionContext(binding);
            clearServerContextAttachments();
        }
    }

    private RpcTransactionContext extractRpcTransactionContext() {
        RpcTransactionContext context = new RpcTransactionContext();
        context.rpcXid = RPCContext.getServerContext().getAttachment(RootContext.KEY_XID);
        context.rpcBranchType = RPCContext.getServerContext().getAttachment(RootContext.KEY_BRANCH_TYPE);
        return context;
    }

    private TransactionContextBinding bindTransactionContext(RpcTransactionContext rpcContext) {
        TransactionContextBinding binding = new TransactionContextBinding();

        if (rpcContext.rpcXid != null) {
            String xidStr = rpcContext.rpcXid.toString();
            RootContext.bind(xidStr);
            binding.wasBound = true;
            binding.bindXid = xidStr;

            if (rpcContext.rpcBranchType != null
                    && StringUtils.equals(BranchType.TCC.name(), rpcContext.rpcBranchType.toString())) {
                RootContext.bindBranchType(BranchType.TCC);
                binding.wasBranchTypeBound = true;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(
                        "bind xid [{}] branchType [{}] to RootContext", rpcContext.rpcXid, rpcContext.rpcBranchType);
            }
        }

        return binding;
    }

    private void unbindTransactionContext(TransactionContextBinding binding) {
        if (!binding.wasBound) {
            return;
        }

        BranchType previousBranchType = RootContext.getBranchType();
        String unbindXid = RootContext.unbind();
        binding.unbindXid = unbindXid;
        binding.unbindBranchType = previousBranchType;

        if (binding.wasBranchTypeBound && BranchType.TCC == previousBranchType) {
            RootContext.unbindBranchType();
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("unbind xid [{}] branchType [{}] from RootContext", unbindXid, previousBranchType);
        }

        handleXidChange(binding);
    }

    private void handleXidChange(TransactionContextBinding binding) {
        if (!binding.bindXid.equalsIgnoreCase(binding.unbindXid)) {
            LOGGER.warn(
                    "xid in change during RPC from {} to {},branchType from {} to {}",
                    binding.bindXid,
                    binding.unbindXid,
                    binding.bindBranchType != null ? binding.bindBranchType : "AT",
                    binding.unbindBranchType);

            if (binding.unbindXid != null) {
                restoreTransactionContext(binding);
            }
        }
    }

    private void restoreTransactionContext(TransactionContextBinding binding) {
        RootContext.bind(binding.unbindXid);
        LOGGER.warn("bind xid [{}] back to RootContext", binding.unbindXid);

        if (BranchType.TCC == binding.unbindBranchType) {
            RootContext.bindBranchType(BranchType.TCC);
            LOGGER.warn("bind branchType [{}] back to RootContext", binding.unbindBranchType);
        }
    }

    private void clearServerContextAttachments() {
        RPCContext.getServerContext().removeAttachment(RootContext.KEY_XID);
        RPCContext.getServerContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
    }

    @Override
    public void onResponse(Invocation invocation, RPCResult rpcResult) {
        // No operation needed
    }

    private static class RpcTransactionContext {
        /**
         * The Rpc xid.
         */
        Object rpcXid;
        /**
         * The Rpc branch type.
         */
        Object rpcBranchType;
    }

    private static class TransactionContextBinding {
        /**
         * The Was bound.
         */
        boolean wasBound = false;
        /**
         * The Was branch type bound.
         */
        boolean wasBranchTypeBound = false;
        /**
         * The Bind xid.
         */
        String bindXid;
        /**
         * The Bind branch type.
         */
        String bindBranchType;
        /**
         * The Unbind xid.
         */
        String unbindXid;
        /**
         * The Unbind branch type.
         */
        BranchType unbindBranchType;
    }
}
