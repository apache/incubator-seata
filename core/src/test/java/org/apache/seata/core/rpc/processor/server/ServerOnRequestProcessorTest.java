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
package org.apache.seata.core.rpc.processor.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.BatchResultMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.ResultCode;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.transaction.BranchRegisterRequest;
import org.apache.seata.core.protocol.transaction.BranchRegisterResponse;
import org.apache.seata.core.protocol.transaction.BranchReportRequest;
import org.apache.seata.core.protocol.transaction.BranchReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalBeginRequest;
import org.apache.seata.core.protocol.transaction.GlobalBeginResponse;
import org.apache.seata.core.protocol.transaction.GlobalCommitRequest;
import org.apache.seata.core.protocol.transaction.GlobalCommitResponse;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryRequest;
import org.apache.seata.core.protocol.transaction.GlobalLockQueryResponse;
import org.apache.seata.core.protocol.transaction.GlobalReportRequest;
import org.apache.seata.core.protocol.transaction.GlobalReportResponse;
import org.apache.seata.core.protocol.transaction.GlobalRollbackRequest;
import org.apache.seata.core.protocol.transaction.GlobalRollbackResponse;
import org.apache.seata.core.protocol.transaction.GlobalStatusRequest;
import org.apache.seata.core.protocol.transaction.GlobalStatusResponse;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.core.rpc.TransactionMessageHandler;
import org.apache.seata.core.rpc.netty.ChannelManager;
import org.apache.seata.core.rpc.netty.NettyPoolKey;
import org.apache.seata.core.rpc.netty.NettyServerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for ServerOnRequestProcessor
 */
public class ServerOnRequestProcessorTest {

    private ServerOnRequestProcessor processor;
    private RemotingServer remotingServer;
    private TransactionMessageHandler transactionMessageHandler;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private RpcContext rpcContext;

    @BeforeEach
    public void setUp() throws Exception {
        remotingServer = mock(RemotingServer.class);
        transactionMessageHandler = mock(TransactionMessageHandler.class);
        processor = new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));

        // Register channel
        rpcContext = new RpcContext();
        rpcContext.setChannel(channel);
        rpcContext.setClientRole(NettyPoolKey.TransactionRole.TMROLE);
        rpcContext.setApplicationId("test-app");
        rpcContext.setTransactionServiceGroup("test-group");
        rpcContext.setVersion("1.5.0");

        java.lang.reflect.Field identifiedChannelsField = ChannelManager.class.getDeclaredField("IDENTIFIED_CHANNELS");
        identifiedChannelsField.setAccessible(true);
        @SuppressWarnings("unchecked")
        ConcurrentHashMap<Channel, RpcContext> identifiedChannels =
                (ConcurrentHashMap<Channel, RpcContext>) identifiedChannelsField.get(null);
        identifiedChannels.put(channel, rpcContext);
    }

    @AfterEach
    public void tearDown() {
        try {
            ChannelManager.releaseRpcContext(channel);
            processor.destroy();
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testProcessSingleMessage() throws Exception {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        GlobalBeginResponse response = new GlobalBeginResponse();
        response.setResultCode(ResultCode.Success);
        response.setXid("127.0.0.1:8091:12345");

        when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testProcessMergedWarpMessage() throws Exception {
        // Create merged message
        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        List<AbstractMessage> msgs = new ArrayList<>();
        List<Integer> msgIds = new ArrayList<>();

        GlobalBeginRequest req1 = new GlobalBeginRequest();
        req1.setTransactionName("tx1");
        msgs.add(req1);
        msgIds.add(1);

        BranchRegisterRequest req2 = new BranchRegisterRequest();
        req2.setXid("127.0.0.1:8091:12345");
        req2.setResourceId("jdbc:mysql://localhost:3306/seata");
        msgs.add(req2);
        msgIds.add(2);

        mergedMessage.msgs = msgs;
        mergedMessage.msgIds = msgIds;

        // Setup responses
        GlobalBeginResponse resp1 = new GlobalBeginResponse();
        resp1.setResultCode(ResultCode.Success);

        BranchRegisterResponse resp2 = new BranchRegisterResponse();
        resp2.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(eq(req1), any(RpcContext.class)))
                .thenReturn(resp1);
        when(transactionMessageHandler.onRequest(eq(req2), any(RpcContext.class)))
                .thenReturn(resp2);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setBody(mergedMessage);

        processor.process(ctx, rpcMessage);

        // Verify transaction handler was called for each message
        verify(transactionMessageHandler, atLeastOnce()).onRequest(any(AbstractMessage.class), any(RpcContext.class));

        // Verify response was sent
        verify(remotingServer, atLeastOnce()).sendAsyncResponse(any(RpcMessage.class), eq(channel), any());
    }

    @Test
    public void testProcessUnregisteredChannel() throws Exception {
        Channel unregisteredChannel = mock(Channel.class);
        ChannelHandlerContext unregisteredCtx = mock(ChannelHandlerContext.class);

        when(unregisteredCtx.channel()).thenReturn(unregisteredChannel);
        when(unregisteredChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.2", 8081));

        GlobalBeginRequest request = new GlobalBeginRequest();
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(unregisteredCtx, rpcMessage);

        // Verify channel is closed
        verify(unregisteredCtx).disconnect();
        verify(unregisteredCtx).close();
    }

    @Test
    public void testProcessNonAbstractMessage() throws Exception {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody("Not an AbstractMessage");

        // Should handle gracefully without processing
        processor.process(ctx, rpcMessage);

        // Transaction handler should not be called
        verify(transactionMessageHandler, org.mockito.Mockito.never()).onRequest(any(), any());
    }

    @Test
    public void testProcessWithException() throws Exception {
        GlobalBeginRequest request = new GlobalBeginRequest();
        request.setTransactionName("test-tx");

        when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                .thenThrow(new RuntimeException("Processing error"));

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        // Should handle exception gracefully
        try {
            processor.process(ctx, rpcMessage);
        } catch (Exception e) {
            // Exception may be thrown but should be handled
        }

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
    }

    @Test
    public void testProcessEmptyMergedMessage() throws Exception {
        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        mergedMessage.msgs = new ArrayList<>();
        mergedMessage.msgIds = new ArrayList<>();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setBody(mergedMessage);

        processor.process(ctx, rpcMessage);

        // Should handle empty merged message
        verify(remotingServer, atLeastOnce()).sendAsyncResponse(any(), eq(channel), any());
    }

    @Test
    public void testProcessMergedMessageWithMultipleTypes() throws Exception {
        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        List<AbstractMessage> msgs = new ArrayList<>();
        List<Integer> msgIds = new ArrayList<>();

        // Add various message types
        for (int i = 0; i < 5; i++) {
            GlobalBeginRequest req = new GlobalBeginRequest();
            req.setTransactionName("tx" + i);
            msgs.add(req);
            msgIds.add(i);

            GlobalBeginResponse resp = new GlobalBeginResponse();
            resp.setResultCode(ResultCode.Success);
            when(transactionMessageHandler.onRequest(eq(req), any(RpcContext.class)))
                    .thenReturn(resp);
        }

        mergedMessage.msgs = msgs;
        mergedMessage.msgIds = msgIds;

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setBody(mergedMessage);

        processor.process(ctx, rpcMessage);

        // Verify all messages were processed
        verify(transactionMessageHandler, org.mockito.Mockito.atLeast(5))
                .onRequest(any(AbstractMessage.class), any(RpcContext.class));
    }

    @Test
    public void testDestroyProcessor() {
        // Test destroy doesn't throw exception
        processor.destroy();

        // Should be able to call destroy multiple times
        processor.destroy();
    }

    @Test
    public void testProcessWithDifferentResponseTypes() throws Exception {
        BranchRegisterRequest request = new BranchRegisterRequest();
        request.setXid("127.0.0.1:8091:12345");
        request.setResourceId("jdbc:mysql://localhost:3306/seata");

        BranchRegisterResponse response = new BranchRegisterResponse();
        response.setResultCode(ResultCode.Success);
        response.setBranchId(1L);

        when(transactionMessageHandler.onRequest(any(BranchRegisterRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        ArgumentCaptor<Object> responseCaptor = ArgumentCaptor.forClass(Object.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        assertTrue(responseCaptor.getValue() instanceof BranchRegisterResponse);
        BranchRegisterResponse capturedResponse = (BranchRegisterResponse) responseCaptor.getValue();
        assertEquals(1L, capturedResponse.getBranchId());
    }

    // ==================== Phase 1: Batch Response Core Functionality Tests ====================

    @Test
    public void testBatchResponseFeatureEnabled() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            Field executorField = ServerOnRequestProcessor.class.getDeclaredField("batchResponseExecutorService");
            executorField.setAccessible(true);
            Object executorService = executorField.get(batchProcessor);

            assertTrue(executorService != null);

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }

    @Test
    public void testHandleRequestsByMergedWarpMessageBy150() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            rpcContext.setVersion("1.5.0");

            MergedWarpMessage mergedMessage = new MergedWarpMessage();
            List<AbstractMessage> msgs = new ArrayList<>();
            List<Integer> msgIds = new ArrayList<>();

            GlobalBeginRequest req = new GlobalBeginRequest();
            req.setTransactionName("tx-150");
            msgs.add(req);
            msgIds.add(1);

            mergedMessage.msgs = msgs;
            mergedMessage.msgIds = msgIds;

            GlobalBeginResponse resp = new GlobalBeginResponse();
            resp.setResultCode(ResultCode.Success);

            when(transactionMessageHandler.onRequest(eq(req), any(RpcContext.class)))
                    .thenReturn(resp);

            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setId(100);
            rpcMessage.setCodec((byte) 1);
            rpcMessage.setCompressor((byte) 1);
            rpcMessage.setHeadMap(new HashMap<>());
            rpcMessage.setBody(mergedMessage);

            batchProcessor.process(ctx, rpcMessage);

            Thread.sleep(100);

            verify(transactionMessageHandler, atLeastOnce())
                    .onRequest(any(GlobalBeginRequest.class), any(RpcContext.class));

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }

    @Test
    public void testComputeIfAbsentMsgQueue() throws Exception {
        Method computeMethod =
                ServerOnRequestProcessor.class.getDeclaredMethod("computeIfAbsentMsgQueue", Channel.class);
        computeMethod.setAccessible(true);

        BlockingQueue<?> queue1 = (BlockingQueue<?>) computeMethod.invoke(processor, channel);
        BlockingQueue<?> queue2 = (BlockingQueue<?>) computeMethod.invoke(processor, channel);

        assertTrue(queue1 == queue2);
    }

    @Test
    public void testOfferMsgSuccessfully() throws Exception {
        Method computeMethod =
                ServerOnRequestProcessor.class.getDeclaredMethod("computeIfAbsentMsgQueue", Channel.class);
        computeMethod.setAccessible(true);
        BlockingQueue<?> msgQueue = (BlockingQueue<?>) computeMethod.invoke(processor, channel);

        Method offerMethod = ServerOnRequestProcessor.class.getDeclaredMethod(
                "offerMsg",
                BlockingQueue.class,
                RpcMessage.class,
                org.apache.seata.core.protocol.AbstractResultMessage.class,
                int.class,
                Channel.class);
        offerMethod.setAccessible(true);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);

        GlobalBeginResponse response = new GlobalBeginResponse();
        response.setResultCode(ResultCode.Success);

        offerMethod.invoke(processor, msgQueue, rpcMessage, response, 1, channel);

        assertTrue(msgQueue.size() > 0);
    }

    @Test
    public void testBuildRpcMessage() throws Exception {
        Method buildMethod = ServerOnRequestProcessor.class.getDeclaredMethod(
                "buildRpcMessage",
                Class.forName(
                        "org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$ClientRequestRpcInfo"));
        buildMethod.setAccessible(true);

        RpcMessage originalMessage = new RpcMessage();
        originalMessage.setId(123);
        originalMessage.setCodec((byte) 7);
        originalMessage.setCompressor((byte) 1);
        Map<String, String> headMap = new HashMap<>();
        headMap.put("test-key", "test-value");
        originalMessage.setHeadMap(headMap);

        Class<?> clientRequestRpcInfoClass = Class.forName(
                "org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$ClientRequestRpcInfo");
        Object clientRequestRpcInfo =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(originalMessage);

        RpcMessage builtMessage = (RpcMessage) buildMethod.invoke(processor, clientRequestRpcInfo);

        assertEquals(123, builtMessage.getId());
        assertEquals((byte) 7, builtMessage.getCodec());
        assertEquals((byte) 1, builtMessage.getCompressor());
        assertEquals("test-value", builtMessage.getHeadMap().get("test-key"));
    }

    @Test
    public void testBatchResponseRunnableGroupingMessages() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            rpcContext.setVersion("1.6.0");

            MergedWarpMessage mergedMessage = new MergedWarpMessage();
            List<AbstractMessage> msgs = new ArrayList<>();
            List<Integer> msgIds = new ArrayList<>();

            for (int i = 0; i < 3; i++) {
                GlobalBeginRequest req = new GlobalBeginRequest();
                req.setTransactionName("tx-batch-" + i);
                msgs.add(req);
                msgIds.add(i);

                GlobalBeginResponse resp = new GlobalBeginResponse();
                resp.setResultCode(ResultCode.Success);
                resp.setXid("xid-" + i);

                when(transactionMessageHandler.onRequest(eq(req), any(RpcContext.class)))
                        .thenReturn(resp);
            }

            mergedMessage.msgs = msgs;
            mergedMessage.msgIds = msgIds;

            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setId(100);
            rpcMessage.setCodec((byte) 1);
            rpcMessage.setCompressor((byte) 1);
            rpcMessage.setHeadMap(new HashMap<>());
            rpcMessage.setBody(mergedMessage);

            batchProcessor.process(ctx, rpcMessage);

            Thread.sleep(200);

            ArgumentCaptor<Object> responseCaptor = ArgumentCaptor.forClass(Object.class);
            verify(remotingServer, atLeastOnce())
                    .sendAsyncResponse(any(RpcMessage.class), eq(channel), responseCaptor.capture());

            boolean foundBatchResult = false;
            for (Object captured : responseCaptor.getAllValues()) {
                if (captured instanceof BatchResultMessage) {
                    foundBatchResult = true;
                    BatchResultMessage batchResult = (BatchResultMessage) captured;
                    assertTrue(batchResult.getResultMessages().size() > 0);
                }
            }
            assertTrue(foundBatchResult);

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }

    @Test
    public void testBatchResponseRunnableDifferentCodec() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            rpcContext.setVersion("1.6.0");

            GlobalBeginRequest req1 = new GlobalBeginRequest();
            req1.setTransactionName("tx-codec-1");
            GlobalBeginResponse resp1 = new GlobalBeginResponse();
            resp1.setResultCode(ResultCode.Success);
            when(transactionMessageHandler.onRequest(eq(req1), any(RpcContext.class)))
                    .thenReturn(resp1);

            RpcMessage rpcMessage1 = new RpcMessage();
            rpcMessage1.setId(101);
            rpcMessage1.setCodec((byte) 7);
            rpcMessage1.setCompressor((byte) 1);
            rpcMessage1.setHeadMap(new HashMap<>());

            MergedWarpMessage mergedMessage1 = new MergedWarpMessage();
            List<AbstractMessage> msgs1 = new ArrayList<>();
            msgs1.add(req1);
            List<Integer> msgIds1 = new ArrayList<>();
            msgIds1.add(1);
            mergedMessage1.msgs = msgs1;
            mergedMessage1.msgIds = msgIds1;
            rpcMessage1.setBody(mergedMessage1);

            batchProcessor.process(ctx, rpcMessage1);

            GlobalBeginRequest req2 = new GlobalBeginRequest();
            req2.setTransactionName("tx-codec-2");
            GlobalBeginResponse resp2 = new GlobalBeginResponse();
            resp2.setResultCode(ResultCode.Success);
            when(transactionMessageHandler.onRequest(eq(req2), any(RpcContext.class)))
                    .thenReturn(resp2);

            RpcMessage rpcMessage2 = new RpcMessage();
            rpcMessage2.setId(102);
            rpcMessage2.setCodec((byte) 8);
            rpcMessage2.setCompressor((byte) 1);
            rpcMessage2.setHeadMap(new HashMap<>());

            MergedWarpMessage mergedMessage2 = new MergedWarpMessage();
            List<AbstractMessage> msgs2 = new ArrayList<>();
            msgs2.add(req2);
            List<Integer> msgIds2 = new ArrayList<>();
            msgIds2.add(2);
            mergedMessage2.msgs = msgs2;
            mergedMessage2.msgIds = msgIds2;
            rpcMessage2.setBody(mergedMessage2);

            batchProcessor.process(ctx, rpcMessage2);

            Thread.sleep(200);

            ArgumentCaptor<RpcMessage> rpcMessageCaptor = ArgumentCaptor.forClass(RpcMessage.class);
            verify(remotingServer, atLeastOnce())
                    .sendAsyncResponse(rpcMessageCaptor.capture(), eq(channel), any(BatchResultMessage.class));

            List<RpcMessage> capturedRpcMessages = rpcMessageCaptor.getAllValues();
            boolean hasCodec7 = false;
            boolean hasCodec8 = false;
            for (RpcMessage msg : capturedRpcMessages) {
                if (msg.getCodec() == 7) hasCodec7 = true;
                if (msg.getCodec() == 8) hasCodec8 = true;
            }

            assertTrue(hasCodec7 || hasCodec8);

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }

    // ==================== Phase 2: Inner Classes Tests ====================

    @Test
    public void testClientRequestRpcInfoConstructor() throws Exception {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(999);
        rpcMessage.setCodec((byte) 5);
        rpcMessage.setCompressor((byte) 2);
        Map<String, String> headMap = new HashMap<>();
        headMap.put("key1", "value1");
        rpcMessage.setHeadMap(headMap);

        Class<?> clientRequestRpcInfoClass = Class.forName(
                "org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$ClientRequestRpcInfo");
        Object clientRequestRpcInfo =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage);

        Method getRpcMessageIdMethod = clientRequestRpcInfoClass.getMethod("getRpcMessageId");
        Method getCodecMethod = clientRequestRpcInfoClass.getMethod("getCodec");
        Method getCompressorMethod = clientRequestRpcInfoClass.getMethod("getCompressor");
        Method getHeadMapMethod = clientRequestRpcInfoClass.getMethod("getHeadMap");

        assertEquals(999, getRpcMessageIdMethod.invoke(clientRequestRpcInfo));
        assertEquals((byte) 5, getCodecMethod.invoke(clientRequestRpcInfo));
        assertEquals((byte) 2, getCompressorMethod.invoke(clientRequestRpcInfo));
        assertEquals("value1", ((Map<?, ?>) getHeadMapMethod.invoke(clientRequestRpcInfo)).get("key1"));
    }

    @Test
    public void testClientRequestRpcInfoGettersAndSetters() throws Exception {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setCodec((byte) 1);
        rpcMessage.setCompressor((byte) 1);
        rpcMessage.setHeadMap(new HashMap<>());

        Class<?> clientRequestRpcInfoClass = Class.forName(
                "org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$ClientRequestRpcInfo");
        Object clientRequestRpcInfo =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage);

        Method setRpcMessageIdMethod = clientRequestRpcInfoClass.getMethod("setRpcMessageId", int.class);
        Method setCodecMethod = clientRequestRpcInfoClass.getMethod("setCodec", byte.class);
        Method setCompressorMethod = clientRequestRpcInfoClass.getMethod("setCompressor", byte.class);
        Method setHeadMapMethod = clientRequestRpcInfoClass.getMethod("setHeadMap", Map.class);

        Map<String, String> newHeadMap = new HashMap<>();
        newHeadMap.put("new-key", "new-value");

        setRpcMessageIdMethod.invoke(clientRequestRpcInfo, 888);
        setCodecMethod.invoke(clientRequestRpcInfo, (byte) 9);
        setCompressorMethod.invoke(clientRequestRpcInfo, (byte) 3);
        setHeadMapMethod.invoke(clientRequestRpcInfo, newHeadMap);

        Method getRpcMessageIdMethod = clientRequestRpcInfoClass.getMethod("getRpcMessageId");
        Method getCodecMethod = clientRequestRpcInfoClass.getMethod("getCodec");
        Method getCompressorMethod = clientRequestRpcInfoClass.getMethod("getCompressor");
        Method getHeadMapMethod = clientRequestRpcInfoClass.getMethod("getHeadMap");

        assertEquals(888, getRpcMessageIdMethod.invoke(clientRequestRpcInfo));
        assertEquals((byte) 9, getCodecMethod.invoke(clientRequestRpcInfo));
        assertEquals((byte) 3, getCompressorMethod.invoke(clientRequestRpcInfo));
        assertEquals("new-value", ((Map<?, ?>) getHeadMapMethod.invoke(clientRequestRpcInfo)).get("new-key"));
    }

    @Test
    public void testClientRequestRpcInfoEquals() throws Exception {
        RpcMessage rpcMessage1 = new RpcMessage();
        rpcMessage1.setId(100);
        rpcMessage1.setCodec((byte) 1);
        rpcMessage1.setCompressor((byte) 1);
        Map<String, String> headMap1 = new HashMap<>();
        headMap1.put("key", "value");
        rpcMessage1.setHeadMap(headMap1);

        RpcMessage rpcMessage2 = new RpcMessage();
        rpcMessage2.setId(100);
        rpcMessage2.setCodec((byte) 1);
        rpcMessage2.setCompressor((byte) 1);
        Map<String, String> headMap2 = new HashMap<>();
        headMap2.put("key", "value");
        rpcMessage2.setHeadMap(headMap2);

        RpcMessage rpcMessage3 = new RpcMessage();
        rpcMessage3.setId(200);
        rpcMessage3.setCodec((byte) 1);
        rpcMessage3.setCompressor((byte) 1);
        rpcMessage3.setHeadMap(headMap1);

        Class<?> clientRequestRpcInfoClass = Class.forName(
                "org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$ClientRequestRpcInfo");
        Object info1 =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage1);
        Object info2 =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage2);
        Object info3 =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage3);

        Method equalsMethod = clientRequestRpcInfoClass.getMethod("equals", Object.class);

        assertTrue((Boolean) equalsMethod.invoke(info1, info1));
        assertTrue((Boolean) equalsMethod.invoke(info1, info2));
        assertTrue(!(Boolean) equalsMethod.invoke(info1, info3));
        assertTrue(!(Boolean) equalsMethod.invoke(info1, new Object[] {null}));
        assertTrue(!(Boolean) equalsMethod.invoke(info1, "string"));
    }

    @Test
    public void testClientRequestRpcInfoHashCode() throws Exception {
        RpcMessage rpcMessage1 = new RpcMessage();
        rpcMessage1.setId(100);
        rpcMessage1.setCodec((byte) 1);
        rpcMessage1.setCompressor((byte) 1);
        Map<String, String> headMap = new HashMap<>();
        headMap.put("key", "value");
        rpcMessage1.setHeadMap(headMap);

        RpcMessage rpcMessage2 = new RpcMessage();
        rpcMessage2.setId(100);
        rpcMessage2.setCodec((byte) 1);
        rpcMessage2.setCompressor((byte) 1);
        rpcMessage2.setHeadMap(headMap);

        Class<?> clientRequestRpcInfoClass = Class.forName(
                "org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$ClientRequestRpcInfo");
        Object info1 =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage1);
        Object info2 =
                clientRequestRpcInfoClass.getConstructor(RpcMessage.class).newInstance(rpcMessage2);

        Method hashCodeMethod = clientRequestRpcInfoClass.getMethod("hashCode");

        assertEquals(hashCodeMethod.invoke(info1), hashCodeMethod.invoke(info2));
    }

    @Test
    public void testQueueItemConstructorAndGetters() throws Exception {
        GlobalBeginResponse resultMessage = new GlobalBeginResponse();
        resultMessage.setResultCode(ResultCode.Success);
        resultMessage.setXid("test-xid");

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(123);

        Class<?> queueItemClass =
                Class.forName("org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$QueueItem");
        Object queueItem = queueItemClass
                .getConstructor(org.apache.seata.core.protocol.AbstractResultMessage.class, int.class, RpcMessage.class)
                .newInstance(resultMessage, 456, rpcMessage);

        Method getResultMessageMethod = queueItemClass.getMethod("getResultMessage");
        Method getMsgIdMethod = queueItemClass.getMethod("getMsgId");
        Method getRpcMessageMethod = queueItemClass.getMethod("getRpcMessage");

        assertEquals(resultMessage, getResultMessageMethod.invoke(queueItem));
        assertEquals(456, getMsgIdMethod.invoke(queueItem));
        assertEquals(rpcMessage, getRpcMessageMethod.invoke(queueItem));
    }

    @Test
    public void testQueueItemSetters() throws Exception {
        GlobalBeginResponse resultMessage1 = new GlobalBeginResponse();
        resultMessage1.setResultCode(ResultCode.Success);

        RpcMessage rpcMessage1 = new RpcMessage();
        rpcMessage1.setId(111);

        Class<?> queueItemClass =
                Class.forName("org.apache.seata.core.rpc.processor.server.ServerOnRequestProcessor$QueueItem");
        Object queueItem = queueItemClass
                .getConstructor(org.apache.seata.core.protocol.AbstractResultMessage.class, int.class, RpcMessage.class)
                .newInstance(resultMessage1, 111, rpcMessage1);

        GlobalBeginResponse resultMessage2 = new GlobalBeginResponse();
        resultMessage2.setXid("new-xid");
        RpcMessage rpcMessage2 = new RpcMessage();
        rpcMessage2.setId(222);

        Method setResultMessageMethod = queueItemClass.getMethod(
                "setResultMessage", org.apache.seata.core.protocol.AbstractResultMessage.class);
        Method setMsgIdMethod = queueItemClass.getMethod("setMsgId", Integer.class);
        Method setRpcMessageMethod = queueItemClass.getMethod("setRpcMessage", RpcMessage.class);

        setResultMessageMethod.invoke(queueItem, resultMessage2);
        setMsgIdMethod.invoke(queueItem, 999);
        setRpcMessageMethod.invoke(queueItem, rpcMessage2);

        Method getResultMessageMethod = queueItemClass.getMethod("getResultMessage");
        Method getMsgIdMethod = queueItemClass.getMethod("getMsgId");
        Method getRpcMessageMethod = queueItemClass.getMethod("getRpcMessage");

        assertEquals(resultMessage2, getResultMessageMethod.invoke(queueItem));
        assertEquals(999, getMsgIdMethod.invoke(queueItem));
        assertEquals(rpcMessage2, getRpcMessageMethod.invoke(queueItem));
    }

    // ==================== Phase 3: Message Types Coverage Tests ====================

    @Test
    public void testProcessGlobalCommitRequest() throws Exception {
        GlobalCommitRequest request = new GlobalCommitRequest();
        request.setXid("127.0.0.1:8091:12345");

        GlobalCommitResponse response = new GlobalCommitResponse();
        response.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalCommitRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testProcessGlobalRollbackRequest() throws Exception {
        GlobalRollbackRequest request = new GlobalRollbackRequest();
        request.setXid("127.0.0.1:8091:12345");

        GlobalRollbackResponse response = new GlobalRollbackResponse();
        response.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalRollbackRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testProcessGlobalStatusRequest() throws Exception {
        GlobalStatusRequest request = new GlobalStatusRequest();
        request.setXid("127.0.0.1:8091:12345");

        GlobalStatusResponse response = new GlobalStatusResponse();
        response.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalStatusRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testProcessGlobalReportRequest() throws Exception {
        GlobalReportRequest request = new GlobalReportRequest();
        request.setXid("127.0.0.1:8091:12345");

        GlobalReportResponse response = new GlobalReportResponse();
        response.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalReportRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testProcessBranchReportRequest() throws Exception {
        BranchReportRequest request = new BranchReportRequest();
        request.setXid("127.0.0.1:8091:12345");
        request.setBranchId(1L);

        BranchReportResponse response = new BranchReportResponse();
        response.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(BranchReportRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    @Test
    public void testProcessGlobalLockQueryRequest() throws Exception {
        GlobalLockQueryRequest request = new GlobalLockQueryRequest();
        request.setXid("127.0.0.1:8091:12345");

        GlobalLockQueryResponse response = new GlobalLockQueryResponse();
        response.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalLockQueryRequest.class), any(RpcContext.class)))
                .thenReturn(response);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(request);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler).onRequest(eq(request), eq(rpcContext));
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(response));
    }

    // ==================== Phase 4: Version Compatibility Tests ====================

    @Test
    public void testMergedMessageWithVersion140() throws Exception {
        rpcContext.setVersion("1.4.0");

        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        List<AbstractMessage> msgs = new ArrayList<>();
        List<Integer> msgIds = new ArrayList<>();

        GlobalBeginRequest req = new GlobalBeginRequest();
        req.setTransactionName("tx-140");
        msgs.add(req);
        msgIds.add(1);

        mergedMessage.msgs = msgs;
        mergedMessage.msgIds = msgIds;

        GlobalBeginResponse resp = new GlobalBeginResponse();
        resp.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                .thenReturn(resp);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setBody(mergedMessage);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler, atLeastOnce()).onRequest(any(AbstractMessage.class), any(RpcContext.class));
        verify(remotingServer, atLeastOnce()).sendAsyncResponse(any(RpcMessage.class), eq(channel), any());
    }

    @Test
    public void testMergedMessageWithVersion150() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            rpcContext.setVersion("1.5.0");

            MergedWarpMessage mergedMessage = new MergedWarpMessage();
            List<AbstractMessage> msgs = new ArrayList<>();
            List<Integer> msgIds = new ArrayList<>();

            GlobalBeginRequest req = new GlobalBeginRequest();
            req.setTransactionName("tx-150-exact");
            msgs.add(req);
            msgIds.add(1);

            mergedMessage.msgs = msgs;
            mergedMessage.msgIds = msgIds;

            GlobalBeginResponse resp = new GlobalBeginResponse();
            resp.setResultCode(ResultCode.Success);

            when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                    .thenReturn(resp);

            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setId(100);
            rpcMessage.setCodec((byte) 1);
            rpcMessage.setCompressor((byte) 1);
            rpcMessage.setHeadMap(new HashMap<>());
            rpcMessage.setBody(mergedMessage);

            batchProcessor.process(ctx, rpcMessage);

            Thread.sleep(100);

            verify(transactionMessageHandler, atLeastOnce())
                    .onRequest(any(GlobalBeginRequest.class), any(RpcContext.class));

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }

    @Test
    public void testMergedMessageWithVersion160() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            rpcContext.setVersion("1.6.0");

            MergedWarpMessage mergedMessage = new MergedWarpMessage();
            List<AbstractMessage> msgs = new ArrayList<>();
            List<Integer> msgIds = new ArrayList<>();

            GlobalBeginRequest req = new GlobalBeginRequest();
            req.setTransactionName("tx-160");
            msgs.add(req);
            msgIds.add(1);

            mergedMessage.msgs = msgs;
            mergedMessage.msgIds = msgIds;

            GlobalBeginResponse resp = new GlobalBeginResponse();
            resp.setResultCode(ResultCode.Success);

            when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                    .thenReturn(resp);

            RpcMessage rpcMessage = new RpcMessage();
            rpcMessage.setId(100);
            rpcMessage.setCodec((byte) 1);
            rpcMessage.setCompressor((byte) 1);
            rpcMessage.setHeadMap(new HashMap<>());
            rpcMessage.setBody(mergedMessage);

            batchProcessor.process(ctx, rpcMessage);

            Thread.sleep(100);

            verify(transactionMessageHandler, atLeastOnce())
                    .onRequest(any(GlobalBeginRequest.class), any(RpcContext.class));

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }

    @Test
    public void testMergedMessageWithNullVersion() throws Exception {
        rpcContext.setVersion(null);

        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        List<AbstractMessage> msgs = new ArrayList<>();
        List<Integer> msgIds = new ArrayList<>();

        GlobalBeginRequest req = new GlobalBeginRequest();
        req.setTransactionName("tx-null-version");
        msgs.add(req);
        msgIds.add(1);

        mergedMessage.msgs = msgs;
        mergedMessage.msgIds = msgIds;

        GlobalBeginResponse resp = new GlobalBeginResponse();
        resp.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                .thenReturn(resp);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setBody(mergedMessage);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler, atLeastOnce()).onRequest(any(AbstractMessage.class), any(RpcContext.class));
    }

    @Test
    public void testMergedMessageWithBlankVersion() throws Exception {
        rpcContext.setVersion("");

        MergedWarpMessage mergedMessage = new MergedWarpMessage();
        List<AbstractMessage> msgs = new ArrayList<>();
        List<Integer> msgIds = new ArrayList<>();

        GlobalBeginRequest req = new GlobalBeginRequest();
        req.setTransactionName("tx-blank-version");
        msgs.add(req);
        msgIds.add(1);

        mergedMessage.msgs = msgs;
        mergedMessage.msgIds = msgIds;

        GlobalBeginResponse resp = new GlobalBeginResponse();
        resp.setResultCode(ResultCode.Success);

        when(transactionMessageHandler.onRequest(any(GlobalBeginRequest.class), any(RpcContext.class)))
                .thenReturn(resp);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setId(100);
        rpcMessage.setBody(mergedMessage);

        processor.process(ctx, rpcMessage);

        verify(transactionMessageHandler, atLeastOnce()).onRequest(any(AbstractMessage.class), any(RpcContext.class));
    }

    // ==================== Phase 5: Parallel Processing Path Tests ====================

    @Test
    public void testDestroyWithBatchResponseEnabled() throws Exception {
        Field enableField = NettyServerConfig.class.getDeclaredField("ENABLE_TC_SERVER_BATCH_SEND_RESPONSE");
        enableField.setAccessible(true);
        boolean originalValue = enableField.getBoolean(null);

        try {
            enableField.setBoolean(null, true);

            ServerOnRequestProcessor batchProcessor =
                    new ServerOnRequestProcessor(remotingServer, transactionMessageHandler);

            Field executorField = ServerOnRequestProcessor.class.getDeclaredField("batchResponseExecutorService");
            executorField.setAccessible(true);
            Object executorService = executorField.get(batchProcessor);

            assertTrue(executorService != null);

            batchProcessor.destroy();

            batchProcessor.destroy();
        } finally {
            enableField.setBoolean(null, originalValue);
        }
    }
}
