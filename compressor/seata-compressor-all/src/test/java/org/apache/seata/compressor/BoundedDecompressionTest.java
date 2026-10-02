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
package org.apache.seata.compressor;

import com.google.protobuf.ByteString;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http2.DefaultHttp2DataFrame;
import org.apache.seata.core.compressor.Compressor;
import org.apache.seata.core.compressor.CompressorFactory;
import org.apache.seata.core.compressor.CompressorType;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.protocol.generated.GrpcMessageProto;
import org.apache.seata.core.rpc.netty.MultiProtocolDecoder;
import org.apache.seata.core.rpc.netty.grpc.GrpcDecoder;
import org.apache.seata.core.rpc.netty.v1.ProtocolDecoderV1;
import org.apache.seata.core.rpc.netty.v2.ProtocolDecoderV2;
import org.apache.seata.core.serializer.SerializerType;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.*;

class BoundedDecompressionTest {

    @ParameterizedTest
    @EnumSource(value = CompressorType.class, names = "SEVENZ", mode = EnumSource.Mode.EXCLUDE)
    void acceptsOutputWithinLimit(CompressorType type) {
        Compressor compressor = CompressorFactory.getCompressor(type.getCode());
        for (int size : new int[] {0, 1, 8191, 8192, 8193, 32768}) {
            byte[] data = new byte[size];
            byte[] compressed = compressor.compress(data);
            assertArrayEquals(data, compressor.decompress(compressed, size));
            assertArrayEquals(data, compressor.decompress(compressed, size + 1));
            if (size > 0) {
                assertThrows(IllegalArgumentException.class, () -> compressor.decompress(compressed, size - 1));
            }
        }
        assertThrows(IllegalArgumentException.class, () -> compressor.decompress(compressor.compress(new byte[0]), -1));
    }

    @ParameterizedTest
    @EnumSource(
            value = CompressorType.class,
            names = {"NONE", "SEVENZ"},
            mode = EnumSource.Mode.EXCLUDE)
    void rejectsLargeOutputInRpcDecoders(CompressorType type) {
        byte[] data = new byte[ProtocolConstants.MAX_FRAME_LENGTH + 1];
        Compressor compressor = CompressorFactory.getCompressor(type.getCode());
        byte[] compressed = compressor.compress(data);
        assertTrue(compressed.length + ProtocolConstants.V1_HEAD_LENGTH < ProtocolConstants.MAX_FRAME_LENGTH);
        // Storage callers retain the original API behavior.
        assertArrayEquals(data, compressor.decompress(compressed));
        for (ProtocolDecoderV1 decoder : new ProtocolDecoderV1[] {new ProtocolDecoderV1(), new ProtocolDecoderV2()}) {
            ByteBuf frame = Unpooled.buffer();
            frame.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);
            frame.writeByte(decoder.protocolVersion());
            frame.writeInt(ProtocolConstants.V1_HEAD_LENGTH + compressed.length);
            frame.writeShort(ProtocolConstants.V1_HEAD_LENGTH);
            frame.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);
            frame.writeByte(SerializerType.SEATA.getCode());
            frame.writeByte(type.getCode());
            frame.writeInt(1);
            frame.writeBytes(compressed);
            EmbeddedChannel channel = new EmbeddedChannel(new MultiProtocolDecoder());
            try {
                RuntimeException error = assertThrows(RuntimeException.class, () -> channel.writeInbound(frame));
                assertLimitFailure(error);
                assertNull(channel.readInbound());
                assertEquals(0, frame.refCnt());
            } finally {
                channel.finishAndReleaseAll();
            }
        }
        GrpcMessageProto message = GrpcMessageProto.newBuilder()
                .setMessageType(ProtocolConstants.MSGTYPE_RESQUEST_SYNC)
                .putHeadMap("compress-type", Byte.toString(type.getCode()))
                .setBody(ByteString.copyFrom(compressed))
                .build();
        byte[] encoded = message.toByteArray();
        ByteBuf content =
                Unpooled.buffer().writeByte(0).writeInt(encoded.length).writeBytes(encoded);
        EmbeddedChannel channel = new EmbeddedChannel(new GrpcDecoder());
        try {
            RuntimeException error = assertThrows(
                    RuntimeException.class, () -> channel.writeInbound(new DefaultHttp2DataFrame(content)));
            assertLimitFailure(error);
            assertNull(channel.readInbound());
            assertEquals(0, content.refCnt());
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private void assertLimitFailure(Throwable error) {
        while (error.getCause() != null) {
            error = error.getCause();
        }
        assertInstanceOf(IllegalArgumentException.class, error);
        assertTrue(error.getMessage().startsWith("Decompressed data exceeds maximum size:"));
    }
}
