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
import org.apache.seata.integration.rpc.core.TransactionPropagationHandler;

import java.util.Map;

@Spi(scope = Scope.SINGLETON)
@Activation(
        key = {MotanConstants.NODE_TYPE_REFERER},
        sequence = 100)
public class MotanTransactionConsumerFilter implements Filter {

    @Override
    public Response filter(final Caller<?> caller, final Request request) {
        Map<String, String> context = TransactionPropagationHandler.getTransactionPropagationContext();
        if (!context.isEmpty()) {
            request.getAttachments().putAll(context);
        }
        try {
            return caller.call(request);
        } finally {
            for (String key : context.keySet()) {
                request.getAttachments().remove(key);
            }
        }
    }
}
