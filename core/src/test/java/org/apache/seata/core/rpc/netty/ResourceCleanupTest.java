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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import io.netty.channel.Channel;
import io.netty.channel.ChannelId;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import org.apache.seata.core.protocol.MergeMessage;
import org.apache.seata.core.protocol.MergedWarpMessage;
import org.apache.seata.core.protocol.MessageFuture;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceCleanupTest {

    private AbstractNettyRemotingClient client;

    @Mock
    private Channel channel;

    @Mock
    private ChannelId channelId;

    private Map<Integer, MessageFuture> futures;
    private Map<Integer, MergeMessage> mergeMsgMap;
    private Map<String, BlockingQueue<RpcMessage>> basketMap;
    private Map<String, Channel> channels;

    @BeforeEach
    void setUp() throws Exception {
        client = TmNettyRemotingClient.getInstance();

        Field futuresField = AbstractNettyRemoting.class.getDeclaredField("futures");
        futuresField.setAccessible(true);
        futures = (Map<Integer, MessageFuture>) futuresField.get(client);

        Field field = AbstractNettyRemotingClient.class.getDeclaredField("mergeMsgMap");
        field.setAccessible(true);
        mergeMsgMap = (Map<Integer, MergeMessage>) field.get(client);

        Field basketMapField = AbstractNettyRemotingClient.class.getDeclaredField("basketMap");
        basketMapField.setAccessible(true);
        basketMap = (Map<String, BlockingQueue<RpcMessage>>) basketMapField.get(client);

        Field channelManagerField = AbstractNettyRemotingClient.class.getDeclaredField("clientChannelManager");
        channelManagerField.setAccessible(true);
        NettyClientChannelManager clientChannelManager = (NettyClientChannelManager) channelManagerField.get(client);

        Field channelsField = clientChannelManager.getClass().getDeclaredField("channels");
        channelsField.setAccessible(true);
        channels = (Map<String, Channel>) channelsField.get(clientChannelManager);
    }

    @Test
    void testCleanupMessageFuturesOnChannelDisconnection() {
        when(channel.id()).thenReturn(channelId);
        when(channel.remoteAddress()).thenReturn(new InetSocketAddress("127.0.0.1", 8091));

        MessageFuture messageFuture1 = new MessageFuture();
        RpcMessage rpcMessage1 = createRpcMessage(1);
        messageFuture1.setRequestMessage(rpcMessage1);
        futures.put(1, messageFuture1);

        MessageFuture messageFuture2 = new MessageFuture();
        RpcMessage rpcMessage2 = createRpcMessage(2);
        messageFuture2.setRequestMessage(rpcMessage2);
        futures.put(2, messageFuture2);

        int parentId = 100;
        MergedWarpMessage mergeMessage = new MergedWarpMessage();
        mergeMessage.msgIds = new ArrayList<>();
        mergeMessage.msgIds.add(1);
        mergeMessage.msgIds.add(2);

        mergeMsgMap.put(parentId, mergeMessage);

        String serverAddress = "127.0.0.1:8091";
        channels.put(serverAddress, channel);

        BlockingQueue<RpcMessage> basket = new LinkedBlockingQueue<>();
        basket.add(rpcMessage1);
        basket.add(rpcMessage2);
        basketMap.put(serverAddress, basket);

        client.cleanupResourcesForChannel(channel);

        assertFalse(futures.containsKey(1), "Future ID 1 has not been removed");
        assertFalse(futures.containsKey(2), "Future ID 2 has not been removed");

        assertThrows(RuntimeException.class, () -> messageFuture1.get(0, java.util.concurrent.TimeUnit.MILLISECONDS));
        assertThrows(RuntimeException.class, () -> messageFuture2.get(0, java.util.concurrent.TimeUnit.MILLISECONDS));
    }

    @Test
    void testCleanupWithNullChannel() {
        MessageFuture messageFuture = new MessageFuture();
        RpcMessage rpcMessage = createRpcMessage(1);
        messageFuture.setRequestMessage(rpcMessage);
        futures.put(1, messageFuture);

        assertDoesNotThrow(() -> client.cleanupResourcesForChannel(null));
        assertTrue(futures.containsKey(1), "Future ID 1 should still exist");
    }

    private RpcMessage createRpcMessage(int id) {
        RpcMessage message = new RpcMessage();
        message.setId(id);
        message.setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        message.setBody("test-body-" + id);
        return message;
    }
}
