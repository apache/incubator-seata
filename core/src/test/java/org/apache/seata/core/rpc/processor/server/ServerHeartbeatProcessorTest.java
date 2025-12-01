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
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for ServerHeartbeatProcessor
 */
public class ServerHeartbeatProcessorTest {

    private ServerHeartbeatProcessor processor;
    private RemotingServer remotingServer;
    private ChannelHandlerContext ctx;
    private Channel channel;
    private RpcMessage rpcMessage;

    @BeforeEach
    public void setUp() {
        remotingServer = mock(RemotingServer.class);
        processor = new ServerHeartbeatProcessor(remotingServer);

        ctx = mock(ChannelHandlerContext.class);
        channel = mock(Channel.class);
        rpcMessage = new RpcMessage();
        rpcMessage.setId(1);
        rpcMessage.setBody(HeartbeatMessage.PING);

        when(ctx.channel()).thenReturn(channel);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
    }

    @Test
    public void testProcessHeartbeatSuccess() throws Exception {
        // Execute
        processor.process(ctx, rpcMessage);

        // Verify
        ArgumentCaptor<Object> responseCaptor = ArgumentCaptor.forClass(Object.class);
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), responseCaptor.capture());

        assertSame(HeartbeatMessage.PONG, responseCaptor.getValue(), "Should send PONG response");
    }

    @Test
    public void testProcessHeartbeatWithException() throws Exception {
        // Mock exception when sending response
        doThrow(new RuntimeException("Send failed")).when(remotingServer).sendAsyncResponse(any(), any(), any());

        // Should not throw exception, just log error
        processor.process(ctx, rpcMessage);

        // Verify sendAsyncResponse was called despite exception
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(channel), eq(HeartbeatMessage.PONG));
    }

    @Test
    public void testProcessWithDifferentChannel() throws Exception {
        Channel differentChannel = mock(Channel.class);
        when(differentChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.2", 8081));
        when(differentChannel.isActive()).thenReturn(true);

        ChannelHandlerContext differentCtx = mock(ChannelHandlerContext.class);
        when(differentCtx.channel()).thenReturn(differentChannel);

        // Should process heartbeat for different channel
        processor.process(differentCtx, rpcMessage);

        // Verify response sent to correct channel
        verify(remotingServer).sendAsyncResponse(eq(rpcMessage), eq(differentChannel), eq(HeartbeatMessage.PONG));
    }

    @Test
    public void testProcessMultipleHeartbeats() throws Exception {
        // Process multiple heartbeats
        processor.process(ctx, rpcMessage);
        processor.process(ctx, rpcMessage);
        processor.process(ctx, rpcMessage);

        // Verify all were processed
        verify(remotingServer, org.mockito.Mockito.times(3))
                .sendAsyncResponse(eq(rpcMessage), eq(channel), eq(HeartbeatMessage.PONG));
    }
}
