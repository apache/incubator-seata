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
import org.apache.seata.core.constants.DubboConstants;
import org.apache.seata.integration.rpc.core.TransactionPropagationHandler;

import java.util.Map;

@Activate(
        group = {DubboConstants.CONSUMER},
        order = 100)
public class AlibabaDubboTransactionConsumerFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        if (!DubboConstants.ALIBABADUBBO) {
            return invoker.invoke(invocation);
        }
        Map<String, String> context = TransactionPropagationHandler.getTransactionPropagationContext();
        if (!context.isEmpty()) {
            for (Map.Entry<String, String> entry : context.entrySet()) {
                RpcContext.getContext().setAttachment(entry.getKey(), entry.getValue());
            }
        }
        try {
            return invoker.invoke(invocation);
        } finally {
            for (String key : context.keySet()) {
                RpcContext.getContext().removeAttachment(key);
            }
        }
    }
}
