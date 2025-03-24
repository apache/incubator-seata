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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Rm branch commit processor test.
 */
public class RmBranchCommitProcessorTest {
    private ChannelHandlerContext mockCtx;
    private RpcMessage mockRpcMessage;
    private TransactionMessageHandler mockHandler;
    private RemotingClient mockRemotingClient;
    private Logger mockLogger;
    private MockedStatic<LoggerFactory> mockedLoggerFactory;
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
        mockLogger = mock(Logger.class);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        mockedLoggerFactory = Mockito.mockStatic(LoggerFactory.class);
        mockedLoggerFactory.when(() -> LoggerFactory.getLogger(RmBranchCommitProcessor.class)).thenReturn(mockLogger);

        mockedNetUtil = Mockito.mockStatic(NetUtil.class);

        processor = new RmBranchCommitProcessor(mockHandler, mockRemotingClient);
        //setField(null, "LOGGER", mockLogger);
    }

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        mockedLoggerFactory.close();
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
        mockedNetUtil.when(() -> NetUtil.toStringAddress(any(SocketAddress.class))).thenReturn("127.0.0.1:8091");

        BranchCommitRequest mockRequest = mock(BranchCommitRequest.class);
        BranchCommitResponse mockResponse = mock(BranchCommitResponse.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);
        when(mockHandler.onRequest(mockRequest, null)).thenReturn(mockResponse);
        when(mockLogger.isInfoEnabled()).thenReturn(true);

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
        mockedNetUtil.when(() -> NetUtil.toStringAddress(any(SocketAddress.class))).thenReturn("127.0.0.1:8091");

        BranchCommitRequest mockRequest = mock(BranchCommitRequest.class);
        BranchCommitResponse mockResponse = mock(BranchCommitResponse.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);
        when(mockHandler.onRequest(mockRequest, null)).thenReturn(mockResponse);

        Throwable simulatedError = new RuntimeException("Network failure");
        doThrow(simulatedError).when(mockRemotingClient).sendAsyncResponse(anyString(), any(), any());

        processor.process(mockCtx, mockRpcMessage);

        verify(mockLogger).error(eq("branch commit error: {}"), eq("Network failure"), eq(simulatedError));
    }

    /**
     * Process should not log debug when disabled.
     *
     * @throws Exception the exception
     */
    @Test
    void processShouldNotLogDebugWhenDisabled() throws Exception {
        InetSocketAddress mockAddress = new InetSocketAddress("127.0.0.1", 8091);
        Channel mockChannel = mock(Channel.class);
        when(mockCtx.channel()).thenReturn(mockChannel);
        when(mockChannel.remoteAddress()).thenReturn(mockAddress);
        mockedNetUtil.when(() -> NetUtil.toStringAddress(any(SocketAddress.class))).thenReturn("127.0.0.1:8091");

        BranchCommitRequest mockRequest = mock(BranchCommitRequest.class);
        BranchCommitResponse mockResponse = mock(BranchCommitResponse.class);
        when(mockRpcMessage.getBody()).thenReturn(mockRequest);
        when(mockHandler.onRequest(mockRequest, null)).thenReturn(mockResponse);
        when(mockLogger.isInfoEnabled()).thenReturn(true);
        when(mockLogger.isDebugEnabled()).thenReturn(false);

        processor.process(mockCtx, mockRpcMessage);

        verify(mockLogger, never()).debug(anyString(), (Object[])any());
    }
}
