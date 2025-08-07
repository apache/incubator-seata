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
import org.apache.seata.common.util.NetUtil;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchCommitRequest;
import org.apache.seata.core.protocol.transaction.BranchCommitResponse;
import org.apache.seata.core.rpc.RemotingClient;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Rm branch commit processor test.
 */
public class RmBranchCommitProcessorTest {
    private static final String CLASS_NAME = "org.apache.seata.core.rpc.processor.client.RmBranchCommitProcessor";

    private final List<Logger> watchedLoggers = new ArrayList<>();
    private final ListAppender<ILoggingEvent> logWatcher = new ListAppender<>();

    private ChannelHandlerContext mockCtx;
    private RpcMessage mockRpcMessage;
    private TransactionMessageHandler mockHandler;
    private RemotingClient mockRemotingClient;
    private MockedStatic<NetUtil> mockedNetUtil;
    private RmBranchCommitProcessor processor;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        mockCtx = mock(ChannelHandlerContext.class);
        mockRpcMessage = mock(RpcMessage.class);
        mockHandler = mock(TransactionMessageHandler.class);
        mockRemotingClient = mock(RemotingClient.class);
        logWatcher.start();
        setUpLogger();
        mockedNetUtil = Mockito.mockStatic(NetUtil.class);
        processor = new RmBranchCommitProcessor(mockHandler, mockRemotingClient);
    }

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        watchedLoggers.forEach(Logger::detachAndStopAllAppenders);
        mockedNetUtil.close();
    }

    /**
     * Process should handle branch commit and send response.
     *
     * @throws Exception the exception
     */
    @Test
    void processShouldHandleBranchCommitAndSendResponse() throws Exception {
        InetSocketAddress mockAddress = new InetSocketAddress("127.0.0.1", 8091);
        Channel mockChannel = mock(Channel.class);
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockChannel.remoteAddress()).thenReturn(mockAddress);
        mockedNetUtil
                .when(() -> NetUtil.toStringAddress(any(SocketAddress.class)))
                .thenReturn("127.0.0.1:8091");

        BranchCommitRequest mockRequest = mock(BranchCommitRequest.class);
        BranchCommitResponse mockResponse = mock(BranchCommitResponse.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);
        when(mockHandler.onRequest(mockRequest, null)).thenReturn(mockResponse);

        processor.process(mockCtx, mockRpcMessage);

        verify(mockHandler).onRequest(mockRequest, null);
        verify(mockRemotingClient).sendAsyncResponse("127.0.0.1:8091", mockRpcMessage, mockResponse);
    }

    /**
     * Process should log error when send fails.
     *
     * @throws Exception the exception
     */
    @Test
    void processShouldLogErrorWhenSendFails() throws Exception {
        InetSocketAddress mockAddress = new InetSocketAddress("127.0.0.1", 8091);
        Channel mockChannel = mock(Channel.class);
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockChannel.remoteAddress()).thenReturn(mockAddress);
        mockedNetUtil
                .when(() -> NetUtil.toStringAddress(any(SocketAddress.class)))
                .thenReturn("127.0.0.1:8091");

        BranchCommitRequest mockRequest = mock(BranchCommitRequest.class);
        BranchCommitResponse mockResponse = mock(BranchCommitResponse.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);
        when(mockHandler.onRequest(mockRequest, null)).thenReturn(mockResponse);

        Throwable simulatedError = new RuntimeException("Network failure");
        doThrow(simulatedError).when(mockRemotingClient).sendAsyncResponse(anyString(), any(), any());

        processor.process(mockCtx, mockRpcMessage);

        assertTrue(getLogs(Level.ERROR).stream().anyMatch(log -> log.equals("branch commit error: Network failure")));
    }

    /**
     * Process print log info when level is info.
     *
     * @throws Exception the exception
     */
    @Test
    void processShouldPrintLogInfoWhenLevelIsInfo() throws Exception {
        InetSocketAddress mockAddress = new InetSocketAddress("127.0.0.1", 8091);
        Channel mockChannel = mock(Channel.class);
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockChannel.remoteAddress()).thenReturn(mockAddress);
        mockedNetUtil
                .when(() -> NetUtil.toStringAddress(any(SocketAddress.class)))
                .thenReturn("127.0.0.1:8091");

        BranchCommitRequest mockRequest = mock(BranchCommitRequest.class);
        BranchCommitResponse mockResponse = mock(BranchCommitResponse.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);
        when(mockHandler.onRequest(mockRequest, null)).thenReturn(mockResponse);
        processor.process(mockCtx, mockRpcMessage);

        assertTrue(getLogs(Level.INFO).stream()
                .anyMatch(log -> log.startsWith("rm client handle branch commit process:")));
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
