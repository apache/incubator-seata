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

import com.baidu.brpc.RpcContext;
import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * load SEATA xid for brpc request
 *
 * @author mxz0828@163.com
 */
public class TransactionPropagationClientInterceptor extends AbstractInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationClientInterceptor.class);

    @Override
    public void aroundProcess(Request brpcRequest, Response brpcResponse, InterceptorChain chain) throws Exception {

        String xid = RootContext.getXID();
        String rpcXid = getRpcXid();
        Map<String, Object> kvAttachment = brpcRequest.getKvAttachment();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SEATA-BRPC[{}]: xid in RootContext[{}] xid in RpcContext[{}]", RootContext.getBranchType(), xid, rpcXid);
        }

        if (null != xid) {
            if (null == kvAttachment) {
                kvAttachment = new HashMap<>();
            }
            kvAttachment.put(RootContext.KEY_XID, xid);
            if (null != RootContext.getBranchType()) {
                kvAttachment.put(RootContext.KEY_BRANCH_TYPE, RootContext.getBranchType().name());
            }
            brpcRequest.setKvAttachment(kvAttachment);
        }

        try {
            chain.intercept(brpcRequest, brpcResponse);
        } finally {
            Map<String, Object> requestAttachment = brpcRequest.getKvAttachment();
            if (null != requestAttachment) {
                requestAttachment.remove(RootContext.KEY_XID);
                requestAttachment.remove(RootContext.KEY_BRANCH_TYPE);
            }
        }
    }

    private String getRpcXid() {
        RpcContext context = RpcContext.getContext();
        Map<String, Object> requestKvAttachmentMap = context.getRequestKvAttachment();
        if (null == requestKvAttachmentMap) {
            return null;
        }
        return (String) requestKvAttachmentMap.get(RootContext.KEY_XID);
    }
}
