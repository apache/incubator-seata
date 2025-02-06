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
package org.apache.seata.server.limit;

import org.apache.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.limit.ratelimit.RateLimiterHandler;

/**
 * LimitRequestDecorator decorate AbstractTransactionRequestToTC to use limiter
 */
public class LimitRequestDecorator extends AbstractTransactionRequestToTC {

    private AbstractTransactionRequestToTC originalRequest;

    private AbstractTransactionRequestHandler requestLimitHandler;

    public LimitRequestDecorator(AbstractTransactionRequestToTC originalRequest) {
        this.originalRequest = originalRequest;

        // create server rate limter
        RateLimiterHandler rateLimiterHandler = RateLimiterHandler.getInstance();
        rateLimiterHandler.setTransactionRequestLimitHandler(null);
        requestLimitHandler = rateLimiterHandler;
    }


    @Override
    public AbstractTransactionResponse handle(RpcContext rpcContext) {
        return requestLimitHandler.handle(originalRequest, rpcContext);
    }

    @Override
    public short getTypeCode() {
        return originalRequest.getTypeCode();
    }
}
