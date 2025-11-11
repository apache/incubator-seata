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
package org.apache.seata.core.rpc.netty.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.serializer.SerializerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProtocolDecoderV1Test {

    private ProtocolDecoderV1 decoder;

    @Mock
    private ChannelHandlerContext ctx;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        decoder = new ProtocolDecoderV1();
    }

    @Test
    public void testConstructor() {
        assertNotNull(decoder);
    }

    @Test
    public void testDecodeHeartbeatRequest() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length (only head, no body)
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12345);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12345, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, rpcMessage.getMessageType());
        assertEquals(HeartbeatMessage.PING, rpcMessage.getBody());
        assertEquals(SerializerType.SEATA.getCode(), rpcMessage.getCodec());
        assertEquals(CompressorType.NONE.getCode(), rpcMessage.getCompressor());

        byteBuf.release();
    }

    @Test
    public void testDecodeHeartbeatResponse() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length (only head, no body)
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12346);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12346, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_RESPONSE, rpcMessage.getMessageType());
        assertEquals(HeartbeatMessage.PONG, rpcMessage.getBody());

        byteBuf.release();
    }

    @Test
    public void testDecodeWithHeadMap() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Prepare head map
        Map<String, String> headMap = new HashMap<>();
        headMap.put("key1", "value1");
        headMap.put("key2", "value2");

        // Encode head map
        ByteBuf headMapBuf = Unpooled.buffer();
        int headMapLength = HeadMapSerializer.getInstance().encode(headMap, headMapBuf);

        int fullLength = ProtocolConstants.V1_HEAD_LENGTH + headMapLength;
        int headLength = ProtocolConstants.V1_HEAD_LENGTH + headMapLength;

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length
        byteBuf.writeInt(fullLength);
        // Write head length
        byteBuf.writeShort(headLength);
        // Write message type (heartbeat to avoid body serialization)
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12347);
        // Write head map
        byteBuf.writeBytes(headMapBuf);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12347, rpcMessage.getId());
        assertNotNull(rpcMessage.getHeadMap());
        assertEquals(2, rpcMessage.getHeadMap().size());
        assertEquals("value1", rpcMessage.getHeadMap().get("key1"));
        assertEquals("value2", rpcMessage.getHeadMap().get("key2"));

        byteBuf.release();
        headMapBuf.release();
    }

    @Test
    public void testDecodeMessageId() {
        ByteBuf byteBuf = Unpooled.buffer();

        long messageId = 999999;

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt((int) messageId);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(messageId, rpcMessage.getId());

        byteBuf.release();
    }

    @Test
    public void testDecodeInvalidMagicCode() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write invalid magic code
        byteBuf.writeByte(0x00);
        byteBuf.writeByte(0x00);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12348);

        assertThrows(IllegalArgumentException.class, () -> {
            decoder.decodeFrame(byteBuf);
        });

        byteBuf.release();
    }

    @Test
    public void testDecodeWithChannelHandlerContext() throws Exception {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12349);

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

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length indicating body exists
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH + 100);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type (not heartbeat, so body is expected)
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12350);
        // Write incorrect body data
        byteBuf.writeBytes(new byte[100]);

        // The decode should fail due to deserialization error
        assertThrows(Exception.class, () -> {
            decoder.decode(ctx, byteBuf);
        });

        byteBuf.release();
    }

    @Test
    public void testDecodeWithEmptyHeadMap() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length (only head, no body, no head map)
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length (no head map)
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12351);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertNotNull(rpcMessage.getHeadMap());
        assertTrue(rpcMessage.getHeadMap().isEmpty());

        byteBuf.release();
    }

    @Test
    public void testDecodeNullReturn() throws Exception {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write partial data (not enough for a complete frame)
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);

        Object result = decoder.decode(ctx, byteBuf);

        // Should return null when frame is not complete
        assertNull(result);

        byteBuf.release();
    }

    @Test
    public void testDecodeInsufficientData() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write only partial header
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Missing message type and other fields

        assertThrows(IndexOutOfBoundsException.class, () -> {
            decoder.decodeFrame(byteBuf);
        });

        byteBuf.release();
    }

    @Test
    public void testDecodeRequestSyncMessage() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length (only head, no body for simplicity)
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12352);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12352, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_RESQUEST_SYNC, rpcMessage.getMessageType());

        byteBuf.release();
    }

    @Test
    public void testDecodeResponseMessage() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length (only head, no body for simplicity)
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_RESPONSE);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12353);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12353, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_RESPONSE, rpcMessage.getMessageType());

        byteBuf.release();
    }

    @Test
    public void testDecodeOnewayMessage() {
        ByteBuf byteBuf = Unpooled.buffer();

        // Write magic code
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        // Write version
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        // Write full length (only head, no body for simplicity)
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        // Write head length
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        // Write message type
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY);
        // Write codec type
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        // Write compressor type
        byteBuf.writeByte(CompressorType.NONE.getCode());
        // Write request id
        byteBuf.writeInt(12354);

        RpcMessage rpcMessage = decoder.decodeFrame(byteBuf);

        assertNotNull(rpcMessage);
        assertEquals(12354, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_RESQUEST_ONEWAY, rpcMessage.getMessageType());

        byteBuf.release();
    }
}
