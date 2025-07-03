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
package org.apache.seata.core.protocol.detector;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class Http2DetectorTest {
    @Test
    void testDetectWithHttp2Prefix() {
        byte[] http2Prefix = "PRI * HTTP/2.0\r\n\r\nSM\r\n\r\n".getBytes(StandardCharsets.UTF_8);
        ByteBuf buf = Unpooled.copiedBuffer(http2Prefix);
        Http2Detector detector = new Http2Detector(new ChannelHandler[] {});
        assertTrue(detector.detect(buf));
        buf.release();
    }

    @Test
    void testDetectWithNonHttp2() {
        ByteBuf buf = Unpooled.copiedBuffer("NOTHTTP2", StandardCharsets.UTF_8);
        Http2Detector detector = new Http2Detector(new ChannelHandler[] {});
        assertFalse(detector.detect(buf));
        buf.release();
    }

    @Test
    void testDetectWithShortBuffer() {
        ByteBuf buf = Unpooled.copiedBuffer("PRI * HTTP/2.0", StandardCharsets.UTF_8);
        Http2Detector detector = new Http2Detector(new ChannelHandler[] {});
        assertFalse(detector.detect(buf));
        buf.release();
    }

    @Test
    void testGetHandlersNotNull() {
        ChannelHandler mockHandler = mock(ChannelHandler.class);
        Http2Detector detector = new Http2Detector(new ChannelHandler[] {mockHandler});
        ChannelHandler[] handlers = detector.getHandlers();
        assertNotNull(handlers);
        assertEquals(2, handlers.length);
        assertNotNull(handlers[0]);
        assertNotNull(handlers[1]);
    }
}
