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
package org.apache.seata.integration.motan;

import com.weibo.api.motan.common.MotanConstants;
import com.weibo.api.motan.core.extension.Activation;
import com.weibo.api.motan.core.extension.Scope;
import com.weibo.api.motan.core.extension.Spi;
import com.weibo.api.motan.filter.Filter;
import com.weibo.api.motan.rpc.Caller;
import com.weibo.api.motan.rpc.Request;
import com.weibo.api.motan.rpc.Response;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@Spi(scope = Scope.SINGLETON)
@Activation(key = {MotanConstants.NODE_TYPE_SERVICE, MotanConstants.NODE_TYPE_REFERER}, sequence = 100)
public class MotanTransactionFilter implements Filter {
    private static final Logger LOGGER = LoggerFactory.getLogger(MotanTransactionFilter.class);

    public MotanTransactionFilter(){}
    @Override
    public Response filter(final Caller<?> caller, final Request request) {
        String currentXid = RootContext.getXID();
        BranchType branchType = RootContext.getBranchType();
        String requestXid = getRpcXid(request);
        String rpcBranchType = getBranchType(request);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("context in RootContext[{},{}], context in RpcContext[{},{}]", currentXid, branchType, requestXid, rpcBranchType);
        }
        boolean bind = false;
        if (currentXid != null) {
            request.getAttachments().put(RootContext.KEY_XID, currentXid);
            request.getAttachments().put(RootContext.KEY_BRANCH_TYPE, branchType.name());

        } else if (requestXid != null) {
            RootContext.bind(requestXid);
            if (StringUtils.equals(BranchType.TCC.name(), rpcBranchType)) {
                RootContext.bindBranchType(BranchType.TCC);
            }
            bind = true;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("bind [{}] to RootContext", requestXid);
            }

        }
        try {
            return caller.call(request);
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
                if (!requestXid.equalsIgnoreCase(unbindXid)) {
                    LOGGER.warn("xid has changed, during RPC from [{}] to [{}]", requestXid, unbindXid);
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        LOGGER.warn("bind [{}}] back to RootContext",unbindXid);
                        if (BranchType.TCC == previousBranchType) {
                            RootContext.bindBranchType(BranchType.TCC);
                            LOGGER.warn("bind branchType [{}] back to RootContext", previousBranchType);
                        }
                    }
                }
            }
        }
    }

    /**
     * get rpc xid
     * @param request
     * @return
     */
    private String getRpcXid(Request request) {
        String rpcXid = request.getAttachments().get(RootContext.KEY_XID);
        if (rpcXid == null) {
            rpcXid = request.getAttachments().get(RootContext.KEY_XID.toLowerCase());
        }
        return rpcXid;
    }

    private String getBranchType(Request request) {
        return request.getAttachments().get(RootContext.KEY_BRANCH_TYPE);
    }

}
