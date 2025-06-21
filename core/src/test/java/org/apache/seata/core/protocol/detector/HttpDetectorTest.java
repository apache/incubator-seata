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
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class HttpDetectorTest {
    private final HttpDetector httpDetector = new HttpDetector();

    @Test
    void testDetectWithHttpMethods() {
        // Test all HTTP methods
        String[] methods = {"GET", "POST", "PUT", "DELETE", "HEAD", "OPTIONS", "PATCH"};

        for (String method : methods) {
            String request = method + " /path HTTP/1.1\r\n";
            ByteBuf buf = Unpooled.copiedBuffer(request, StandardCharsets.UTF_8);
            assertTrue(httpDetector.detect(buf), "Should detect " + method + " as HTTP");
            buf.release();
        }
    }

    @Test
    void testDetectWithNonHttp() {
        // Test non-HTTP protocol
        ByteBuf buf = Unpooled.copiedBuffer("NOTHTTP /path", StandardCharsets.UTF_8);
        assertFalse(httpDetector.detect(buf));
        buf.release();
    }

    @Test
    void testGetHandlers() {
        ChannelHandler[] handlers = httpDetector.getHandlers();
        assertEquals(3, handlers.length);
        assertInstanceOf(HttpServerCodec.class, handlers[0]);
        assertInstanceOf(HttpObjectAggregator.class, handlers[1]);
        assertInstanceOf(HttpDispatchHandler.class, handlers[2]);

        // Verify aggregator size
        HttpObjectAggregator aggregator = (HttpObjectAggregator) handlers[1];
        assertEquals(1048576, aggregator.maxContentLength());
    }
}
