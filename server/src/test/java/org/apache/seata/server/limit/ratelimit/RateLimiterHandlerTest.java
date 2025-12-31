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
package org.apache.seata.server.limit.ratelimit;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.transaction.AbstractTransactionRequestToTC;
import org.apache.seata.core.protocol.transaction.AbstractTransactionResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.limit.AbstractTransactionRequestHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RateLimiterHandler Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimiterHandler Test")
class RateLimiterHandlerTest {

    private RateLimiterHandler handler;

    @Mock
    private RateLimiter mockRateLimiter;

    @Mock
    private AbstractTransactionRequestToTC mockRequest;

    @Mock
    private RpcContext mockRpcContext;

    @Mock
    private AbstractTransactionResponse mockResponse;

    @Mock
    private RateLimiterHandlerConfig mockConfig;

    @BeforeEach
    void setUp() {
        when(mockRateLimiter.obtainConfig()).thenReturn(mockConfig);
        handler = new RateLimiterHandler(mockRateLimiter);
    }

    @Test
    @DisplayName("test handle when rate limiter disabled")
    void testHandleWhenRateLimiterDisabled() {
        when(mockRateLimiter.isEnable()).thenReturn(false);
        when(mockRequest.getTypeCode()).thenReturn((short)1);

        // Mock the next() method behavior
        handler.setTransactionRequestLimitHandler(null);

        // Should pass through when disabled
        handler.handle(mockRequest, mockRpcContext);

        verify(mockRateLimiter).isEnable();
    }

    @Test
    @DisplayName("test handle GlobalBegin when rate limit passed")
    void testHandleGlobalBeginWhenRateLimitPassed() {
        GlobalBeginRequest request = new GlobalBeginRequest();
        when(mockRateLimiter.isEnable()).thenReturn(true);
        when(mockRateLimiter.canPass()).thenReturn(true);
        when(mockRequest.getTypeCode()).thenReturn((short)1);

        // Mock next handler
        AbstractTransactionRequestHandler nextHandler = mock(AbstractTransactionRequestHandler.class);
        handler.setTransactionRequestLimitHandler(nextHandler);

        verify(mockRateLimiter).isEnable();
    }

    @Test
    @DisplayName("test handle GlobalBegin when rate limit exceeded")
    void testHandleGlobalBeginWhenRateLimitExceeded() {
        GlobalBeginRequest request = new GlobalBeginRequest();

        when(mockRateLimiter.isEnable()).thenReturn(true);
        when(mockRateLimiter.canPass()).thenReturn(false);
        when(mockRpcContext.getApplicationId()).thenReturn("testApp");

        AbstractTransactionResponse response = handler.handle(request, mockRpcContext);

        assertNotNull(response);
        assertEquals(ResultCode.Failed, response.getResultCode());
    }

    @Test
    @DisplayName("test onChangeEvent with RATE_LIMIT_ENABLE")
    void testOnChangeEventWithRateLimitEnable() {
        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn(ConfigurationKeys.RATE_LIMIT_ENABLE);
        when(event.getNewValue()).thenReturn("true");

        handler.onChangeEvent(event);

        verify(mockConfig).setEnable(true);
    }

    @Test
    @DisplayName("test onChangeEvent with RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND")
    void testOnChangeEventWithBucketTokenNumPerSecond() {
        when(mockConfig.getBucketTokenNumPerSecond()).thenReturn(100);

        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_NUM_PER_SECOND);
        when(event.getNewValue()).thenReturn("200");

        handler.onChangeEvent(event);

        verify(mockConfig).setBucketTokenNumPerSecond(200);
    }

    @Test
    @DisplayName("test onChangeEvent with RATE_LIMIT_BUCKET_TOKEN_MAX_NUM")
    void testOnChangeEventWithBucketTokenMaxNum() {
        when(mockConfig.getBucketTokenMaxNum()).thenReturn(1000);

        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_MAX_NUM);
        when(event.getNewValue()).thenReturn("2000");

        handler.onChangeEvent(event);

        verify(mockConfig).setBucketTokenMaxNum(2000);
    }

    @Test
    @DisplayName("test onChangeEvent with RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM")
    void testOnChangeEventWithBucketTokenInitialNum() {
        when(mockConfig.getBucketTokenInitialNum()).thenReturn(500);

        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn(ConfigurationKeys.RATE_LIMIT_BUCKET_TOKEN_INITIAL_NUM);
        when(event.getNewValue()).thenReturn("1000");

        handler.onChangeEvent(event);

        verify(mockConfig).setBucketTokenInitialNum(1000);
    }

    @Test
    @DisplayName("test onChangeEvent with unknown dataId")
    void testOnChangeEventWithUnknownDataId() {
        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn("unknown.key");
        when(event.getNewValue()).thenReturn("value");

        // Should not throw exception
        handler.onChangeEvent(event);
    }

    @Test
    @DisplayName("test constructor with custom rate limiter")
    void testConstructorWithCustomRateLimiter() {
        assertNotNull(handler);
    }

    @Test
    @DisplayName("test rate limiter reInit after config change")
    void testRateLimiterReInitAfterConfigChange() {
        ConfigurationChangeEvent event = mock(ConfigurationChangeEvent.class);
        when(event.getDataId()).thenReturn(ConfigurationKeys.RATE_LIMIT_ENABLE);
        when(event.getNewValue()).thenReturn("true");

        handler.onChangeEvent(event);

        verify(mockRateLimiter).reInit(mockConfig);
    }

    @Test
    @DisplayName("test handle with null request")
    void testHandleWithNullRequest() {
        assertThrows(NullPointerException.class, () -> handler.handle(null, mockRpcContext));
    }
}

