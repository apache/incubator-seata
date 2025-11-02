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

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.exception.DecodeException;
import org.apache.seata.core.protocol.HeartbeatMessage;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.RpcMessage;
import org.apache.seata.core.rpc.netty.v0.ProtocolDecoderV0;
import org.apache.seata.core.rpc.netty.v0.ProtocolEncoderV0;
import org.apache.seata.core.rpc.netty.v1.ProtocolDecoderV1;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
import org.apache.seata.core.serializer.SerializerType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class MultiProtocolDecoderTest {

    private MultiProtocolDecoder decoder;

    @Mock
    private ChannelHandlerContext ctx;

    @Mock
    private ChannelPipeline pipeline;

    @Mock
    private ChannelHandler customHandler;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(pipeline.addLast(any(ChannelHandler.class))).thenReturn(pipeline);
        when(pipeline.remove(any(ChannelHandler.class))).thenReturn(pipeline);
    }

    @Test
    public void testDefaultConstructor() {
        decoder = new MultiProtocolDecoder();
        assertNotNull(decoder);
    }

    @Test
    public void testConstructorWithChannelHandlers() {
        decoder = new MultiProtocolDecoder(customHandler);
        assertNotNull(decoder);
    }

    @Test
    public void testConstructorWithMaxFrameLength() {
        int maxFrameLength = 16 * 1024 * 1024;
        decoder = new MultiProtocolDecoder(maxFrameLength, new ChannelHandler[] {customHandler});
        assertNotNull(decoder);
    }

    @Test
    public void testIsV0WithV0Protocol() {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V0 protocol header (magic code + version 0)
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte((byte) 0); // V0 flag first byte

        boolean isV0 = decoder.isV0(byteBuf);

        assertTrue(isV0);
        assertEquals(0, byteBuf.readerIndex()); // verify reader index was reset

        byteBuf.release();
    }

    @Test
    public void testIsV0WithV1Protocol() {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V1 protocol header
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);

        boolean isV0 = decoder.isV0(byteBuf);

        assertFalse(isV0);
        assertEquals(0, byteBuf.readerIndex()); // verify reader index was reset

        byteBuf.release();
    }

    @Test
    public void testIsV0WithInvalidMagicCode() {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write invalid magic code
        byteBuf.writeByte((byte) 0x00);
        byteBuf.writeByte((byte) 0x00);
        byteBuf.writeByte((byte) 0);

        boolean isV0 = decoder.isV0(byteBuf);

        assertFalse(isV0);
        assertEquals(0, byteBuf.readerIndex()); // verify reader index was reset

        byteBuf.release();
    }

    @Test
    public void testIsV0WithByte() {
        decoder = new MultiProtocolDecoder();

        assertTrue(decoder.isV0(ProtocolConstants.VERSION_0));
        assertFalse(decoder.isV0(ProtocolConstants.VERSION_1));
        assertFalse(decoder.isV0((byte) 2));
    }

    @Test
    public void testDecideVersionWithV1() {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V1 protocol header
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);

        byte version = decoder.decideVersion(byteBuf);

        assertEquals(ProtocolConstants.VERSION_1, version);
        assertEquals(0, byteBuf.readerIndex()); // verify reader index was reset

        byteBuf.release();
    }

    @Test
    public void testDecideVersionWithInvalidMagicCode() {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write invalid magic code
        byteBuf.writeByte((byte) 0x00);
        byteBuf.writeByte((byte) 0x00);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);

        assertThrows(IllegalArgumentException.class, () -> {
            decoder.decideVersion(byteBuf);
        });

        byteBuf.release();
    }

    @Test
    public void testDecideVersionWithNonByteBufInput() {
        decoder = new MultiProtocolDecoder();
        String nonByteBuf = "not a bytebuf";

        byte version = decoder.decideVersion(nonByteBuf);

        assertEquals(-1, version);
    }

    @Test
    public void testDecodeV1HeartbeatMessage() throws Exception {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V1 heartbeat message
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        byteBuf.writeByte(CompressorType.NONE.getCode());
        byteBuf.writeInt(12345);

        Object result = decoder.decode(ctx, byteBuf);

        assertNotNull(result);
        assertTrue(result instanceof RpcMessage);
        RpcMessage rpcMessage = (RpcMessage) result;
        assertEquals(12345, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST, rpcMessage.getMessageType());
        assertEquals(HeartbeatMessage.PING, rpcMessage.getBody());

        // Verify pipeline modifications
        ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(pipeline, atLeast(2)).addLast(handlerCaptor.capture());
        verify(pipeline).remove(decoder);

        // Verify correct decoder and encoder were added
        assertTrue(handlerCaptor.getAllValues().stream().anyMatch(h -> h instanceof ProtocolDecoderV1));
        assertTrue(handlerCaptor.getAllValues().stream().anyMatch(h -> h instanceof ProtocolEncoderV1));

        byteBuf.release();
    }

    @Test
    public void testDecodeV0Message() throws Exception {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V0 protocol header - complete V0 heartbeat message
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte((byte) 0); // V0 version flag (first byte of flag field)
        byteBuf.writeByte((byte) 0x20); // Second byte of flag - heartbeat flag
        byteBuf.writeShort((short) 0); // body length/type code
        byteBuf.writeLong(12345L); // message id
        // Total: 2 (magic) + 2 (flag) + 2 (body length) + 8 (id) = 14 bytes (V0 HEAD_LENGTH)

        Object result = decoder.decode(ctx, byteBuf);

        assertNotNull(result);
        assertTrue(result instanceof RpcMessage);

        // Verify pipeline modifications for V0
        ArgumentCaptor<ChannelHandler> handlerCaptor = ArgumentCaptor.forClass(ChannelHandler.class);
        verify(pipeline, atLeast(2)).addLast(handlerCaptor.capture());
        verify(pipeline).remove(decoder);

        // Verify correct V0 decoder and encoder were added
        assertTrue(handlerCaptor.getAllValues().stream().anyMatch(h -> h instanceof ProtocolDecoderV0));
        assertTrue(handlerCaptor.getAllValues().stream().anyMatch(h -> h instanceof ProtocolEncoderV0));

        byteBuf.release();
    }

    @Test
    public void testDecodeWithCustomChannelHandlers() throws Exception {
        decoder = new MultiProtocolDecoder(customHandler);
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V1 heartbeat message
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        byteBuf.writeByte(CompressorType.NONE.getCode());
        byteBuf.writeInt(12346);

        Object result = decoder.decode(ctx, byteBuf);

        assertNotNull(result);

        // Verify custom handler was added to pipeline
        verify(pipeline, times(1)).addLast(new ChannelHandler[] {customHandler});

        byteBuf.release();
    }

    @Test
    public void testDecodeWithNullInput() throws Exception {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write incomplete data (not enough for a complete frame)
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(100); // Full length

        Object result = decoder.decode(ctx, byteBuf);

        // Should return null for incomplete frame (parent LengthFieldBasedFrameDecoder returns null)
        assertNull(result);

        byteBuf.release();
    }

    @Test
    public void testDecodeWithUnsupportedVersion() throws Exception {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write header with unsupported version (e.g., version 99)
        // Note: When decoder for version 99 is not found, it falls back to current version (V1)
        // So this test verifies that fallback works correctly
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte((byte) 99); // Unsupported version
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        byteBuf.writeByte(CompressorType.NONE.getCode());
        byteBuf.writeInt(12347);

        // Should not throw exception because it falls back to current version
        Object result = decoder.decode(ctx, byteBuf);

        assertNotNull(result);
        assertTrue(result instanceof RpcMessage);

        byteBuf.release();
    }

    @Test
    public void testDecodeExceptionHandling() throws Exception {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write invalid data that will cause decode error
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(100); // Full length
        byteBuf.writeShort(16); // Head length
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        byteBuf.writeByte(CompressorType.NONE.getCode());
        byteBuf.writeInt(12348);
        // Write invalid body data
        byteBuf.writeBytes(new byte[84]);

        assertThrows(DecodeException.class, () -> {
            decoder.decode(ctx, byteBuf);
        });

        byteBuf.release();
    }

    @Test
    public void testDecodeV1ResponseMessage() throws Exception {
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf = Unpooled.buffer();

        // Write V1 response message
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_RESPONSE);
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        byteBuf.writeByte(CompressorType.NONE.getCode());
        byteBuf.writeInt(54321);

        Object result = decoder.decode(ctx, byteBuf);

        assertNotNull(result);
        assertTrue(result instanceof RpcMessage);
        RpcMessage rpcMessage = (RpcMessage) result;
        assertEquals(54321, rpcMessage.getId());
        assertEquals(ProtocolConstants.MSGTYPE_RESPONSE, rpcMessage.getMessageType());

        byteBuf.release();
    }

    @Test
    public void testDecodeMultipleMessagesSequentially() throws Exception {
        // Test decoding multiple messages to ensure decoder state is properly managed

        // First message - V1
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf1 = Unpooled.buffer();
        byteBuf1.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf1.writeByte(ProtocolConstants.VERSION_1);
        byteBuf1.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf1.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf1.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        byteBuf1.writeByte(SerializerType.SEATA.getCode());
        byteBuf1.writeByte(CompressorType.NONE.getCode());
        byteBuf1.writeInt(111);

        // Reset mocks
        reset(ctx, pipeline);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(pipeline.addLast(any(ChannelHandler.class))).thenReturn(pipeline);
        when(pipeline.remove(any(ChannelHandler.class))).thenReturn(pipeline);

        Object result1 = decoder.decode(ctx, byteBuf1);
        assertNotNull(result1);
        assertTrue(result1 instanceof RpcMessage);
        assertEquals(111, ((RpcMessage) result1).getId());

        byteBuf1.release();

        // Second message - V0 (complete V0 heartbeat message)
        decoder = new MultiProtocolDecoder();
        ByteBuf byteBuf2 = Unpooled.buffer();
        byteBuf2.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf2.writeByte((byte) 0); // V0 version flag (first byte of flag field)
        byteBuf2.writeByte((byte) 0x20); // Second byte of flag - heartbeat flag
        byteBuf2.writeShort((short) 0); // body length/type code
        byteBuf2.writeLong(222L); // message id

        // Reset mocks again
        reset(ctx, pipeline);
        when(ctx.pipeline()).thenReturn(pipeline);
        when(pipeline.addLast(any(ChannelHandler.class))).thenReturn(pipeline);
        when(pipeline.remove(any(ChannelHandler.class))).thenReturn(pipeline);

        Object result2 = decoder.decode(ctx, byteBuf2);
        assertNotNull(result2);
        assertTrue(result2 instanceof RpcMessage);
        assertEquals(222L, ((RpcMessage) result2).getId());

        byteBuf2.release();
    }

    @Test
    public void testDecodeWithEmbeddedChannel() {
        // Test with real channel instead of mocks
        EmbeddedChannel channel = new EmbeddedChannel(new MultiProtocolDecoder());

        ByteBuf byteBuf = Unpooled.buffer();
        byteBuf.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
        byteBuf.writeByte(ProtocolConstants.VERSION_1);
        byteBuf.writeInt(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
        byteBuf.writeByte(ProtocolConstants.MSGTYPE_HEARTBEAT_REQUEST);
        byteBuf.writeByte(SerializerType.SEATA.getCode());
        byteBuf.writeByte(CompressorType.NONE.getCode());
        byteBuf.writeInt(99999);

        channel.writeInbound(byteBuf);

        Object result = channel.readInbound();
        assertNotNull(result);
        assertTrue(result instanceof RpcMessage);
        RpcMessage rpcMessage = (RpcMessage) result;
        assertEquals(99999, rpcMessage.getId());

        // Verify pipeline was modified - MultiProtocolDecoder should be removed
        assertNull(channel.pipeline().get(MultiProtocolDecoder.class));
        // Verify appropriate decoder and encoder were added
        assertNotNull(channel.pipeline().get(ProtocolDecoderV1.class));
        assertNotNull(channel.pipeline().get(ProtocolEncoderV1.class));

        channel.finish();
    }
}
