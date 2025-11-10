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
package org.apache.seata.core.rpc;

import io.netty.channel.Channel;
import org.apache.seata.core.protocol.MessageType;
import org.apache.seata.core.protocol.MessageTypeAware;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.protocol.Version;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MsgVersionHelperTest {

    @Test
    public void testVersionNotSupportWithNullRpcMessage() {
        Channel channel = mock(Channel.class);
        boolean result = MsgVersionHelper.versionNotSupport(channel, null);
        Assertions.assertFalse(result);
    }

    @Test
    public void testVersionNotSupportWithNullBody() {
        Channel channel = mock(Channel.class);
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(null);
        boolean result = MsgVersionHelper.versionNotSupport(channel, rpcMessage);
        Assertions.assertFalse(result);
    }

    @Test
    public void testVersionNotSupportWithNullChannel() {
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(new Object());
        boolean result = MsgVersionHelper.versionNotSupport(null, rpcMessage);
        Assertions.assertFalse(result);
    }

    @Test
    public void testVersionNotSupportWithBlankVersion() {
        Channel channel = mock(Channel.class);
        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(new Object());

        boolean result = MsgVersionHelper.versionNotSupport(channel, rpcMessage);
        Assertions.assertFalse(result);
    }

    @Test
    public void testVersionNotSupportWithV1Version() {
        Channel channel = mock(Channel.class);
        MessageTypeAware msg = mock(MessageTypeAware.class);
        when(msg.getTypeCode()).thenReturn(MessageType.TYPE_RM_DELETE_UNDOLOG);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(msg);

        try (MockedStatic<Version> mockedVersion = Mockito.mockStatic(Version.class)) {
            mockedVersion.when(() -> Version.getChannelVersion(channel)).thenReturn("1.5.0");
            mockedVersion.when(() -> Version.isV0("1.5.0")).thenReturn(false);

            boolean result = MsgVersionHelper.versionNotSupport(channel, rpcMessage);
            Assertions.assertFalse(result);
        }
    }

    @Test
    public void testVersionNotSupportWithV0VersionAndUnsupportedMsg() {
        Channel channel = mock(Channel.class);
        MessageTypeAware msg = mock(MessageTypeAware.class);
        when(msg.getTypeCode()).thenReturn(MessageType.TYPE_RM_DELETE_UNDOLOG);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(msg);

        try (MockedStatic<Version> mockedVersion = Mockito.mockStatic(Version.class)) {
            mockedVersion.when(() -> Version.getChannelVersion(channel)).thenReturn("0.7.0");
            mockedVersion.when(() -> Version.isV0("0.7.0")).thenReturn(true);

            boolean result = MsgVersionHelper.versionNotSupport(channel, rpcMessage);
            Assertions.assertTrue(result);
        }
    }

    @Test
    public void testVersionNotSupportWithV0VersionAndSupportedMsg() {
        Channel channel = mock(Channel.class);
        MessageTypeAware msg = mock(MessageTypeAware.class);
        when(msg.getTypeCode()).thenReturn(MessageType.TYPE_HEARTBEAT_MSG);

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(msg);

        try (MockedStatic<Version> mockedVersion = Mockito.mockStatic(Version.class)) {
            mockedVersion.when(() -> Version.getChannelVersion(channel)).thenReturn("0.7.0");
            mockedVersion.when(() -> Version.isV0("0.7.0")).thenReturn(true);

            boolean result = MsgVersionHelper.versionNotSupport(channel, rpcMessage);
            Assertions.assertFalse(result);
        }
    }

    @Test
    public void testVersionNotSupportWithNonMessageTypeAware() {
        Channel channel = mock(Channel.class);
        Object msg = new Object();

        RpcMessage rpcMessage = new RpcMessage();
        rpcMessage.setBody(msg);

        try (MockedStatic<Version> mockedVersion = Mockito.mockStatic(Version.class)) {
            mockedVersion.when(() -> Version.getChannelVersion(channel)).thenReturn("0.7.0");
            mockedVersion.when(() -> Version.isV0("0.7.0")).thenReturn(true);

            boolean result = MsgVersionHelper.versionNotSupport(channel, rpcMessage);
            Assertions.assertFalse(result);
        }
    }
}
