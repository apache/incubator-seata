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
import io.netty.channel.ChannelFuture;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.Resource;
import org.apache.seata.core.model.ResourceManager;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Rm RPC client test.
 */
@Order(2)
class RmNettyClientTest {

    @BeforeAll
    public static void beforeAll() {
        RmNettyRemotingClient.getInstance().destroy();
    }

    @AfterAll
    public static void afterAll() {
        RmNettyRemotingClient.getInstance().destroy();
    }

    @Test
    public void assertGetInstanceAfterDestroy() {
        RmNettyRemotingClient oldClient = RmNettyRemotingClient.getInstance("ap", "group");
        AtomicBoolean initialized = getInitializeStatus(oldClient);
        oldClient.init();
        assertTrue(initialized.get());
        oldClient.destroy();
        assertFalse(initialized.get());
        RmNettyRemotingClient newClient = RmNettyRemotingClient.getInstance("ap", "group");
        Assertions.assertNotEquals(oldClient, newClient);
        initialized = getInitializeStatus(newClient);
        assertFalse(initialized.get());
        newClient.init();
        assertTrue(initialized.get());
        newClient.destroy();
    }

    private AtomicBoolean getInitializeStatus(final RmNettyRemotingClient rmNettyRemotingClient) {
        try {
            Field field = rmNettyRemotingClient.getClass().getDeclaredField("initialized");
            field.setAccessible(true);
            return (AtomicBoolean) field.get(rmNettyRemotingClient);
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    @Test
    public void testSendAsyncRequestWithNullChannelLogsWarning() {
        RmNettyRemotingClient remotingClient = RmNettyRemotingClient.getInstance();
        Object message = HeartbeatMessage.PING;
        assertThrows(FrameworkException.class, () -> {
            remotingClient.sendAsyncRequest(null, message);
        });
    }

    @Test
    public void testRegisterResourceWithBlankTransactionServiceGroup() {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance();
        client.setTransactionServiceGroup(null);

        ResourceManager resourceManager = mock(ResourceManager.class);
        client.setResourceManager(resourceManager);

        client.registerResource("group1", "jdbc:mysql://localhost:3306/test");

        verify(resourceManager, never()).getManagedResources();
    }

    @Test
    public void testRegisterResourceWithBlankResourceId() {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        client.setResourceManager(resourceManager);

        client.registerResource("group1", "");
        client.registerResource("group1", null);

        verify(resourceManager, never()).getManagedResources();
    }

    @Test
    public void testRegisterResourceWithEmptyChannels() {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        client.setResourceManager(resourceManager);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        when(channelManager.getChannels()).thenReturn(new ConcurrentHashMap<>());

        try {
            setChannelManager(client, channelManager);

            System.setProperty(ConfigurationKeys.ENABLE_RM_CLIENT_CHANNEL_CHECK_FAIL_FAST, "false");
            ConfigurationCache.clear();

            client.registerResource("group1", "jdbc:mysql://localhost:3306/test");

            verify(channelManager).initReconnect(eq("test_group"), eq(false));
        } catch (Exception e) {
            Assertions.fail("Should not throw exception: " + e.getMessage());
        } finally {
            System.clearProperty(ConfigurationKeys.ENABLE_RM_CLIENT_CHANNEL_CHECK_FAIL_FAST);
            ConfigurationCache.clear();
        }
    }

    @Test
    public void testOnRegisterMsgSuccess() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        Map<String, Resource> resourceMap = new HashMap<>();
        Resource mockResource = mock(Resource.class);
        resourceMap.put("jdbc:mysql://localhost:3306/db1", mockResource);
        when(resourceManager.getManagedResources()).thenReturn(resourceMap);
        client.setResourceManager(resourceManager);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new java.net.InetSocketAddress("127.0.0.1", 8091));
        String serverAddress = "127.0.0.1:8091";

        RegisterRMRequest request = new RegisterRMRequest("app", "test_group");
        request.setResourceIds("jdbc:mysql://localhost:3306/db1");

        RegisterRMResponse response = new RegisterRMResponse();
        response.setVersion("1.5.0");
        response.setResultCode(ResultCode.Success);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        setChannelManager(client, channelManager);

        client.onRegisterMsgSuccess(serverAddress, channel, response, request);

        verify(channelManager).registerChannel(eq(serverAddress), eq(channel), anyString());
    }

    @Test
    public void testOnRegisterMsgSuccessWithDifferentResourceIds() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        Map<String, Resource> resourceMap = new HashMap<>();
        Resource mockResource1 = mock(Resource.class);
        Resource mockResource2 = mock(Resource.class);
        resourceMap.put("jdbc:mysql://localhost:3306/db1", mockResource1);
        resourceMap.put("jdbc:mysql://localhost:3306/db2", mockResource2);
        when(resourceManager.getManagedResources()).thenReturn(resourceMap);
        client.setResourceManager(resourceManager);

        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new java.net.InetSocketAddress("127.0.0.1", 8091));
        String serverAddress = "127.0.0.1:8091";

        RegisterRMRequest request = new RegisterRMRequest("app", "test_group");
        request.setResourceIds("jdbc:mysql://localhost:3306/db1");

        RegisterRMResponse response = new RegisterRMResponse();
        response.setVersion("1.5.0");
        response.setResultCode(ResultCode.Success);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        setChannelManager(client, channelManager);

        client.onRegisterMsgSuccess(serverAddress, channel, response, request);

        verify(channelManager).registerChannel(eq(serverAddress), eq(channel), anyString());
    }

    @Test
    public void testOnRegisterMsgFail() {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        Channel channel = mock(Channel.class);
        String serverAddress = "127.0.0.1:8091";

        RegisterRMRequest request = new RegisterRMRequest("app", "test_group");
        request.setVersion("1.0.0");

        RegisterRMResponse response = new RegisterRMResponse();
        response.setVersion("1.5.0");
        response.setResultCode(ResultCode.Failed);
        response.setMsg("Registration failed");

        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            client.onRegisterMsgFail(serverAddress, channel, response, request);
        });

        assertTrue(exception.getMessage().contains("register RM failed"));
        assertTrue(exception.getMessage().contains("Registration failed"));
    }

    @Test
    public void testGetMergedResourceKeys() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        Map<String, Resource> resourceMap = new HashMap<>();
        Resource mockResource1 = mock(Resource.class);
        Resource mockResource2 = mock(Resource.class);
        Resource mockResource3 = mock(Resource.class);
        resourceMap.put("jdbc:mysql://localhost:3306/db1", mockResource1);
        resourceMap.put("jdbc:mysql://localhost:3306/db2", mockResource2);
        resourceMap.put("jdbc:mysql://localhost:3306/db3", mockResource3);
        when(resourceManager.getManagedResources()).thenReturn(resourceMap);
        client.setResourceManager(resourceManager);

        String mergedKeys = client.getMergedResourceKeys();

        assertNotNull(mergedKeys);
        assertTrue(mergedKeys.contains("jdbc:mysql://localhost:3306/db1"));
        assertTrue(mergedKeys.contains("jdbc:mysql://localhost:3306/db2"));
        assertTrue(mergedKeys.contains("jdbc:mysql://localhost:3306/db3"));
        assertTrue(mergedKeys.contains(","));
    }

    @Test
    public void testGetMergedResourceKeysWithEmptyResources() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        when(resourceManager.getManagedResources()).thenReturn(new HashMap<>());
        client.setResourceManager(resourceManager);

        String mergedKeys = client.getMergedResourceKeys();

        assertNull(mergedKeys);
    }

    @Test
    public void testGetMergedResourceKeysWithBlankResourceId() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        Map<String, Resource> resourceMap = new HashMap<>();
        Resource mockResource1 = mock(Resource.class);
        Resource mockResource2 = mock(Resource.class);
        resourceMap.put("jdbc:mysql://localhost:3306/db1", mockResource1);
        resourceMap.put("", mockResource2);
        resourceMap.put("jdbc:mysql://localhost:3306/db3", mockResource2);
        when(resourceManager.getManagedResources()).thenReturn(resourceMap);
        client.setResourceManager(resourceManager);

        String mergedKeys = client.getMergedResourceKeys();

        assertNotNull(mergedKeys);
        assertFalse(mergedKeys.contains(",,"));
    }

    @Test
    public void testSendRegisterMessageWithChannelNotWritable() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        Channel channel = mock(Channel.class);
        String serverAddress = "127.0.0.1:8091";
        String resourceId = "jdbc:mysql://localhost:3306/test";

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        setChannelManager(client, channelManager);

        RmNettyRemotingClient spyClient = Mockito.spy(client);
        doThrow(new FrameworkException("Channel is not writable", FrameworkErrorCode.ChannelIsNotWritable))
                .when(spyClient)
                .sendAsyncRequest(eq(channel), any(RegisterRMRequest.class));

        spyClient.sendRegisterMessage(serverAddress, channel, resourceId);

        verify(channelManager).releaseChannel(eq(channel), eq(serverAddress));
    }

    @Test
    public void testSendRegisterMessageWithOtherException() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        Channel channel = mock(Channel.class);
        String serverAddress = "127.0.0.1:8091";
        String resourceId = "jdbc:mysql://localhost:3306/test";

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        setChannelManager(client, channelManager);

        RmNettyRemotingClient spyClient = Mockito.spy(client);
        doThrow(new FrameworkException("Other error", FrameworkErrorCode.UnknownAppError))
                .when(spyClient)
                .sendAsyncRequest(eq(channel), any(RegisterRMRequest.class));

        spyClient.sendRegisterMessage(serverAddress, channel, resourceId);

        verify(channelManager, never()).releaseChannel(any(), anyString());
    }

    @Test
    public void testGetPoolKeyFunction() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ResourceManager resourceManager = mock(ResourceManager.class);
        Map<String, Resource> resourceMap = new HashMap<>();
        Resource mockResource = mock(Resource.class);
        resourceMap.put("jdbc:mysql://localhost:3306/db1", mockResource);
        when(resourceManager.getManagedResources()).thenReturn(resourceMap);
        client.setResourceManager(resourceManager);

        Method method = client.getClass().getDeclaredMethod("getPoolKeyFunction");
        method.setAccessible(true);

        Object function = method.invoke(client);
        assertNotNull(function);
    }

    @Test
    public void unregisterResourceWithBlankParamsTest() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        setChannelManager(client, channelManager);

        client.setTransactionServiceGroup(null);
        client.unregisterResource("group1", "jdbc:mysql://localhost:3306/test");
        verify(channelManager, never()).getChannels();

        client.setTransactionServiceGroup("test_group");
        client.unregisterResource("group1", "");
        verify(channelManager, never()).getChannels();

        client.unregisterResource("group1", null);
        verify(channelManager, never()).getChannels();
    }

    @Test
    public void unregisterResourceWithActiveChannelTest() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        ChannelFuture channelFuture = mock(ChannelFuture.class);
        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(true);
        when(channel.writeAndFlush(any())).thenReturn(channelFuture);

        String serverAddress = "127.0.0.1:8091";
        ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
        channels.put(serverAddress, channel);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        when(channelManager.getChannels()).thenReturn(channels);
        setChannelManager(client, channelManager);

        when(channelManager.getServerVersion(serverAddress)).thenReturn("2.6.0");

        client.unregisterResource("group1", "jdbc:mysql://localhost:3306/test");

        verify(channel).writeAndFlush(any());
    }

    @Test
    public void unregisterResourceSkipsOldServerVersionTest() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);

        String serverAddress = "127.0.0.1:8091";
        ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
        channels.put(serverAddress, channel);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        when(channelManager.getChannels()).thenReturn(channels);
        setChannelManager(client, channelManager);

        when(channelManager.getServerVersion(serverAddress)).thenReturn("2.5.0");

        client.unregisterResource("group1", "jdbc:mysql://localhost:3306/test");

        verify(channel, never()).writeAndFlush(any());
    }

    @Test
    public void unregisterResourceSkipsInactiveChannelTest() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(false);

        String serverAddress = "127.0.0.1:8091";
        ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
        channels.put(serverAddress, channel);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        when(channelManager.getChannels()).thenReturn(channels);
        setChannelManager(client, channelManager);

        client.unregisterResource("group1", "jdbc:mysql://localhost:3306/test");

        verify(channel, never()).writeAndFlush(any());
    }

    @Test
    public void unregisterResourceHandlesChannelNotWritableTest() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        Channel channel = mock(Channel.class);
        when(channel.isActive()).thenReturn(true);
        when(channel.isWritable()).thenReturn(false);

        String serverAddress = "127.0.0.1:8091";
        ConcurrentHashMap<String, Channel> channels = new ConcurrentHashMap<>();
        channels.put(serverAddress, channel);

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        when(channelManager.getChannels()).thenReturn(channels);
        setChannelManager(client, channelManager);

        when(channelManager.getServerVersion(serverAddress)).thenReturn("2.6.0");

        client.unregisterResource("group1", "jdbc:mysql://localhost:3306/test");

        verify(channelManager).releaseChannel(eq(channel), eq(serverAddress));
    }

    @Test
    public void unregisterResourceHandlesNullChannelsTest() throws Exception {
        RmNettyRemotingClient client = RmNettyRemotingClient.getInstance("app", "test_group");

        NettyClientChannelManager channelManager = mock(NettyClientChannelManager.class);
        when(channelManager.getChannels()).thenReturn(null);
        setChannelManager(client, channelManager);

        Assertions.assertDoesNotThrow(() -> client.unregisterResource("group1", "jdbc:mysql://localhost:3306/test"));
    }

    private void setChannelManager(RmNettyRemotingClient client, NettyClientChannelManager channelManager)
            throws Exception {
        Field field = AbstractNettyRemotingClient.class.getDeclaredField("clientChannelManager");
        field.setAccessible(true);
        field.set(client, channelManager);
    }

    private Method getBatchConfigListener(RmNettyRemotingClient client) {
        try {
            Field field = client.getClass().getDeclaredField("enableClientBatchSendRequest");
            field.setAccessible(true);
            return null;
        } catch (Exception e) {
            return null;
        }
    }
}
