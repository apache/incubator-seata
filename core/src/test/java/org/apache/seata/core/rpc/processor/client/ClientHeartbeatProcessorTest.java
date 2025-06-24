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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The type Client heartbeat processor test.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClientHeartbeatProcessorTest {
    private static final String CLASS_NAME = "org.apache.seata.core.rpc.processor.client.ClientHeartbeatProcessor";

    private final List<Logger> watchedLoggers = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();

    private ClientHeartbeatProcessor processor;
    private ChannelHandlerContext mockCtx;
    private RpcMessage mockRpcMessage;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        mockCtx = mock(ChannelHandlerContext.class);
        mockRpcMessage = mock(RpcMessage.class);
        logWatcher.start();
        setUpLogger();
        processor = new ClientHeartbeatProcessor();
    }

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        watchedLoggers.forEach(Logger::detachAndStopAllAppenders);
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
        assertTrue(LoggerFactory.getLogger(ClientHeartbeatProcessor.class).isDebugEnabled());

        // Act
        processor.process(mockCtx, mockRpcMessage);

        // Assert
        assertTrue(
                getLogs(Level.DEBUG).stream().anyMatch(log -> log.equals("received PONG from " + mockRemoteAddress)));
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

        // Act
        processor.process(mockCtx, mockRpcMessage);

        // Assert
        assertTrue(getLogs(Level.DEBUG).isEmpty());
    }

    private List<String> getLogs(Level level) {
        return logWatcher.list.stream()
                .filter(event -> event.getLoggerName().endsWith(CLASS_NAME)
                        && event.getLevel().equals(level))
                .map(ILoggingEvent::getFormattedMessage)
                .collect(Collectors.toList());
    }

    private void setUpLogger() {
        Logger logger = ((Logger) LoggerFactory.getLogger(CLASS_NAME));
        logger.addAppender(logWatcher);
        watchedLoggers.add(logger);
    }
}
