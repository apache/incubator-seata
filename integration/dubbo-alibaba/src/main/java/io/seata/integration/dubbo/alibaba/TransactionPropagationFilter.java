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

import com.alibaba.dubbo.common.Constants;
import com.alibaba.dubbo.common.extension.Activate;
import com.alibaba.dubbo.rpc.Filter;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcContext;
import com.alibaba.dubbo.rpc.RpcException;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type Transaction propagation filter.
 *
 * @author sharajava
 */
@Activate(group = {Constants.PROVIDER, Constants.CONSUMER}, order = 100)
public class TransactionPropagationFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationFilter.class);

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        String xid = RootContext.getXID();
        String xidFilterType = RootContext.getXIDFilterType();

        String rpcXid = RpcContext.getContext().getAttachment(RootContext.KEY_XID);
        String rpcXidFilterType = RpcContext.getContext().getAttachment(RootContext.KEY_XID_FILTER_TYPE);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[" + xid + "] xid in RpcContext[" + rpcXid + "]");
        }
        boolean bind = false;
        if (xid != null) {
            RpcContext.getContext().setAttachment(RootContext.KEY_XID, xid);
            RpcContext.getContext().setAttachment(RootContext.KEY_XID_FILTER_TYPE, xidFilterType);
        } else {
            if (rpcXid != null) {
                RootContext.bind(rpcXid);
                RootContext.bindFilterType(rpcXidFilterType);
                bind = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind[{}}] to RootContext", rpcXid);
                    LOGGER.debug("bind filterType[{}}] to RootContext", rpcXidFilterType);
                }
            }
        }
        try {
            return invoker.invoke(invocation);

        } finally {
            if (bind) {
                String unbindXid = RootContext.unbind();
                String unbindFilterType = RootContext.unbindFilterType();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind[{}}] from RootContext", unbindXid);
                    LOGGER.debug("unbind filterType[{}}] from RootContext", unbindFilterType);
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn("xid in change during RPC from {} to {}", rpcXid, unbindXid);
                    LOGGER.warn("xidFilterType in change during RPC from {} to {}", rpcXidFilterType, unbindFilterType);
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        RootContext.bindFilterType(unbindFilterType);
                        LOGGER.warn("bind [{}}] back to RootContext", unbindXid);
                        LOGGER.warn("bind filterType [{}}] back to RootContext", unbindFilterType);
                    }
                }
            }
        }
    }
}
