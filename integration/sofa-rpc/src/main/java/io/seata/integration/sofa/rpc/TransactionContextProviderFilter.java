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
package io.seata.integration.sofa.rpc;

import com.alipay.sofa.rpc.context.RpcInternalContext;
import com.alipay.sofa.rpc.core.exception.SofaRpcException;
import com.alipay.sofa.rpc.core.request.SofaRequest;
import com.alipay.sofa.rpc.core.response.SofaResponse;
import com.alipay.sofa.rpc.ext.Extension;
import com.alipay.sofa.rpc.filter.AutoActive;
import com.alipay.sofa.rpc.filter.Filter;
import com.alipay.sofa.rpc.filter.FilterInvoker;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransactionContext on provider side.
 * 
 * @author Geng Zhang
 * @since 0.6.0
 */
@Extension(value = "transactionContextProvider")
@AutoActive(providerSide = true)
public class TransactionContextProviderFilter extends Filter {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionContextProviderFilter.class);

    @Override
    public SofaResponse invoke(FilterInvoker filterInvoker, SofaRequest sofaRequest) throws SofaRpcException {
        String xid = RootContext.getXID();
        String rpcXid = getRpcXid(sofaRequest);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("xid in RootContext[" + xid + "] xid in RpcContext[" + rpcXid + "]");
        }
        boolean bind = false;
        if (xid != null) {
            RpcInternalContext.getContext().setAttachment(RootContext.KEY_XID, xid);
        } else {
            if (rpcXid != null) {
                RootContext.bind(rpcXid);
                bind = true;
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("bind[" + rpcXid + "] to RootContext");
                }
            }
        }
        try {
            return filterInvoker.invoke(sofaRequest);
        } finally {
            if (bind) {
                String unbindXid = RootContext.unbind();
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("unbind[" + unbindXid + "] from RootContext");
                }
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("xid in change during RPC from " + rpcXid + " to " + unbindXid);
                    }
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("bind [" + unbindXid + "] back to RootContext");
                        }
                    }
                }
            }
        }
    }

    /**
     * get rpc xid
     * @return
     */
    private String getRpcXid(SofaRequest sofaRequest) {
        String rpcXid = (String) sofaRequest.getRequestProp(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = (String) sofaRequest.getRequestProp(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
    }

}
