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
package org.apache.seata.integration.dubbo.alibaba;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.constants.DubboConstants;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Alibaba dubbo transaction provider filter.
 */
@Activate(
        group = {DubboConstants.PROVIDER},
        order = 100)
public class AlibabaDubboTransactionProviderFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlibabaDubboTransactionProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!DubboConstants.ALIBABADUBBO) {
            return invoker.invoke(invocation);
        }

        return doInvoke(invoker, invocation);
    }

    private Result doInvoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String rpcXid = getRpcXid();
        String rpcBranchType = RpcContext.getContext().getAttachment(RootContext.KEY_BRANCH_TYPE);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RpcContext[{}], branchType in RpcContext[{}]", rpcXid, rpcBranchType);
        }

        TransactionContextBinding binding = bindTransactionContext(rpcXid, rpcBranchType);

        try {
            return invoker.invoke(invocation);
        } finally {
            unbindTransactionContext(binding);
            clearServerContextAttachments();
        }
    }

    private TransactionContextBinding bindTransactionContext(String rpcXid, String rpcBranchType) {
        TransactionContextBinding binding = new TransactionContextBinding();

        if (rpcXid != null) {
            RootContext.bind(rpcXid);
            binding.wasBound = true;
            binding.bindXid = rpcXid;

            if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType)) {
                RootContext.bindBranchType(BranchType.TCC);
                binding.wasBranchTypeBound = true;
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bind xid [{}] branchType [{}] to RootContext", rpcXid, rpcBranchType);
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
                    binding.bindBranchType != null ? binding.bindBranchType : BranchType.AT,
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
        RpcContext.getServerContext().removeAttachment(RootContext.KEY_XID);
        RpcContext.getServerContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
    }

    /**
     * get rpc xid
     *
     * @return
     */
    private String getRpcXid() {
        String rpcXid = RpcContext.getContext().getAttachment(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = RpcContext.getContext().getAttachment(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
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
