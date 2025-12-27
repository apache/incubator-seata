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
package org.apache.seata.core.rpc.netty.v0;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.exception.DecodeException;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolDecoderV0Test {

    private ProtocolDecoderV0 decoder;

    @Mock
    private ChannelHandlerContext ctx;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        decoder = new ProtocolDecoderV0();
    }

    @Test
    public void testConstructor() {
        assertNotNull(decoder);
    }

    @Test
    public void testDecodeHeartbeatRequest() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write header
        byteBuf.writeShort(ProtocolConstantsV0.MAGIC);
        int flag = ProtocolConstantsV0.FLAG_HEARTBEAT | ProtocolConstantsV0.FLAG_REQUEST;
        byteBuf.writeShort((short) flag);
        byteBuf.writeShort((short) 0); // body length
        byteBuf.writeLong(12345L); // message id

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12345, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, rpcMessage.getMessageType());
        assertEquals(HeartbeatMessage.PING, rpcMessage.getBody());

        byteBuf.release();
    }

    @Test
    public void testDecodeHeartbeatResponse() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write header - heartbeat but not request (so it's a response)
        byteBuf.writeShort(ProtocolConstantsV0.MAGIC);
        int flag = ProtocolConstantsV0.FLAG_HEARTBEAT;
        byteBuf.writeShort((short) flag);
        byteBuf.writeShort((short) 0); // body length
        byteBuf.writeLong(12346L); // message id

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12346, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE, rpcMessage.getMessageType());
        assertEquals(HeartbeatMessage.PONG, rpcMessage.getBody());

        byteBuf.release();
    }

    @Test
    public void testDecodeMessageId() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write heartbeat with specific message ID to test ID parsing
        byteBuf.writeShort(ProtocolConstantsV0.MAGIC);
        int flag = ProtocolConstantsV0.FLAG_HEARTBEAT | ProtocolConstantsV0.FLAG_REQUEST;
        byteBuf.writeShort((short) flag);
        byteBuf.writeShort((short) 0); // body length
        long messageId = 999999L;
        byteBuf.writeLong(messageId);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(messageId, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, rpcMessage.getMessageType());
        assertEquals(HeartbeatMessage.PING, rpcMessage.getBody());

        byteBuf.release();
    }

    @Test
    public void testDecodeInsufficientData() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write only partial header (less than HEAD_LENGTH)
        byteBuf.writeShort(ProtocolConstantsV0.MAGIC);
        byteBuf.writeShort((short) 0);
        // Missing the rest of the header

        assertThrows(IllegalArgumentException.class, () -> {
            decoder.decodeFrame(byteBuf);
        });

        byteBuf.release();
    }

    @Test
    public void testDecodeWithChannelHandlerContext() throws Exception {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write heartbeat request
        byteBuf.writeShort(ProtocolConstantsV0.MAGIC);
        int flag = ProtocolConstantsV0.FLAG_HEARTBEAT | ProtocolConstantsV0.FLAG_REQUEST;
        byteBuf.writeShort((short) flag);
        byteBuf.writeShort((short) 0);
        byteBuf.writeLong(12350L);

        Object result = decoder.decode(ctx, byteBuf);

        assertNotNull(result);
        assertTrue(result instanceof RpcMessage);
        RpcMessage rpcMessage = (RpcMessage) result;
        assertEquals(HeartbeatMessage.PING, rpcMessage.getBody());

        byteBuf.release();
    }

    @Test
    public void testDecodeErrorHandling() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write invalid data that will cause decode error
        byteBuf.writeShort(ProtocolConstantsV0.MAGIC);
        byteBuf.writeShort((short) ProtocolConstantsV0.FLAG_REQUEST);
        byteBuf.writeShort((short) 100); // body length = 100
        byteBuf.writeLong(12351L);
        // But write less data than body length indicates
        byteBuf.writeBytes(new byte[10]);

        assertThrows(DecodeException.class, () -> {
            decoder.decode(ctx, byteBuf);
        });

        byteBuf.release();
    }
}
