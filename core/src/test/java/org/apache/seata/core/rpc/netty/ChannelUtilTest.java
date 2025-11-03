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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChannelUtilTest {

    @Test
    public void testGetAddressFromChannel() {
        Channel channel = mock(Channel.class);
        InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        String result = ChannelUtil.getAddressFromChannel(channel);
        Assertions.assertEquals("192.168.1.100:8080", result);
    }

    @Test
    public void testGetAddressFromChannelWithLeadingSlash() {
        Channel channel = mock(Channel.class);
        InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        String result = ChannelUtil.getAddressFromChannel(channel);
        Assertions.assertFalse(result.startsWith("/"));
    }

    @Test
    public void testGetClientIpFromChannel() {
        Channel channel = mock(Channel.class);
        InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        String result = ChannelUtil.getClientIpFromChannel(channel);
        Assertions.assertEquals("192.168.1.100", result);
    }

    @Test
    public void testGetClientIpFromChannelWithoutPort() {
        Channel channel = mock(Channel.class);
        InetSocketAddress address = new InetSocketAddress("localhost", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        String result = ChannelUtil.getClientIpFromChannel(channel);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testGetClientPortFromChannel() {
        Channel channel = mock(Channel.class);
        InetSocketAddress address = new InetSocketAddress("192.168.1.100", 8080);
        when(channel.remoteAddress()).thenReturn(address);

        Integer result = ChannelUtil.getClientPortFromChannel(channel);
        Assertions.assertEquals(8080, result);
    }

    @Test
    public void testGetClientPortFromChannelWithInvalidPort() {
        Channel channel = mock(Channel.class);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.100", 0));

        Integer result = ChannelUtil.getClientPortFromChannel(channel);
        Assertions.assertNotNull(result);
    }

    @Test
    public void testGetClientIpAndPortFromChannel() {
        Channel channel = mock(Channel.class);
        InetSocketAddress address = new InetSocketAddress("10.0.0.1", 9999);
        when(channel.remoteAddress()).thenReturn(address);

        String ip = ChannelUtil.getClientIpFromChannel(channel);
        Integer port = ChannelUtil.getClientPortFromChannel(channel);

        Assertions.assertEquals("10.0.0.1", ip);
        Assertions.assertEquals(9999, port);
    }

    @Test
    public void testGetClientPortFromChannelWithDifferentPorts() {
        Channel channel1 = mock(Channel.class);
        when(channel1.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.1", 8080));

        Channel channel2 = mock(Channel.class);
        when(channel2.remoteAddress()).thenReturn(new InetSocketAddress("192.168.1.2", 9090));

        Integer port1 = ChannelUtil.getClientPortFromChannel(channel1);
        Integer port2 = ChannelUtil.getClientPortFromChannel(channel2);

        Assertions.assertEquals(8080, port1);
        Assertions.assertEquals(9090, port2);
    }
}
