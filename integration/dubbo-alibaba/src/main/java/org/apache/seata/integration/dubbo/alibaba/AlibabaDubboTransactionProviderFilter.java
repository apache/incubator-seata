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
<<<<<<<< HEAD:integration/dubbo-alibaba/src/main/java/org/apache/seata/integration/dubbo/alibaba/AlibabaDubboTransactionPropagationFilter.java
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.constants.DubboConstants;
========

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.constants.DubboConstants;
import org.apache.seata.core.context.RootContext;
>>>>>>>> upstream/2.x:integration/dubbo-alibaba/src/main/java/org/apache/seata/integration/dubbo/alibaba/AlibabaDubboTransactionProviderFilter.java
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
<<<<<<<< HEAD:integration/dubbo-alibaba/src/main/java/org/apache/seata/integration/dubbo/alibaba/AlibabaDubboTransactionPropagationFilter.java
 * The type Transaction propagation filter.
 *
========
 * The type Alibaba dubbo transaction provider filter.
>>>>>>>> upstream/2.x:integration/dubbo-alibaba/src/main/java/org/apache/seata/integration/dubbo/alibaba/AlibabaDubboTransactionProviderFilter.java
 */
@Activate(group = {DubboConstants.PROVIDER}, order = 100)
public class AlibabaDubboTransactionProviderFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(AlibabaDubboTransactionProviderFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!DubboConstants.ALIBABADUBBO) {
            return invoker.invoke(invocation);
        }
        String rpcXid = getRpcXid();
        String rpcBranchType = RpcContext.getContext().getAttachment(RootContext.KEY_BRANCH_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RpcContext[{}], branchType in RpcContext[{}]", rpcXid, rpcBranchType);
        }
        boolean bind = false;
        if (rpcXid != null) {
            RootContext.bind(rpcXid);
            if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType)) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            bind = true;
        }

        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                BranchType previousBranchType = RootContext.getBranchType();
                String unbindXid = RootContext.unbind();
                if (BranchType.TCC == previousBranchType) {
                    RootContext.unbindBranchType();
                }
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind xid [{}] branchType [{}] from RootContext", unbindXid, previousBranchType);
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn("xid in change during RPC from {} to {},branchType from {} to {}", rpcXid, unbindXid,
                        rpcBranchType != null ? rpcBranchType : BranchType.AT, previousBranchType);
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        LOGGER.warn("bind xid [{}] back to RootContext", unbindXid);
                        if (BranchType.TCC == previousBranchType) {
                            RootContext.bindBranchType(BranchType.TCC);
                            LOGGER.warn("bind branchType [{}] back to RootContext", previousBranchType);
                        }
                    }
                }
            }
            RpcContext.getServerContext().removeAttachment(RootContext.KEY_XID);
            RpcContext.getServerContext().removeAttachment(RootContext.KEY_BRANCH_TYPE);
        }
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
}
