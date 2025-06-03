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
import io.netty.channel.ChannelId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelEventListenerTest {

    private AbstractNettyRemotingClient client;

    @Mock
    private Channel channel;

    @Mock
    private ChannelId channelId;

    private TestChannelEventListener testListener;

    @BeforeEach
    void setUp() {
        client = TmNettyRemotingClient.getInstance();
        testListener = new TestChannelEventListener();
        client.registerChannelEventListener(testListener);
    }

    @Test
    void testRegisterAndUnregisterListener() {
        client.onChannelActive(channel);

        assertTrue(testListener.isConnectedCalled());
        assertEquals(channel, testListener.getChannel());

        client.unregisterChannelEventListener(testListener);
        testListener.reset();
        client.onChannelActive(channel);

        assertFalse(testListener.isConnectedCalled());
        assertNotEquals(channel, testListener.getChannel());
        assertNull(testListener.getChannel());
    }

    @Test
    void testChannelConnectedEvent() {
        client.onChannelActive(channel);

        assertTrue(testListener.isConnectedCalled());
        assertEquals(channel, testListener.getChannel());
    }

    @Test
    void testChannelIdleEvent() {
        client.onChannelIdle(channel);

        assertTrue(testListener.isIdleCalled());
        assertEquals(channel, testListener.getChannel());
    }

    @Test
    void testChannelDisconnectedEvent() {
        when(channel.id()).thenReturn(channelId);
        AbstractNettyRemotingClient spyClient = spy(client);
        spyClient.onChannelInactive(channel);

        assertTrue(testListener.isDisconnectedCalled());
        assertEquals(channel, testListener.getChannel());
        verify(spyClient, times(1)).cleanupResourcesForChannel(channel);
    }

    @Test
    void testChannelExceptionEvent() {
        when(channel.id()).thenReturn(channelId);
        AbstractNettyRemotingClient spyClient = spy(client);
        Exception testException = new RuntimeException("Test exception");
        spyClient.onChannelException(channel, testException);

        assertTrue(testListener.isExceptionCalled());
        assertEquals(channel, testListener.getChannel());
        assertEquals(testException, testListener.getLastException());
        verify(spyClient, times(1)).cleanupResourcesForChannel(channel);
    }

    private static class TestChannelEventListener implements ChannelEventListener {
        private boolean connectedCalled = false;
        private boolean disconnectedCalled = false;
        private boolean exceptionCalled = false;
        private boolean idleCalled = false;
        private Throwable lastException = null;
        private Channel channel;

        @Override
        public void onChannelConnected(Channel channel) {
            this.channel = channel;
            this.connectedCalled = true;
        }

        @Override
        public void onChannelDisconnected(Channel channel) {
            this.channel = channel;
            this.disconnectedCalled = true;
        }

        @Override
        public void onChannelException(Channel channel, Throwable cause) {
            this.channel = channel;
            this.exceptionCalled = true;
            this.lastException = cause;
        }

        @Override
        public void onChannelIdle(Channel channel) {
            this.channel = channel;
            this.idleCalled = true;
        }

        public void reset() {
            this.channel = null;
            this.connectedCalled = false;
            this.disconnectedCalled = false;
            this.exceptionCalled = false;
            this.idleCalled = false;
            this.lastException = null;
        }

        public boolean isConnectedCalled() {
            return connectedCalled;
        }

        public boolean isDisconnectedCalled() {
            return disconnectedCalled;
        }

        public boolean isExceptionCalled() {
            return exceptionCalled;
        }

        public boolean isIdleCalled() {
            return idleCalled;
        }

        public Throwable getLastException() {
            return lastException;
        }

        public Channel getChannel() {
            return channel;
        }
    }
}
