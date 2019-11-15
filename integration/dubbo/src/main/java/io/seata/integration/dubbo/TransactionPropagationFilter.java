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
package io.seata.integration.dubbo;

import io.seata.core.context.RootContext;
import org.apache.dubbo.common.Constants;
import org.apache.dubbo.common.extension.Activate;
import org.apache.dubbo.rpc.Filter;
import org.apache.dubbo.rpc.Invocation;
import org.apache.dubbo.rpc.Invoker;
import org.apache.dubbo.rpc.Result;
import org.apache.dubbo.rpc.RpcContext;
import org.apache.dubbo.rpc.RpcException;
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
        String xidInterceptorType = RootContext.getXIDInterceptorType();
        String xidFilterType = RootContext.getXIDFilterType();

        String rpcXid = getRpcXid();
        String rpcXidInterceptorType = RpcContext.getContext().getAttachment(RootContext.KEY_XID_INTERCEPTOR_TYPE);
        String rpcXidFilterType = RpcContext.getContext().getAttachment(RootContext.KEY_XID_FILTER_TYPE);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[{}] xid in RpcContext[{}]", xid, rpcXid);
        }
        boolean bind = false;
        if (xid != null) {
            RpcContext.getContext().setAttachment(RootContext.KEY_XID, xid);
            RpcContext.getContext().setAttachment(RootContext.KEY_XID_INTERCEPTOR_TYPE, xidInterceptorType);
            RpcContext.getContext().setAttachment(RootContext.KEY_XID_FILTER_TYPE, xidFilterType);
        } else {
            if (rpcXid != null) {
                RootContext.bind(rpcXid);
                RootContext.bindInterceptorType(rpcXidInterceptorType);
                RootContext.bindFilterType(rpcXidFilterType);
                bind = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind[{}] interceptorType[{}] filterType[{}] to RootContext", rpcXid, rpcXidInterceptorType, rpcXidFilterType);
                }
            }
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            if (bind) {
                String unbindXid = RootContext.unbind();
                String unbindInterceptorType = RootContext.unbindInterceptorType();
                String unbindFilterType = RootContext.unbindFilterType();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind[{}] interceptorType[{}] filterType[{}] from RootContext", unbindXid, unbindInterceptorType);
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn("xid in change during RPC from {} to {}, xidInterceptorType from {} to {} , xidFilterType from {} to {} ", rpcXid, unbindXid, rpcXidFilterType, unbindFilterType);
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        RootContext.bindInterceptorType(unbindInterceptorType);
                        RootContext.bindFilterType(unbindFilterType);
                        LOGGER.warn("bind [{}] interceptorType[{}] filterType[{}] back to RootContext", unbindXid, unbindInterceptorType, unbindFilterType);
                    }
                }
            }
        }
    }

    /**
     * get rpc xid
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
