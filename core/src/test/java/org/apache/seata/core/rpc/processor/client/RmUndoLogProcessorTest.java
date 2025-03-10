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
package org.apache.seata.core.rpc.processor.client;

import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.UndoLogDeleteRequest;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Rm undo log processor test.
 */
public class RmUndoLogProcessorTest {
    private ChannelHandlerContext mockCtx;
    private RpcMessage mockRpcMessage;
    private RmUndoLogProcessor processor;
    private MockedStatic<LoggerFactory> mockedLoggerFactory;
    private TransactionMessageHandler mockHandler;
    private Logger mockLogger;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        mockCtx = mock(ChannelHandlerContext.class);
        mockRpcMessage = mock(RpcMessage.class);
        mockHandler = mock(TransactionMessageHandler.class);
        mockLogger = mock(Logger.class);

        mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
        mockedLoggerFactory.when(() -> LoggerFactory.getLogger(RmUndoLogProcessor.class)).thenReturn(mockLogger);

        processor = new RmUndoLogProcessor(mockHandler);
    }

    /**
     * Process should invoke handler.
     *
     * @throws Exception the exception
     */
    @Test
    void process_ShouldInvokeHandler() throws Exception {
        // Arrange
        UndoLogDeleteRequest mockRequest = mock(UndoLogDeleteRequest.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);

        // Act
        processor.process(mockCtx, mockRpcMessage);

        // Assert
        verify(mockHandler).onRequest(mockRequest, null);
    }

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        if (mockedLoggerFactory != null) {
            mockedLoggerFactory.close();
        }
    }
}
