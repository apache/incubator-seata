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

import java.net.InetSocketAddress;
import java.net.SocketAddress;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.ArgumentMatchers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Client heartbeat processor test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientHeartbeatProcessorTest {
    private ClientHeartbeatProcessor processor;
    private ChannelHandlerContext mockCtx;
    private RpcMessage mockRpcMessage;
    private Logger mockLogger;
    private MockedStatic<LoggerFactory> mockedLoggerFactory;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        mockCtx = mock(ChannelHandlerContext.class);
        mockRpcMessage = mock(RpcMessage.class);
        mockLogger = mock(Logger.class);

        // Mock static LoggerFactory to control LOGGER behavior
        mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
        mockedLoggerFactory.when(() -> LoggerFactory.getLogger(ClientHeartbeatProcessor.class)).thenReturn(mockLogger);
        processor = new ClientHeartbeatProcessor();
    }

    /**
     * Process should log debug when receive pong message and debug enabled.
     *
     * @throws Exception the exception
     */
    @Test
    @Order(1)
    void process_ShouldLogDebug_WhenReceivePongMessageAndDebugEnabled() throws Exception {
        // Arrange
        Channel mockChannel = mock(Channel.class);
        when(mockCtx.channel()).thenReturn(mockChannel);

        SocketAddress mockRemoteAddress = new InetSocketAddress("127.0.0.1", 8080);
        when(mockChannel.remoteAddress()).thenReturn(mockRemoteAddress);

        when(mockRpcMessage.getBody()).thenReturn(HeartbeatMessage.PONG);
        when(mockLogger.isDebugEnabled()).thenReturn(true);
        assertTrue(LoggerFactory.getLogger(ClientHeartbeatProcessor.class).isDebugEnabled());

        // Act
        processor.process(mockCtx, mockRpcMessage);

        // Assert
        verify(mockLogger).debug("received PONG from {}", mockRemoteAddress);
    }

    /**
     * Process should not log when receive non pong message.
     *
     * @throws Exception the exception
     */
    @Test
    @Order(2)
    void process_ShouldNotLog_WhenReceiveNonPongMessage() throws Exception {
        // Arrange
        when(mockRpcMessage.getBody()).thenReturn("OTHER_MESSAGE");
        when(mockLogger.isDebugEnabled()).thenReturn(true);

        // Act
        processor.process(mockCtx, mockRpcMessage);

        // Assert
        verify(mockLogger, never()).debug(anyString(), ArgumentMatchers.<Object[]>any());
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
