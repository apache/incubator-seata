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
package io.seata.integration.dubbo.alibaba;

import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;

import io.seata.core.constants.DubboConstants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Alibaba dubbo transaction consumer filter.
 */
@Activate(group = {DubboConstants.CONSUMER}, order = 100)
public class AlibabaDubboTransactionConsumerFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlibabaDubboTransactionConsumerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!DubboConstants.ALIBABADUBBO) {
            return invoker.invoke(invocation);
        }
        String xid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("consumer xid in RootContext[{}], branchType in RootContext[{}]", xid, branchType);
        }
        if (xid != null) {
            RpcContext.getContext().setAttachment(RootContext.KEY_XID, xid);
            RpcContext.getContext().setAttachment(RootContext.KEY_BRANCH_TYPE, branchType.name());
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            RpcContext.getContext().removeAttachment(RootContext.KEY_XID);
            RpcContext.getContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
        }
    }
}
