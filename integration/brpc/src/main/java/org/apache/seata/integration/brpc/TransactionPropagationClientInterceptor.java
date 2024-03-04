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

import java.util.HashMap;
import java.util.Map;

import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import org.apache.seata.integration.rpc.core.ConsumerRpcFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * load SEATA xid for brpc request
 *
 */
public class TransactionPropagationClientInterceptor extends AbstractInterceptor implements ConsumerRpcFilter<Request> {

    private static final Logger LOGGER = LoggerFactory.getLogger(TransactionPropagationClientInterceptor.class);

    @Override
    public void aroundProcess(Request brpcRequest, Response brpcResponse, InterceptorChain chain) throws Exception {

        Map<String, String> rootContexts = getRootContexts();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("SEATA-BRPC context:{}", getJsonContext(rootContexts));
        }

        if (null != getXidFromRootContexts(rootContexts)) {
            bindContextsToRequest(brpcRequest, rootContexts);
        }

        try {
            chain.intercept(brpcRequest, brpcResponse);
        } finally {
            cleanRequestContexts(brpcRequest, rootContexts);
        }
    }

    @Override
    public void bindContextToRequest(Request rpcRequest, String key, String value) {
        Map<String, Object> kvAttachment = rpcRequest.getKvAttachment();
        if (null == kvAttachment) {
            kvAttachment = new HashMap<>();
            rpcRequest.setKvAttachment(kvAttachment);
        }
        kvAttachment.put(key, value);
    }

    @Override
    public void cleanRequestContext(Request rpcRequest, String key) {
        Map<String, Object> requestAttachment = rpcRequest.getKvAttachment();
        if (null != requestAttachment) {
            requestAttachment.remove(key);
        }
    }
}
