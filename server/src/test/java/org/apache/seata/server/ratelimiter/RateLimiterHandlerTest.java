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
package org.apache.seata.server.ratelimiter;

import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.limit.ratelimit.RateLimiter;
import org.apache.seata.server.limit.ratelimit.RateLimiterHandler;
import org.apache.seata.server.limit.ratelimit.TokenBucketLimiter;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * RateLimiterHandlerTest
 */
@SpringBootTest
public class RateLimiterHandlerTest {

    /**
     * Logger for TokenBucketLimiterTest
     **/
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimiterHandlerTest.class);

    private static RateLimiterHandler rateLimiterHandler;

    @Test
    public void testHandlePass() {
        RateLimiter rateLimiter = new TokenBucketLimiter(true, 1,
                10, 10);
        rateLimiterHandler = new RateLimiterHandler(rateLimiter);
        GlobalBeginRequest request = new GlobalBeginRequest();
        RpcContext rpcContext = new RpcContext();
        Assertions.assertThrowsExactly(NullPointerException.class, () -> rateLimiterHandler.handle(request, rpcContext));
    }

    @Test
    public void testHandleNotPass() {
        RateLimiter rateLimiter = new TokenBucketLimiter(true, 1,
                1, 0);
        rateLimiterHandler = new RateLimiterHandler(rateLimiter);
        GlobalBeginRequest request = new GlobalBeginRequest();
        RpcContext rpcContext = new RpcContext();
        Assertions.assertThrowsExactly(NullPointerException.class, () -> rateLimiterHandler.handle(request, rpcContext));
    }

}
