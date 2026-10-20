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
package org.apache.seata.core.rpc.netty;

import io.netty.channel.Channel;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.RemotingServer;
import org.apache.seata.core.rpc.RpcContext;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyByte;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ServerSyncRequestTest {
    @Test
    void allSyncOverloadsCountAndReleaseOnSuccessTimeoutAndFailure() throws Exception {
        for (int overload = 0; overload < 3; overload++) {
            for (int outcome = 0; outcome < 3; outcome++) {
                verifyLifecycle(overload, outcome);
            }
        }
    }

    private void verifyLifecycle(int overload, int outcome) throws Exception {
        Channel channel = mock(Channel.class);
        RpcContext context = new RpcContext();
        context.setChannel(channel);
        Object request = new Object();
        Object response = new Object();
        AbstractNettyRemotingServer server = mock(AbstractNettyRemotingServer.class, CALLS_REAL_METHODS);
        doReturn(new RpcMessage()).when(server).buildRequestMessage(eq(request), anyByte());
        doAnswer(invocation -> {
                    assertEquals(1, context.getActiveCount());
                    if (outcome == 1) {
                        throw new TimeoutException("test timeout");
                    }
                    if (outcome == 2) {
                        throw new IllegalStateException("test send failure");
                    }
                    return response;
                })
                .when(server)
                .sendSync(eq(channel), any(RpcMessage.class), anyLong());
        try (MockedStatic<ChannelManager> manager = mockStatic(ChannelManager.class)) {
            manager.when(() -> ChannelManager.getChannel("resource", "client", false))
                    .thenReturn(channel);
            manager.when(() -> ChannelManager.getChannel("resource", "client", false, BranchType.AT))
                    .thenReturn(channel);
            manager.when(() -> ChannelManager.getContextFromIdentified(channel)).thenReturn(context);
            if (outcome == 0) {
                assertSame(response, send(server, channel, request, overload));
            } else if (outcome == 1) {
                assertThrows(TimeoutException.class, () -> send(server, channel, request, overload));
            } else {
                assertThrows(IllegalStateException.class, () -> send(server, channel, request, overload));
            }
            assertEquals(0, context.getActiveCount());
        }
    }

    private Object send(AbstractNettyRemotingServer server, Channel channel, Object request, int overload)
            throws IOException, TimeoutException {
        if (overload == 0) {
            return server.sendSyncRequest(channel, request);
        }
        if (overload == 1) {
            return server.sendSyncRequest("resource", "client", request, false);
        }
        return server.sendSyncRequest("resource", "client", request, false, BranchType.AT);
    }

    @Test
    void messageConstructionFailureAlsoReleasesCount() {
        Channel channel = mock(Channel.class);
        RpcContext context = new RpcContext();
        AbstractNettyRemotingServer server = mock(AbstractNettyRemotingServer.class, CALLS_REAL_METHODS);
        doThrow(new IllegalArgumentException("test build failure")).when(server).buildRequestMessage(any(), anyByte());
        try (MockedStatic<ChannelManager> manager = mockStatic(ChannelManager.class)) {
            manager.when(() -> ChannelManager.getContextFromIdentified(channel)).thenReturn(context);
            assertThrows(IllegalArgumentException.class, () -> server.sendSyncRequest(channel, new Object()));
            assertEquals(0, context.getActiveCount());
        }
    }

    @Test
    void defaultBranchTypeOverloadDelegatesToLegacyImplementation() throws Exception {
        RemotingServer server = mock(RemotingServer.class, CALLS_REAL_METHODS);
        Object request = new Object();
        Object response = new Object();
        when(server.sendSyncRequest("resource", "client", request, true)).thenReturn(response);
        assertSame(response, server.sendSyncRequest("resource", "client", request, true, BranchType.AT));
        verify(server).sendSyncRequest("resource", "client", request, true);
    }

    @Test
    void directChannelWithoutRegisteredContextCanStillSend() throws Exception {
        Channel channel = mock(Channel.class);
        Object request = new Object();
        Object response = new Object();
        AbstractNettyRemotingServer server = mock(AbstractNettyRemotingServer.class, CALLS_REAL_METHODS);
        doReturn(new RpcMessage()).when(server).buildRequestMessage(eq(request), anyByte());
        doReturn(response).when(server).sendSync(eq(channel), any(RpcMessage.class), anyLong());
        try (MockedStatic<ChannelManager> manager = mockStatic(ChannelManager.class)) {
            assertSame(response, server.sendSyncRequest(channel, request));
        }
    }

    @Test
    void missingChannelFailsBeforeSending() {
        AbstractNettyRemotingServer server = mock(AbstractNettyRemotingServer.class, CALLS_REAL_METHODS);
        try (MockedStatic<ChannelManager> manager = mockStatic(ChannelManager.class)) {
            assertThrows(IOException.class, () -> server.sendSyncRequest((Channel) null, new Object()));
            assertThrows(IOException.class, () -> server.sendSyncRequest("resource", "client", new Object(), false));
            assertThrows(
                    IOException.class,
                    () -> server.sendSyncRequest("resource", "client", new Object(), false, BranchType.AT));
        }
    }
}
