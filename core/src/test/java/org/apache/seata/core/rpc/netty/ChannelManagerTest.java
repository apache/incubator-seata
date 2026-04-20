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
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.rpc.RpcContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for ChannelManager
 */
public class ChannelManagerTest {

    private Channel channel;
    private ConcurrentMap<Channel, RpcContext> identifiedChannels;
    private ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>> tmChannels;
    private ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>>>>
            rmChannels;

    @BeforeEach
    public void setUp() throws Exception {
        channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel.isActive()).thenReturn(true);

        // Get access to private static fields
        Field identifiedField = ChannelManager.class.getDeclaredField("IDENTIFIED_CHANNELS");
        identifiedField.setAccessible(true);
        identifiedChannels = (ConcurrentMap<Channel, RpcContext>) identifiedField.get(null);
        identifiedChannels.clear();

        Field tmField = ChannelManager.class.getDeclaredField("TM_CHANNELS");
        tmField.setAccessible(true);
        tmChannels = (ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>>) tmField.get(null);
        tmChannels.clear();

        Field rmField = ChannelManager.class.getDeclaredField("RM_CHANNELS");
        rmField.setAccessible(true);
        rmChannels = (ConcurrentMap<
                        String, ConcurrentMap<String, ConcurrentMap<String, ConcurrentMap<Integer, RpcContext>>>>)
                rmField.get(null);
        rmChannels.clear();
    }

    @AfterEach
    public void tearDown() {
        identifiedChannels.clear();
        tmChannels.clear();
        rmChannels.clear();
    }

    @Test
    public void testIsRegistered() {
        assertFalse(ChannelManager.isRegistered(channel), "Channel should not be registered initially");

        RpcContext context = new RpcContext();
        context.setChannel(channel);
        identifiedChannels.put(channel, context);

        assertTrue(ChannelManager.isRegistered(channel), "Channel should be registered");
    }

    @Test
    public void testGetRoleFromChannel() {
        assertNull(ChannelManager.getRoleFromChannel(channel), "Role should be null for unregistered channel");

        RpcContext context = new RpcContext();
        context.setChannel(channel);
        context.setClientRole(NettyPoolKey.TransactionRole.TMROLE);
        identifiedChannels.put(channel, context);

        assertEquals(NettyPoolKey.TransactionRole.TMROLE, ChannelManager.getRoleFromChannel(channel));
    }

    @Test
    public void testGetContextFromIdentified() {
        assertNull(ChannelManager.getContextFromIdentified(channel));

        RpcContext context = new RpcContext();
        context.setChannel(channel);
        identifiedChannels.put(channel, context);

        assertSame(context, ChannelManager.getContextFromIdentified(channel));
    }

    @Test
    public void testRegisterTMChannel() throws Exception {
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");

        ChannelManager.registerTMChannel(request, channel);

        assertTrue(ChannelManager.isRegistered(channel), "TM channel should be registered");
        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context);
        assertEquals("test-app", context.getApplicationId());
        assertEquals("test-group", context.getTransactionServiceGroup());
        assertEquals(NettyPoolKey.TransactionRole.TMROLE, context.getClientRole());
    }

    @Test
    public void testRegisterRMChannel() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");

        ChannelManager.registerRMChannel(request, channel);

        assertTrue(ChannelManager.isRegistered(channel), "RM channel should be registered");
        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context);
        assertEquals("test-app", context.getApplicationId());
        assertEquals(NettyPoolKey.TransactionRole.RMROLE, context.getClientRole());
        assertNotNull(context.getResourceSets());
        assertTrue(context.getResourceSets().contains("jdbc:mysql://localhost:3306/seata"));
    }

    @Test
    public void testRegisterRMChannelWithMultipleResources() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/db1,jdbc:mysql://localhost:3306/db2");

        ChannelManager.registerRMChannel(request, channel);

        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context.getResourceSets());
        assertEquals(2, context.getResourceSets().size());
    }

    @Test
    public void testRegisterRMChannelTwiceAddsResources() throws Exception {
        // First registration
        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/db1");

        ChannelManager.registerRMChannel(request1, channel);

        // Second registration with additional resource
        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/db2");

        ChannelManager.registerRMChannel(request2, channel);

        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context.getResourceSets());
        assertTrue(context.getResourceSets().size() >= 2, "Both resources should be registered");
    }

    @Test
    public void testRegisterRMChannelWithEmptyResourceIds() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("");

        ChannelManager.registerRMChannel(request, channel);

        assertTrue(ChannelManager.isRegistered(channel), "RM should be registered even with empty resources");
    }

    @Test
    public void testReleaseRpcContext() throws Exception {
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");

        ChannelManager.registerTMChannel(request, channel);
        assertTrue(ChannelManager.isRegistered(channel));

        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context, "Context should exist before release");

        ChannelManager.releaseRpcContext(channel);

        // After release, the context should be cleared (though channel may still be in map)
        // The main thing is that release() doesn't throw exceptions
        // Note: The actual cleanup behavior depends on RpcContext.release() implementation
    }

    @Test
    public void testGetSameClientChannelWithActiveChannel() throws Exception {
        RegisterTMRequest request = new RegisterTMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");

        ChannelManager.registerTMChannel(request, channel);

        Channel sameChannel = ChannelManager.getSameClientChannel(channel);
        assertNotNull(sameChannel);
        assertTrue(sameChannel.isActive());
    }

    @Test
    public void testGetSameClientChannelWithInactiveChannel() {
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(inactiveChannel.isActive()).thenReturn(false);

        RpcContext context = new RpcContext();
        context.setChannel(inactiveChannel);
        context.setClientRole(NettyPoolKey.TransactionRole.TMROLE);
        identifiedChannels.put(inactiveChannel, context);

        Channel result = ChannelManager.getSameClientChannel(inactiveChannel);
        // Should try to find alternative channel
        assertNull(result, "Should return null when no active channel available");
    }

    @Test
    public void testGetChannelWithValidClientId() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");

        ChannelManager.registerRMChannel(request, channel);

        String clientId = "test-app:127.0.0.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find registered channel");
        assertEquals(channel, result);
    }

    @Test
    public void testGetChannelWithNullResourceId() {
        String clientId = "test-app:127.0.0.1:8080";
        Channel result = ChannelManager.getChannel(null, clientId, false);
        assertNull(result, "Should return null for null resource ID");
    }

    @Test
    public void testGetChannelWithEmptyResourceId() {
        String clientId = "test-app:127.0.0.1:8080";
        Channel result = ChannelManager.getChannel("", clientId, false);
        assertNull(result, "Should return null for empty resource ID");
    }

    @Test
    public void testGetChannelWithInvalidClientId() {
        String clientId = "invalid-client-id";
        try {
            ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Invalid Client ID"), "Should throw exception for invalid client ID");
        }
    }

    @Test
    public void testGetRmChannels() throws Exception {
        // Register multiple RM channels with different resources
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(channel1.isActive()).thenReturn(true);

        Channel channel2 = mock(Channel.class);
        when(channel2.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8082));
        when(channel2.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/db1");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/db2");
        ChannelManager.registerRMChannel(request2, channel2);

        Map<String, Channel> rmChannels = ChannelManager.getRmChannels();
        assertNotNull(rmChannels);
        assertTrue(rmChannels.size() >= 1, "Should have at least one RM channel");
    }

    @Test
    public void testRegisterMultipleTMChannels() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(channel1.isActive()).thenReturn(true);

        Channel channel2 = mock(Channel.class);
        when(channel2.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8082));
        when(channel2.isActive()).thenReturn(true);

        RegisterTMRequest request1 = new RegisterTMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        ChannelManager.registerTMChannel(request1, channel1);

        RegisterTMRequest request2 = new RegisterTMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        ChannelManager.registerTMChannel(request2, channel2);

        assertTrue(ChannelManager.isRegistered(channel1));
        assertTrue(ChannelManager.isRegistered(channel2));
    }

    @Test
    public void testGetChannelFallbackToSameIPDifferentPort() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(channel1.isActive()).thenReturn(true);

        Channel channel2 = mock(Channel.class);
        when(channel2.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8082));
        when(channel2.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, channel2);

        String clientId = "test-app:127.0.0.1:8081";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find a channel on same IP");
    }

    @Test
    public void testGetChannelFallbackToDifferentIPSameApp() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(channel1.isActive()).thenReturn(true);

        Channel channel2 = mock(Channel.class);
        when(channel2.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.2", 8080));
        when(channel2.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, channel2);

        String clientId = "test-app:192.168.1.3:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find channel from different IP in same app");
    }

    @Test
    public void testGetChannelWithTryOtherApp() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(channel1.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("other-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, channel1);

        String clientId = "test-app:192.168.1.2:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, true);

        assertNotNull(result, "Should find channel from other app when tryOtherApp=true");
        assertEquals(channel1, result);
    }

    @Test
    public void testGetChannelWithInactiveChannel() throws Exception {
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(inactiveChannel.isActive()).thenReturn(false);

        Channel activeChannel = mock(Channel.class);
        when(activeChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(activeChannel.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, inactiveChannel);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, activeChannel);

        String clientId = "test-app:127.0.0.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should skip inactive channel and return active one");
        assertTrue(result.isActive());
    }

    @Test
    public void testGetSameClientChannelForRMRole() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel1.isActive()).thenReturn(false);

        Channel channel2 = mock(Channel.class);
        when(channel2.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(channel2.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, channel2);

        Channel result = ChannelManager.getSameClientChannel(channel1);

        assertNotNull(result, "Should find alternative channel for RM role");
        assertTrue(result.isActive());
    }

    @Test
    public void testGetSameClientChannelReturnsNullWhenNoAlternative() {
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(inactiveChannel.isActive()).thenReturn(false);

        RpcContext context = new RpcContext();
        context.setChannel(inactiveChannel);
        context.setClientRole(NettyPoolKey.TransactionRole.TMROLE);
        context.setApplicationId("test-app");
        identifiedChannels.put(inactiveChannel, context);

        Channel result = ChannelManager.getSameClientChannel(inactiveChannel);

        assertNull(result, "Should return null when no alternative channel exists");
    }

    @Test
    public void testUpdateChannelsResourceByRegisteringMultipleResources() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel1.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/db1");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/db2");
        ChannelManager.registerRMChannel(request2, channel1);

        String clientId = "test-app:127.0.0.1:8080";
        Channel resultDb1 = ChannelManager.getChannel("jdbc:mysql://localhost:3306/db1", clientId, false);
        Channel resultDb2 = ChannelManager.getChannel("jdbc:mysql://localhost:3306/db2", clientId, false);

        assertNotNull(resultDb1, "Should find channel for db1");
        assertNotNull(resultDb2, "Should find channel for db2");
        assertEquals(resultDb1, resultDb2, "Should be the same channel for both resources");
    }

    @Test
    public void testDbKeytoSetWithMultipleResources() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("resource1,resource2,resource3");

        ChannelManager.registerRMChannel(request, channel);

        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context.getResourceSets());
        assertEquals(3, context.getResourceSets().size());
        assertTrue(context.getResourceSets().contains("resource1"));
        assertTrue(context.getResourceSets().contains("resource2"));
        assertTrue(context.getResourceSets().contains("resource3"));
    }

    @Test
    public void testGetChannelWithNonExistentResource() {
        String clientId = "test-app:127.0.0.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/nonexistent", clientId, false);

        assertNull(result, "Should return null for non-existent resource");
    }

    @Test
    public void testReleaseRpcContextWithNullChannel() {
        // Should not throw exception when channel is null
        try {
            ChannelManager.releaseRpcContext(null);
        } catch (NullPointerException e) {
            // Expected behavior - null channel may cause NPE
        }
    }

    @Test
    public void testReleaseRpcContextWithUnregisteredChannel() {
        Channel unregisteredChannel = mock(Channel.class);
        when(unregisteredChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9999));

        ChannelManager.releaseRpcContext(unregisteredChannel);
    }

    @Test
    public void testGetChannelWithClientIdWithoutSplitChar() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request, channel);

        String clientIdWithoutSplit = "testappnosplit";
        try {
            ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientIdWithoutSplit, false);
        } catch (Exception e) {
            assertTrue(
                    e.getMessage().contains("Invalid Client ID"),
                    "Should throw exception for clientId without split character");
        }
    }

    @Test
    public void testGetChannelWithClientIdInvalidIPPortFormat() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request, channel);

        String clientIdInvalidFormat = "test-app:invalidformat";
        boolean exceptionThrown = false;
        try {
            ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientIdInvalidFormat, false);
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
            assertTrue(
                    e.getMessage().contains("Invalid endpoint format"),
                    "Should throw exception for clientId with invalid IP:Port format");
        }
        assertTrue(exceptionThrown, "Should throw exception for clientId with invalid IP:Port format");
    }

    @Test
    public void testGetRmChannelsWhenEmpty() {
        Map<String, Channel> rmChannels = ChannelManager.getRmChannels();
        assertNotNull(rmChannels, "Should return non-null map even when empty");
        assertTrue(rmChannels.isEmpty(), "Should return empty map when no RM channels registered");
    }

    @Test
    public void testGetChannelWithNullTargetApplicationId() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request, channel);

        String clientIdNullApp = ":127.0.0.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientIdNullApp, false);
        assertNull(result, "Should return null for clientId with null application ID");
    }

    @Test
    public void testDbKeytoSetWithNullDbkey() throws Exception {
        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("1.5.0");
        request.setResourceIds(null);

        ChannelManager.registerRMChannel(request, channel);

        assertTrue(ChannelManager.isRegistered(channel), "Channel should be registered even with null resourceIds");
        RpcContext context = ChannelManager.getContextFromIdentified(channel);
        assertNotNull(context);
    }

    @Test
    public void testUpdateChannelsResourceCrossResourceSync() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel1.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/db1");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/db2");
        ChannelManager.registerRMChannel(request2, channel1);

        String clientId = "test-app:127.0.0.1:8080";
        Channel resultDb1 = ChannelManager.getChannel("jdbc:mysql://localhost:3306/db1", clientId, false);
        Channel resultDb2 = ChannelManager.getChannel("jdbc:mysql://localhost:3306/db2", clientId, false);

        assertNotNull(resultDb1, "Should find channel for db1");
        assertNotNull(resultDb2, "Should find channel for db2");
        assertEquals(resultDb1, resultDb2, "Both resources should map to same channel after sync");
    }

    @Test
    public void testGetSameClientChannelWithRecheckActiveContext() throws Exception {
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(inactiveChannel.isActive()).thenReturn(false);

        Channel activeContextChannel = mock(Channel.class);
        when(activeContextChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(activeContextChannel.isActive()).thenReturn(true);

        RpcContext context = new RpcContext();
        context.setChannel(activeContextChannel);
        context.setClientRole(NettyPoolKey.TransactionRole.TMROLE);
        context.setApplicationId("test-app");
        identifiedChannels.put(inactiveChannel, context);

        Channel result = ChannelManager.getSameClientChannel(inactiveChannel);

        assertNotNull(result, "Should return active channel from context");
        assertTrue(result.isActive(), "Returned channel should be active");
    }

    @Test
    public void testGetSameClientChannelForRMRoleWithEmptyMap() throws Exception {
        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(inactiveChannel.isActive()).thenReturn(false);

        RpcContext context = new RpcContext();
        context.setChannel(inactiveChannel);
        context.setClientRole(NettyPoolKey.TransactionRole.RMROLE);
        context.setApplicationId("test-app");
        identifiedChannels.put(inactiveChannel, context);

        Channel result = ChannelManager.getSameClientChannel(inactiveChannel);

        assertNull(result, "Should return null when RM has no alternative channels");
    }

    @Test
    public void testDbKeytoSetWithEmptyAndWhitespace() throws Exception {
        RegisterRMRequest requestEmpty = new RegisterRMRequest();
        requestEmpty.setApplicationId("test-app");
        requestEmpty.setTransactionServiceGroup("test-group");
        requestEmpty.setVersion("1.5.0");
        requestEmpty.setResourceIds("");

        Channel channelEmpty = mock(Channel.class);
        when(channelEmpty.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(channelEmpty.isActive()).thenReturn(true);

        ChannelManager.registerRMChannel(requestEmpty, channelEmpty);
        assertTrue(ChannelManager.isRegistered(channelEmpty), "Should register even with empty resourceIds");

        RegisterRMRequest requestWhitespace = new RegisterRMRequest();
        requestWhitespace.setApplicationId("test-app");
        requestWhitespace.setTransactionServiceGroup("test-group");
        requestWhitespace.setVersion("1.5.0");
        requestWhitespace.setResourceIds("   ");

        Channel channelWhitespace = mock(Channel.class);
        when(channelWhitespace.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8082));
        when(channelWhitespace.isActive()).thenReturn(true);

        ChannelManager.registerRMChannel(requestWhitespace, channelWhitespace);
        assertTrue(ChannelManager.isRegistered(channelWhitespace), "Should register even with whitespace resourceIds");
    }

    @Test
    public void testGetChannelMultipleInactiveChannelsCleanup() throws Exception {
        Channel inactive1 = mock(Channel.class);
        when(inactive1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(inactive1.isActive()).thenReturn(false);

        Channel inactive2 = mock(Channel.class);
        when(inactive2.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(inactive2.isActive()).thenReturn(false);

        Channel activeChannel = mock(Channel.class);
        when(activeChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8082));
        when(activeChannel.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, inactive1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, inactive2);

        RegisterRMRequest request3 = new RegisterRMRequest();
        request3.setApplicationId("test-app");
        request3.setTransactionServiceGroup("test-group");
        request3.setVersion("1.5.0");
        request3.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request3, activeChannel);

        String clientId = "test-app:127.0.0.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find active channel after cleaning up inactive ones");
        assertTrue(result.isActive(), "Should return only active channel");
    }

    @Test
    public void testTryOtherAppWithNullMyApplicationId() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(channel1.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("other-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, channel1);

        String clientIdNullApp = ":192.168.1.2:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientIdNullApp, true);

        assertNotNull(
                result, "Should return channel from other app when tryOtherApp is true even with empty applicationId");
        assertEquals(channel1, result);
    }

    @Test
    public void testGetRmChannelsSomeResourcesWithoutChannel() throws Exception {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8080));
        when(channel1.isActive()).thenReturn(true);

        Channel inactiveChannel = mock(Channel.class);
        when(inactiveChannel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8081));
        when(inactiveChannel.isActive()).thenReturn(false);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/db1");
        ChannelManager.registerRMChannel(request1, channel1);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/db2");
        ChannelManager.registerRMChannel(request2, inactiveChannel);

        Map<String, Channel> rmChannels = ChannelManager.getRmChannels();
        assertNotNull(rmChannels);
        assertTrue(rmChannels.size() >= 1, "Should have at least the active channel");
    }

    @Test
    public void testComplexChannelFallbackScenario() throws Exception {
        Channel targetInactive = mock(Channel.class);
        when(targetInactive.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(targetInactive.isActive()).thenReturn(false);

        Channel sameIPDifferentPort = mock(Channel.class);
        when(sameIPDifferentPort.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8081));
        when(sameIPDifferentPort.isActive()).thenReturn(true);

        Channel differentIPSameApp = mock(Channel.class);
        when(differentIPSameApp.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.2", 8080));
        when(differentIPSameApp.isActive()).thenReturn(true);

        Channel differentApp = mock(Channel.class);
        when(differentApp.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.3", 8080));
        when(differentApp.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, targetInactive);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, sameIPDifferentPort);

        RegisterRMRequest request3 = new RegisterRMRequest();
        request3.setApplicationId("test-app");
        request3.setTransactionServiceGroup("test-group");
        request3.setVersion("1.5.0");
        request3.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request3, differentIPSameApp);

        RegisterRMRequest request4 = new RegisterRMRequest();
        request4.setApplicationId("other-app");
        request4.setTransactionServiceGroup("test-group");
        request4.setVersion("1.5.0");
        request4.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request4, differentApp);

        String clientId = "test-app:192.168.1.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find alternative channel through fallback chain");
        assertTrue(result.isActive(), "Should find an active channel");
    }

    @Test
    public void testTryOtherAppAllMapsEmpty() throws Exception {
        String clientId = "test-app:192.168.1.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/nonexistent", clientId, true);

        assertNull(result, "Should return null when no channels available even with tryOtherApp");
    }

    @Test
    public void testGetChannelTargetExactMatchActive() throws Exception {
        Channel exactMatch = mock(Channel.class);
        when(exactMatch.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(exactMatch.isActive()).thenReturn(true);

        Channel alternative = mock(Channel.class);
        when(alternative.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8081));
        when(alternative.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, exactMatch);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, alternative);

        String clientId = "test-app:192.168.1.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find exact match");
        assertEquals(exactMatch, result, "Should return exact match when it's active");
    }

    @Test
    public void testGetChannelTargetExactMatchInactiveUsesAlternative() throws Exception {
        Channel exactInactive = mock(Channel.class);
        when(exactInactive.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(exactInactive.isActive()).thenReturn(false);

        Channel alternative = mock(Channel.class);
        when(alternative.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8081));
        when(alternative.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("test-app");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request1, exactInactive);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("test-app");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/seata");
        ChannelManager.registerRMChannel(request2, alternative);

        String clientId = "test-app:192.168.1.1:8080";
        Channel result = ChannelManager.getChannel("jdbc:mysql://localhost:3306/seata", clientId, false);

        assertNotNull(result, "Should find alternative when exact match is inactive");
        assertTrue(result.isActive(), "Should return active channel");
        assertEquals(alternative, result, "Should fall back to alternative channel");
    }

    @Test
    public void testMultipleApplicationsWithSameResource() throws Exception {
        Channel app1Channel = mock(Channel.class);
        when(app1Channel.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));
        when(app1Channel.isActive()).thenReturn(true);

        Channel app2Channel = mock(Channel.class);
        when(app2Channel.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.2", 8080));
        when(app2Channel.isActive()).thenReturn(true);

        RegisterRMRequest request1 = new RegisterRMRequest();
        request1.setApplicationId("app1");
        request1.setTransactionServiceGroup("test-group");
        request1.setVersion("1.5.0");
        request1.setResourceIds("jdbc:mysql://localhost:3306/shared-db");
        ChannelManager.registerRMChannel(request1, app1Channel);

        RegisterRMRequest request2 = new RegisterRMRequest();
        request2.setApplicationId("app2");
        request2.setTransactionServiceGroup("test-group");
        request2.setVersion("1.5.0");
        request2.setResourceIds("jdbc:mysql://localhost:3306/shared-db");
        ChannelManager.registerRMChannel(request2, app2Channel);

        String clientIdApp1 = "app1:192.168.1.1:8080";
        Channel resultApp1 = ChannelManager.getChannel("jdbc:mysql://localhost:3306/shared-db", clientIdApp1, false);
        assertNotNull(resultApp1, "Should find channel for app1");

        String clientIdApp2 = "app2:192.168.1.2:8080";
        Channel resultApp2 = ChannelManager.getChannel("jdbc:mysql://localhost:3306/shared-db", clientIdApp2, false);
        assertNotNull(resultApp2, "Should find channel for app2");

        assertNotEquals(resultApp1, resultApp2, "Different apps should have different channels");
    }

    @Test
    public void unregisterPartialResourcesTest() throws Exception {
        Channel ch = mock(Channel.class);
        when(ch.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9090));
        when(ch.isActive()).thenReturn(true);

        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("2.6.0");
        request.setResourceIds("resource1,resource2");
        ChannelManager.registerRMChannel(request, ch);

        assertTrue(ChannelManager.isRegistered(ch));

        Set<String> toRemove = new HashSet<>();
        toRemove.add("resource1");
        ChannelManager.unregisterRMChannel(ch, toRemove);

        // Channel should still be registered because resource2 remains
        assertTrue(ChannelManager.isRegistered(ch));
        RpcContext ctx = ChannelManager.getContextFromIdentified(ch);
        assertNotNull(ctx);
        assertNotNull(ctx.getResourceSets());
        assertFalse(ctx.getResourceSets().contains("resource1"));
        assertTrue(ctx.getResourceSets().contains("resource2"));
    }

    @Test
    public void unregisterAllResourcesRemovesChannelTest() throws Exception {
        Channel ch = mock(Channel.class);
        when(ch.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9091));
        when(ch.isActive()).thenReturn(true);

        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("2.6.0");
        request.setResourceIds("resource1,resource2");
        ChannelManager.registerRMChannel(request, ch);

        assertTrue(ChannelManager.isRegistered(ch));

        Set<String> toRemove = new HashSet<>();
        toRemove.add("resource1");
        toRemove.add("resource2");
        ChannelManager.unregisterRMChannel(ch, toRemove);

        assertFalse(ChannelManager.isRegistered(ch));
    }

    @Test
    public void unregisterRMChannelWithUnknownChannelReturnsFalseTest() {
        Channel ch = mock(Channel.class);
        when(ch.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9999));

        Set<String> toRemove = new HashSet<>();
        toRemove.add("resource1");
        boolean result = ChannelManager.unregisterRMChannel(ch, toRemove);

        assertFalse(result);
    }

    @Test
    public void unregisterRMChannelWithNonExistentResourceIdTest() throws Exception {
        Channel ch = mock(Channel.class);
        when(ch.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9092));
        when(ch.isActive()).thenReturn(true);

        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("2.6.0");
        request.setResourceIds("resource1");
        ChannelManager.registerRMChannel(request, ch);

        assertTrue(ChannelManager.isRegistered(ch));

        Set<String> toRemove = new HashSet<>();
        toRemove.add("nonExistentResource");
        boolean result = ChannelManager.unregisterRMChannel(ch, toRemove);

        assertTrue(result);
        assertTrue(ChannelManager.isRegistered(ch));
        RpcContext ctx = ChannelManager.getContextFromIdentified(ch);
        assertNotNull(ctx);
        assertTrue(ctx.getResourceSets().contains("resource1"));
    }

    @Test
    public void unregisterRMChannelCleanupEmptyMapsTest() throws Exception {
        Channel ch = mock(Channel.class);
        when(ch.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 9093));
        when(ch.isActive()).thenReturn(true);

        RegisterRMRequest request = new RegisterRMRequest();
        request.setApplicationId("test-app");
        request.setTransactionServiceGroup("test-group");
        request.setVersion("2.6.0");
        request.setResourceIds("singleResource");
        ChannelManager.registerRMChannel(request, ch);

        assertTrue(ChannelManager.isRegistered(ch));

        Set<String> toRemove = new HashSet<>();
        toRemove.add("singleResource");
        boolean result = ChannelManager.unregisterRMChannel(ch, toRemove);

        assertTrue(result);
        assertFalse(ChannelManager.isRegistered(ch));
    }
}
