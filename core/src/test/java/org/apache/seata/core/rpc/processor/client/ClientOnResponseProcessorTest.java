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
import org.apache.seata.core.protocol.AbstractResultMessage;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergeMessage;
import org.apache.seata.core.protocol.MergeResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The type Client on response processor test.
 */
class ClientOnResponseProcessorTest {

    private ChannelHandlerContext mockCtx;
    private RpcMessage mockRpcMessage;
    private Map<Integer, MergeMessage> mergeMsgMap;
    private ConcurrentHashMap<Integer, MessageFuture> futures;
    private Map<Integer, Integer> childToParentMap;
    private TransactionMessageHandler mockTransactionMessageHandler;
    private Logger mockLogger;

    private ClientOnResponseProcessor processor;

    private MockedStatic<LoggerFactory> mockedLogger;

    /**
     * Sets up.
     */
    @BeforeEach
    void setUp() {
        mockCtx = mock(ChannelHandlerContext.class);
        mockRpcMessage = mock(RpcMessage.class);
        mergeMsgMap = new HashMap<>();
        futures = new ConcurrentHashMap<>();
        childToParentMap = new HashMap<>();
        mockTransactionMessageHandler = mock(TransactionMessageHandler.class);
        mockLogger = mock(Logger.class);

        // Mock static logger
        mockedLogger = Mockito.mockStatic(LoggerFactory.class);
        mockedLogger
                .when(() -> LoggerFactory.getLogger(ClientOnResponseProcessor.class))
                .thenReturn(mockLogger);

        processor =
                new ClientOnResponseProcessor(mergeMsgMap, futures, childToParentMap, mockTransactionMessageHandler);
    }

    /**
     * Process merge result message.
     *
     * @throws Exception the exception
     */
    @Test
    @Disabled
    void processMergeResultMessage() throws Exception {
        // Setup merge result message
        MergeResultMessage mockMergeResult = mock(MergeResultMessage.class);
        when(mockRpcMessage.getBody()).thenReturn(mockMergeResult);
        GlobalCommitResponse gitCommitResponse = new GlobalCommitResponse();
        mockMergeResult.setMsgs(new AbstractResultMessage[] {gitCommitResponse});
        int rpcId = 123;
        when(mockRpcMessage.getId()).thenReturn(rpcId);

        MergedWarpMessage mergedWarp = new MergedWarpMessage();
        GlobalBeginRequest mockRpc = mock(GlobalBeginRequest.class);
        mergedWarp.msgs.add(mockRpc);
        mergedWarp.msgIds.add(456);
        mergeMsgMap.put(rpcId, mergedWarp);

        // Configure future
        MessageFuture mockFuture = mock(MessageFuture.class);
        futures.put(456, mockFuture);

        // Execute
        processor.process(mockCtx, mockRpcMessage);

        // Verify
        verify(futures).remove(456);
        verify(childToParentMap).remove(456);
        verify(mockFuture).setResultMessage(any());
        verify(mockLogger, never()).error(anyString(), any(), any());
    }

    /**
     * Process batch result message.
     *
     * @throws Exception the exception
     */
    @Test
    void processBatchResultMessage() throws Exception {
        // Setup batch result message
        BatchResultMessage mockBatchResult = mock(BatchResultMessage.class);
        when(mockRpcMessage.getBody()).thenReturn(mockBatchResult);
        when(mockBatchResult.getMsgIds()).thenReturn(Collections.singletonList(789));
        when(mockBatchResult.getResultMessages())
                .thenReturn(Collections.singletonList(mock(AbstractResultMessage.class)));

        // Configure child-parent mapping
        childToParentMap.put(789, 101112);
        mergeMsgMap.put(101112, mock(MergeMessage.class));

        // Configure future
        MessageFuture mockFuture = mock(MessageFuture.class);
        futures.put(789, mockFuture);

        // Execute
        processor.process(mockCtx, mockRpcMessage);

        // Verify
        assertFalse(futures.containsKey(789), "Future should be removed from the map");
        assertFalse(childToParentMap.containsKey(789), "Child-parent mapping should be removed");
        assertFalse(mergeMsgMap.containsKey(101112), "Parent message should be removed");
        verify(mockFuture).setResultMessage(any());
    }

    /**
     * Process generic result message with future.
     *
     * @throws Exception the exception
     */
    @Test
    void processGenericResultMessageWithFuture() throws Exception {
        // Setup generic message
        AbstractResultMessage mockResult = mock(AbstractResultMessage.class);
        when(mockRpcMessage.getBody()).thenReturn(mockResult);
        int msgId = 131415;
        when(mockRpcMessage.getId()).thenReturn(msgId);

        // Configure future
        MessageFuture mockFuture = mock(MessageFuture.class);
        futures.put(msgId, mockFuture);

        // Execute
        processor.process(mockCtx, mockRpcMessage);

        // Verify
        assertFalse(futures.containsKey(msgId), "Future should be removed from the map");
        verify(mockFuture).setResultMessage(mockResult);
        verify(mockTransactionMessageHandler, never()).onResponse(any(), any());
    }

    /**
     * Process generic result message without future.
     *
     * @throws Exception the exception
     */
    @Test
    void processGenericResultMessageWithoutFuture() throws Exception {
        // Setup generic message
        AbstractResultMessage mockResult = mock(AbstractResultMessage.class);
        when(mockRpcMessage.getBody()).thenReturn(mockResult);
        int msgId = 161718;
        when(mockRpcMessage.getId()).thenReturn(msgId);

        // No future exists

        // Execute
        processor.process(mockCtx, mockRpcMessage);

        // Verify
        verify(mockTransactionMessageHandler).onResponse(mockResult, null);
        verify(mockLogger, never()).error(anyString(), any(Object.class), any(Object.class));
    }

    /**
     * Tear down.
     */
    @AfterEach
    void tearDown() {
        mockCtx = null;
        mockRpcMessage = null;
        mergeMsgMap = null;
        futures = null;
        childToParentMap = null;
        mockTransactionMessageHandler = null;
        if (mockedLogger != null) {
            mockedLogger.close();
        }
    }
}
