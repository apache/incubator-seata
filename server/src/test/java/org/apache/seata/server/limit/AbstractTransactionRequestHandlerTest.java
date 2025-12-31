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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * AbstractTransactionRequestHandler Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AbstractTransactionRequestHandler Test")
class AbstractTransactionRequestHandlerTest {

    private TestTransactionRequestHandler handler;
    private TestTransactionRequestHandler nextHandler;

    @Mock
    private AbstractTransactionRequestToTC mockRequest;

    @Mock
    private RpcContext mockRpcContext;

    @Mock
    private AbstractTransactionResponse mockResponse;

    private static class TestTransactionRequestHandler extends AbstractTransactionRequestHandler {
        @Override
        public AbstractTransactionResponse handle(AbstractTransactionRequestToTC originRequest, RpcContext context) {
            return next(originRequest, context);
        }
    }

    @BeforeEach
    void setUp() {
        handler = new TestTransactionRequestHandler();
        nextHandler = new TestTransactionRequestHandler();
    }

    @Test
    @DisplayName("test next handler without chaining")
    void testNextHandlerWithoutChaining() {
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        AbstractTransactionResponse result = handler.handle(mockRequest, mockRpcContext);
        
        assertNotNull(result);
        verify(mockRequest).handle(mockRpcContext);
    }

    @Test
    @DisplayName("test next handler with chaining")
    void testNextHandlerWithChaining() {
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        handler.setTransactionRequestLimitHandler(nextHandler);
        
        AbstractTransactionResponse result = handler.handle(mockRequest, mockRpcContext);
        
        assertNotNull(result);
        verify(mockRequest).handle(mockRpcContext);
    }

    @Test
    @DisplayName("test setTransactionRequestLimitHandler")
    void testSetTransactionRequestLimitHandler() {
        assertNull(handler.abstractTransactionRequestHandler);
        
        handler.setTransactionRequestLimitHandler(nextHandler);
        
        assertNotNull(handler.abstractTransactionRequestHandler);
        assertSame(handler.abstractTransactionRequestHandler, nextHandler);
    }

    @Test
    @DisplayName("test next with null handler")
    void testNextWithNullHandler() {
        handler.setTransactionRequestLimitHandler(null);
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        AbstractTransactionResponse result = handler.next(mockRequest, mockRpcContext);
        
        assertNotNull(result);
        verify(mockRequest).handle(mockRpcContext);
    }

    @Test
    @DisplayName("test handler chain delegation")
    void testHandlerChainDelegation() {
        AbstractTransactionRequestHandler handler1 = new TestTransactionRequestHandler();
        AbstractTransactionRequestHandler handler2 = new TestTransactionRequestHandler();
        AbstractTransactionRequestHandler handler3 = new TestTransactionRequestHandler();
        
        handler1.setTransactionRequestLimitHandler(handler2);
        handler2.setTransactionRequestLimitHandler(handler3);
        
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        AbstractTransactionResponse result = handler1.handle(mockRequest, mockRpcContext);
        
        assertNotNull(result);
    }

    @Test
    @DisplayName("test handler multiple setTransactionRequestLimitHandler calls")
    void testMultipleSetTransactionRequestLimitHandlerCalls() {
        TestTransactionRequestHandler handler1 = new TestTransactionRequestHandler();
        TestTransactionRequestHandler handler2 = new TestTransactionRequestHandler();
        TestTransactionRequestHandler handler3 = new TestTransactionRequestHandler();
        
        handler.setTransactionRequestLimitHandler(handler1);
        assertEquals(handler1, handler.abstractTransactionRequestHandler);
        
        handler.setTransactionRequestLimitHandler(handler2);
        assertEquals(handler2, handler.abstractTransactionRequestHandler);
        
        handler.setTransactionRequestLimitHandler(handler3);
        assertEquals(handler3, handler.abstractTransactionRequestHandler);
    }

    @Test
    @DisplayName("test handle delegates to next")
    void testHandleDelegatesToNext() {
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        AbstractTransactionResponse result = handler.handle(mockRequest, mockRpcContext);
        
        assertEquals(mockResponse, result);
    }

    @Test
    @DisplayName("test handler is abstract or can be extended")
    void testHandlerIsAbstractOrExtendable() {
        // Verify the handler instance is a subclass of AbstractTransactionRequestHandler
        assertTrue(handler instanceof AbstractTransactionRequestHandler,
                "Handler should be an instance of AbstractTransactionRequestHandler");
        
        // Verify it can be extended by checking TestTransactionRequestHandler
        assertTrue(TestTransactionRequestHandler.class.getSuperclass()
                   .equals(AbstractTransactionRequestHandler.class),
                "TestTransactionRequestHandler should extend AbstractTransactionRequestHandler");
    }

    @Test
    @DisplayName("test next method returns response from request")
    void testNextMethodReturnsResponseFromRequest() {
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        AbstractTransactionResponse result = handler.next(mockRequest, mockRpcContext);
        
        assertEquals(mockResponse, result);
        verify(mockRequest).handle(mockRpcContext);
    }

    @Test
    @DisplayName("test next method with chained handler")
    void testNextMethodWithChainedHandler() {
        handler.setTransactionRequestLimitHandler(nextHandler);
        when(mockRequest.handle(mockRpcContext)).thenReturn(mockResponse);
        
        AbstractTransactionResponse result = handler.next(mockRequest, mockRpcContext);
        
        assertNotNull(result);
    }
}

