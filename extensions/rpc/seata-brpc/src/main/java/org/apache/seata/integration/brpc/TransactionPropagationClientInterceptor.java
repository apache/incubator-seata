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

import com.baidu.brpc.interceptor.AbstractInterceptor;
import com.baidu.brpc.interceptor.InterceptorChain;
import com.baidu.brpc.protocol.Request;
import com.baidu.brpc.protocol.Response;
import org.apache.seata.integration.rpc.core.TransactionPropagationHandler;

import java.util.HashMap;
import java.util.Map;

public class TransactionPropagationClientInterceptor extends AbstractInterceptor {

    @Override
    public void aroundProcess(Request brpcRequest, Response brpcResponse, InterceptorChain chain) throws Exception {
        Map<String, String> context = TransactionPropagationHandler.getTransactionPropagationContext();
        if (!context.isEmpty()) {
            Map<String, Object> kvAttachment = brpcRequest.getKvAttachment();
            if (kvAttachment == null) {
                kvAttachment = new HashMap<>();
                brpcRequest.setKvAttachment(kvAttachment);
            }
            kvAttachment.putAll(context);
        }
        try {
            chain.intercept(brpcRequest, brpcResponse);
        } finally {
            Map<String, Object> kvAttachment = brpcRequest.getKvAttachment();
            if (kvAttachment != null) {
                for (String key : context.keySet()) {
                    kvAttachment.remove(key);
                }
            }
        }
    }
}
