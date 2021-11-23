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
package io.seata.integration.brpc;

import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import io.seata.core.context.RootContext;
import io.seata.core.model.BranchType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * <p>1. load SEATA xid from brpc request in handleRequest</p>
 * <p>2. clear SEATA xid when brpc request done in aroundProcess</p>
 *
 * @author mxz0828@163.com
 */
public class TransactionPropagationServerInterceptor extends AbstractInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationServerInterceptor.class);

    @Override
    public boolean handleRequest(Request request) {

        String branchType = getRpcBranchType(request);
        String xid = RootContext.getXID();
        String rpcXid = getRpcXid(request);
        if (null == xid) {
            if (null != rpcXid) {
                RootContext.bind(rpcXid);
                RootContext.bindBranchType(BranchType.valueOf(branchType));
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("SEATA-BRPC[{}], bind [{}] to RootContext", branchType, rpcXid);
                }
            }
        }

        return super.handleRequest(request);
    }

    @Override
    public void aroundProcess(Request brpcRequest, Response brpcResponse, InterceptorChain chain) throws Exception {

        try {
            chain.intercept(brpcRequest, brpcResponse);
        } finally {
            String unbindXid = RootContext.unbind();
            String rpcXid = getRpcXid(brpcRequest);
            BranchType branchType = RootContext.unbindBranchType();
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("SEATA-BRPC[{}]: unbind[{}] from RootContext", branchType, unbindXid);
            }
            if (null != rpcXid) {
                if (!rpcXid.equalsIgnoreCase(unbindXid)) {
                    if (LOGGER.isWarnEnabled()) {
                        LOGGER.warn("SEATA-BRPC[{}]: xid in change during RPC from [{}] to [{}]", branchType, rpcXid, unbindXid);
                    }
                    if (unbindXid != null) {
                        RootContext.bind(unbindXid);
                        if (LOGGER.isWarnEnabled()) {
                            LOGGER.warn("SEATA-BRPC[{}]: bind [{}] back to RootContext", branchType, unbindXid);
                        }
                    }
                }
            }
        }
    }

    private String getRpcXid(Request brpcRequest) {

        Map<String, Object> kvAttachment = brpcRequest.getKvAttachment();
        if (null == kvAttachment) {
            return null;
        }
        return (String) kvAttachment.get(RootContext.KEY_XID);
    }

    private String getRpcBranchType(Request brpcRequest) {
        Map<String, Object> kvAttachment = brpcRequest.getKvAttachment();
        if (null == kvAttachment) {
            return null;
        }
        return (String) kvAttachment.get(RootContext.KEY_BRANCH_TYPE);
    }
}
