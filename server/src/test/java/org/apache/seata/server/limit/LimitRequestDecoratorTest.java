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
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.limit.ratelimit.RateLimiterHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * LimitRequestDecorator Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LimitRequestDecorator Test")
class LimitRequestDecoratorTest {

    private LimitRequestDecorator decorator;

    @Mock
    private AbstractTransactionRequestToTC mockOriginalRequest;

    @Mock
    private RpcContext mockRpcContext;

    @Mock
    private AbstractTransactionResponse mockResponse;

    @BeforeEach
    void setUp() {
        when(mockOriginalRequest.getTypeCode()).thenReturn((short) 1);
    }

    @Test
    @DisplayName("test getTypeCode returns original request type code")
    void testGetTypeCodeReturnsOriginalRequestTypeCode() {
        try (MockedStatic<RateLimiterHandler> rateLimiterMock = mockStatic(RateLimiterHandler.class)) {
            RateLimiterHandler mockRateLimiter = mock(RateLimiterHandler.class);
            rateLimiterMock.when(RateLimiterHandler::getInstance).thenReturn(mockRateLimiter);
            
            when(mockRateLimiter.handle(mockOriginalRequest, mockRpcContext)).thenReturn(mockResponse);
            
            decorator = new LimitRequestDecorator(mockOriginalRequest);
            
            short typeCode = decorator.getTypeCode();
            assertEquals((short) 1, typeCode);
            verify(mockOriginalRequest).getTypeCode();
        }
    }

    @Test
    @DisplayName("test handle delegates to rate limiter")
    void testHandleDelegatesToRateLimiter() {
        try (MockedStatic<RateLimiterHandler> rateLimiterMock = mockStatic(RateLimiterHandler.class)) {
            RateLimiterHandler mockRateLimiter = mock(RateLimiterHandler.class);
            rateLimiterMock.when(RateLimiterHandler::getInstance).thenReturn(mockRateLimiter);
            
            when(mockRateLimiter.handle(mockOriginalRequest, mockRpcContext)).thenReturn(mockResponse);
            
            decorator = new LimitRequestDecorator(mockOriginalRequest);
            
            AbstractTransactionResponse result = decorator.handle(mockRpcContext);
            
            assertNotNull(result);
            verify(mockRateLimiter).handle(mockOriginalRequest, mockRpcContext);
        }
    }

    @Test
    @DisplayName("test constructor initializes request handler")
    void testConstructorInitializesRequestHandler() {
        try (MockedStatic<RateLimiterHandler> rateLimiterMock = mockStatic(RateLimiterHandler.class)) {
            RateLimiterHandler mockRateLimiter = mock(RateLimiterHandler.class);
            rateLimiterMock.when(RateLimiterHandler::getInstance).thenReturn(mockRateLimiter);
            
            decorator = new LimitRequestDecorator(mockOriginalRequest);
            
            assertNotNull(decorator);
            verify(mockRateLimiter).setTransactionRequestLimitHandler(null);
        }
    }

    @Test
    @DisplayName("test handle with null response")
    void testHandleWithNullResponse() {
        try (MockedStatic<RateLimiterHandler> rateLimiterMock = mockStatic(RateLimiterHandler.class)) {
            RateLimiterHandler mockRateLimiter = mock(RateLimiterHandler.class);
            rateLimiterMock.when(RateLimiterHandler::getInstance).thenReturn(mockRateLimiter);
            
            when(mockRateLimiter.handle(mockOriginalRequest, mockRpcContext)).thenReturn(null);
            
            decorator = new LimitRequestDecorator(mockOriginalRequest);
            
            AbstractTransactionResponse result = decorator.handle(mockRpcContext);
            
            assertNull(result);
        }
    }

    @Test
    @DisplayName("test handle with exception")
    void testHandleWithException() {
        try (MockedStatic<RateLimiterHandler> rateLimiterMock = mockStatic(RateLimiterHandler.class)) {
            RateLimiterHandler mockRateLimiter = mock(RateLimiterHandler.class);
            rateLimiterMock.when(RateLimiterHandler::getInstance).thenReturn(mockRateLimiter);
            
            when(mockRateLimiter.handle(mockOriginalRequest, mockRpcContext))
                    .thenThrow(new RuntimeException("Rate limit exceeded"));
            
            decorator = new LimitRequestDecorator(mockOriginalRequest);
            
            assertThrows(RuntimeException.class, () -> decorator.handle(mockRpcContext));
        }
    }

    @Test
    @DisplayName("test multiple decorators with same original request")
    void testMultipleDecoratorsWithSameOriginalRequest() {
        try (MockedStatic<RateLimiterHandler> rateLimiterMock = mockStatic(RateLimiterHandler.class)) {
            RateLimiterHandler mockRateLimiter = mock(RateLimiterHandler.class);
            rateLimiterMock.when(RateLimiterHandler::getInstance).thenReturn(mockRateLimiter);
            
            when(mockRateLimiter.handle(any(), any())).thenReturn(mockResponse);
            
            LimitRequestDecorator decorator1 = new LimitRequestDecorator(mockOriginalRequest);
            LimitRequestDecorator decorator2 = new LimitRequestDecorator(mockOriginalRequest);
            
            AbstractTransactionResponse result1 = decorator1.handle(mockRpcContext);
            AbstractTransactionResponse result2 = decorator2.handle(mockRpcContext);
            
            assertNotNull(result1);
            assertNotNull(result2);
        }
    }
}

