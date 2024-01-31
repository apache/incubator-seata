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
package org.apache.seata.integration.brpc;

import java.util.Map;

import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.integration.rpc.core.ProviderRpcFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>1. load SEATA xid from brpc request in handleRequest</p>
 * <p>2. clear SEATA xid when brpc request done in aroundProcess</p>
 *
 */
public class TransactionPropagationServerInterceptor extends AbstractInterceptor implements ProviderRpcFilter<Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationServerInterceptor.class);

    @Override
    public boolean handleRequest(Request request) {

        Map<String, String> rpcContexts = getRpcContexts(request);
        String xid = RootContext.getXID();
        String rpcXid = getXidFromContexts(rpcContexts);
        if (null == xid) {
            if (null != rpcXid) {
                bindRequestToContexts(rpcContexts);
                if (LOGGER.isInfoEnabled()) {
                    LOGGER.info("SEATA-BRPC bind {} to RootContext", getJsonContext(rpcContexts));
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
            Map<String, String> rootContexts = cleanRootContexts();
            Map<String, String> rpcContexts = getRpcContexts(brpcRequest);
            String rpcXid = getXidFromContexts(rpcContexts);
            String xid = getXidFromContexts(rootContexts);
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("SEATA-BRPC unbind {} from RootContext", getJsonContext(rootContexts));
            }
            if (null != rpcXid && !rpcXid.equalsIgnoreCase(xid)) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("SEATA-BRPC context changed during RPC from {} to {},will be reset.", getJsonContext(rpcContexts), getJsonContext(rootContexts));
                }
                resetRootContexts(rootContexts);
            }
        }
    }

    @Override
    public String getRpcContext(Request rpcContext, String key) {
        if (null == rpcContext.getKvAttachment()) {
            return null;
        }
        Object value = rpcContext.getKvAttachment().get(key);
        return value == null ? null : value.toString();
    }
}
