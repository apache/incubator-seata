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
import io.netty.channel.ChannelPipeline;
import io.netty.channel.embedded.EmbeddedChannel;
import org.apache.seata.core.protocol.ProtocolConstants;
import org.apache.seata.core.rpc.netty.v1.ProtocolDecoderV1;
import org.apache.seata.core.rpc.netty.v1.ProtocolEncoderV1;
import org.apache.seata.core.rpc.netty.v2.ProtocolEncoderV2;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for MultiProtocolDecoder encoder replacement functionality
 */
public class MultiProtocolDecoderTest {

    /**
     * Test that MultiProtocolDecoder correctly removes existing encoder when processing messages
     */
    @Test
    public void testEncoderReplacement() throws Exception {
        // Create an embedded channel with V2 encoder and MultiProtocolDecoder
        EmbeddedChannel channel = new EmbeddedChannel();

        // Add V2 encoder first (simulating client-side setup)
        channel.pipeline().addLast("encoder", new ProtocolEncoderV2());

        // Add MultiProtocolDecoder
        channel.pipeline().addLast("multiDecoder", createMultiProtocolDecoder(ProtocolConstants.VERSION_1));

        // Create a V1 message (magic + version + length + data)
        ByteBuf testMessage = createV1TestMessage();

        // Process the message through MultiProtocolDecoder
        channel.writeInbound(testMessage);

        // Verify that the original V2 encoder was removed and V1 encoder was added
        ChannelPipeline pipeline = channel.pipeline();

        // The MultiProtocolDecoder should have been removed after processing
        assertNull(pipeline.get("multiDecoder"), "MultiProtocolDecoder should be removed after processing");

        // Should not have the old V2 encoder anymore
        assertNull(pipeline.get("encoder"), "Original V2 encoder should be removed");

        // Should have V1 encoder and decoder
        assertNotNull(pipeline.get(ProtocolDecoderV1.class), "Should have V1 decoder");
        assertNotNull(pipeline.get(ProtocolEncoderV1.class), "Should have V1 encoder");

        channel.close();
    }

    /**
     * Test that no encoder duplication occurs
     */
    @Test
    public void testNoEncoderDuplication() throws Exception {
        EmbeddedChannel channel = new EmbeddedChannel();

        // Add V1 encoder first
        channel.pipeline().addLast("v1encoder", new ProtocolEncoderV1());

        // Add MultiProtocolDecoder
        channel.pipeline().addLast("multiDecoder", new MultiProtocolDecoder());

        // Create a V1 message
        ByteBuf testMessage = createV1TestMessage();

        // Process the message
        channel.writeInbound(testMessage);

        // Count encoders in the pipeline
        long encoderCount = channel.pipeline().toMap().values().stream()
                .filter(handler -> handler instanceof ProtocolEncoder && !(handler instanceof ProtocolDecoder))
                .count();

        // Should have exactly one encoder
        assertEquals(1, encoderCount, "Should have exactly one encoder after processing");

        channel.close();
    }

    /**
     * Create a test V1 message
     */
    private ByteBuf createV1TestMessage() {
        ByteBuf buffer = Unpooled.buffer();

        // Magic code (0xdada)
        buffer.writeBytes(ProtocolConstants.MAGIC_CODE_BYTES);

        // Version (V1)
        buffer.writeByte(ProtocolConstants.VERSION_1);

        // Full length (4 bytes) - we'll update this
        int lengthIndex = buffer.writerIndex();
        buffer.writeInt(0); // placeholder

        // Head length (2 bytes)
        buffer.writeShort(16);

        // Message type
        buffer.writeByte(ProtocolConstants.MSGTYPE_RESQUEST_SYNC);

        // Serializer
        buffer.writeByte(1);

        // Compressor
        buffer.writeByte(0);

        // Request ID (4 bytes)
        buffer.writeInt(1);

        // Update full length
        int fullLength = buffer.readableBytes();
        buffer.setInt(lengthIndex, fullLength);

        return buffer;
    }

    public MultiProtocolDecoder createMultiProtocolDecoder(byte currentVersion) {
        return new MultiProtocolDecoder(currentVersion, new TestServerHandler());
    }

    public MultiProtocolDecoder createMultiProtocolDecoder(byte currentVersion, ChannelHandler... handlers) {
        return new MultiProtocolDecoder(currentVersion, handlers);
    }
}
