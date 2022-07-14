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
package io.seata.integration.dubbo3;

import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.BaseFilter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcException;
import org.apache.dubbo.rpc.cluster.filter.ClusterFilter;

import io.seata.core.constants.DubboConstants;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction propagation consumer filter.
 *
 * @author albumenk
 */
@Activate(group = DubboConstants.CONSUMER)
public class ApacheDubbo3TransactionPropagationConsumerFilter implements ClusterFilter, BaseFilter {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApacheDubbo3TransactionPropagationConsumerFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String xid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Client side xid in RootContext[{}]", xid);
        }
        if (xid != null) {
            invocation.setAttachment(RootContext.KEY_XID, xid);
            if (branchType != null) {
                invocation.setAttachment(RootContext.KEY_BRANCH_TYPE, branchType.name());
            }
        }
        return invoker.invoke(invocation);
    }
}
