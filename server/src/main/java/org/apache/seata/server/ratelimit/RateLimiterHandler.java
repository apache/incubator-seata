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
package org.apache.seata.server.ratelimit;

import org.apache.seata.common.XID;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.metrics.MetricsPublisher;

/**
 * RateLimiterHandler
 */
public class RateLimiterHandler {

    private static volatile RateLimiterHandler instance;

    private final RateLimiter rateLimiter;

    public RateLimiterHandler(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    private RateLimiterHandler() {
        rateLimiter = EnhancedServiceLoader.load(RateLimiter.class);
    }

    public static RateLimiterHandler getInstance() {
        if (instance == null) {
            synchronized (RateLimiterHandler.class) {
                if (instance == null) {
                    instance = new RateLimiterHandler();
                }
            }
        }
        return instance;
    }

    public AbstractResultMessage handle(AbstractMessage request, RpcContext rpcContext) {
        if (request instanceof GlobalBeginRequest) {
            if (!rateLimiter.canPass()) {
                GlobalBeginResponse response = new GlobalBeginResponse();
                response.setTransactionExceptionCode(TransactionExceptionCode.BeginFailedRateLimited);
                response.setResultCode(ResultCode.RateLimited);
                RateLimitInfo rateLimitInfo = RateLimitInfo.generateRateLimitInfo(rpcContext.getApplicationId(),
                        RateLimitInfo.GLOBAL_BEGIN_FAILED, rpcContext.getClientId(), XID.getIpAddressAndPort());
                MetricsPublisher.postRateLimitEvent(rateLimitInfo);
                response.setMsg(String.format("TransactionException[rate limit exception, rate limit info: %s]", rateLimitInfo));
                return response;
            }
        }
        return null;
    }
}
