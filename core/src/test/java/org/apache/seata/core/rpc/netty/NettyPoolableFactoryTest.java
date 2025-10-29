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
import org.apache.seata.common.exception.FrameworkException;
import org.apache.seata.core.protocol.AbstractMessage;
import org.apache.seata.core.protocol.RegisterRMRequest;
import org.apache.seata.core.protocol.RegisterRMResponse;
import org.apache.seata.core.protocol.RegisterTMRequest;
import org.apache.seata.core.protocol.RegisterTMResponse;
import org.apache.seata.core.protocol.ResultCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NettyPoolableFactoryTest {

    private NettyPoolableFactory factory;
    private AbstractNettyRemotingClient remotingClient;
    private NettyClientBootstrap clientBootstrap;

    @BeforeEach
    public void setUp() {
        remotingClient = mock(AbstractNettyRemotingClient.class);
        clientBootstrap = mock(NettyClientBootstrap.class);
        factory = new NettyPoolableFactory(remotingClient, clientBootstrap);
    }

    @Test
    public void testMakeObjectSuccessForRM() throws Exception {
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.isActive()).thenReturn(true);

        when(clientBootstrap.getNewChannel(any(InetSocketAddress.class))).thenReturn(mockChannel);

        RegisterRMRequest request = new RegisterRMRequest("app", "tx_group");
        RegisterRMResponse response = new RegisterRMResponse();
        response.setResultCode(ResultCode.Success);
        response.setIdentified(true);
        response.setVersion("1.5.0");

        when(remotingClient.sendSyncRequest(eq(mockChannel), any(AbstractMessage.class)))
                .thenReturn(response);

        NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", request);

        Channel result = factory.makeObject(key);

        assertNotNull(result);
        assertEquals(mockChannel, result);
        verify(remotingClient).onRegisterMsgSuccess(eq("127.0.0.1:8091"), eq(mockChannel), eq(response), eq(request));
    }

    @Test
    public void testMakeObjectSuccessForTM() throws Exception {
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.isActive()).thenReturn(true);

        when(clientBootstrap.getNewChannel(any(InetSocketAddress.class))).thenReturn(mockChannel);

        RegisterTMRequest request = new RegisterTMRequest("app", "tx_group");
        RegisterTMResponse response = new RegisterTMResponse();
        response.setResultCode(ResultCode.Success);
        response.setIdentified(true);
        response.setVersion("1.5.0");

        when(remotingClient.sendSyncRequest(eq(mockChannel), any(AbstractMessage.class)))
                .thenReturn(response);

        NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.TMROLE, "127.0.0.1:8091", request);

        Channel result = factory.makeObject(key);

        assertNotNull(result);
        assertEquals(mockChannel, result);
        verify(remotingClient).onRegisterMsgSuccess(eq("127.0.0.1:8091"), eq(mockChannel), eq(response), eq(request));
    }

    @Test
    public void testMakeObjectRegisterFailure() throws Exception {
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.isActive()).thenReturn(true);

        when(clientBootstrap.getNewChannel(any(InetSocketAddress.class))).thenReturn(mockChannel);

        RegisterRMRequest request = new RegisterRMRequest("app", "tx_group");
        RegisterRMResponse response = new RegisterRMResponse();
        response.setResultCode(ResultCode.Failed);
        response.setIdentified(false);
        response.setMsg("Registration failed");

        when(remotingClient.sendSyncRequest(eq(mockChannel), any(AbstractMessage.class)))
                .thenReturn(response);

        NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", request);

        factory.makeObject(key);

        verify(remotingClient).onRegisterMsgFail(eq("127.0.0.1:8091"), eq(mockChannel), eq(response), eq(request));
        verify(remotingClient, never()).onRegisterMsgSuccess(any(), any(), any(), any());
    }

    @Test
    public void testMakeObjectWithException() throws Exception {
        Channel mockChannel = mock(Channel.class);

        when(clientBootstrap.getNewChannel(any(InetSocketAddress.class))).thenReturn(mockChannel);

        RegisterRMRequest request = new RegisterRMRequest("app", "tx_group");

        when(remotingClient.sendSyncRequest(eq(mockChannel), any(AbstractMessage.class)))
                .thenThrow(new RuntimeException("Connection failed"));

        NettyPoolKey key = new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", request);

        FrameworkException exception = assertThrows(FrameworkException.class, () -> {
            factory.makeObject(key);
        });

        assertTrue(exception.getMessage().contains("register"));
        assertTrue(exception.getMessage().contains("RMROLE"));
        verify(mockChannel).close();
    }

    @Test
    public void testDestroyObjectWithValidChannel() throws Exception {
        Channel mockChannel = mock(Channel.class);

        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        factory.destroyObject(key, mockChannel);

        verify(mockChannel).disconnect();
        verify(mockChannel).close();
    }

    @Test
    public void testDestroyObjectWithNullChannel() throws Exception {
        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        factory.destroyObject(key, null);
    }

    @Test
    public void testValidateObjectWithActiveChannel() {
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.isActive()).thenReturn(true);

        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        boolean result = factory.validateObject(key, mockChannel);

        assertTrue(result);
    }

    @Test
    public void testValidateObjectWithInactiveChannel() {
        Channel mockChannel = mock(Channel.class);
        when(mockChannel.isActive()).thenReturn(false);

        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        boolean result = factory.validateObject(key, mockChannel);

        assertFalse(result);
    }

    @Test
    public void testValidateObjectWithNullChannel() {
        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        boolean result = factory.validateObject(key, null);

        assertFalse(result);
    }

    @Test
    public void testActivateObject() throws Exception {
        Channel mockChannel = mock(Channel.class);
        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        factory.activateObject(key, mockChannel);
    }

    @Test
    public void testPassivateObject() throws Exception {
        Channel mockChannel = mock(Channel.class);
        NettyPoolKey key =
                new NettyPoolKey(NettyPoolKey.TransactionRole.RMROLE, "127.0.0.1:8091", new RegisterRMRequest());

        factory.passivateObject(key, mockChannel);
    }
}
