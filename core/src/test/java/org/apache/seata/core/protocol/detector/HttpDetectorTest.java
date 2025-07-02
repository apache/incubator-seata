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
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.HttpServerUpgradeHandler;
import org.apache.seata.core.rpc.netty.http.HttpDispatchHandler;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

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
        assertEquals(6, handlers.length);
        assertInstanceOf(HttpServerCodec.class, handlers[0]);
        assertInstanceOf(HttpServerUpgradeHandler.class, handlers[1]);
        assertInstanceOf(ChannelHandler.class, handlers[2]); // upgradeCleanupHandler
        assertInstanceOf(HttpObjectAggregator.class, handlers[3]);
        assertInstanceOf(HttpDispatchHandler.class, handlers[4]);
        assertInstanceOf(ChannelHandler.class, handlers[5]); // finalExceptionHandler

        // Verify aggregator size
        HttpObjectAggregator aggregator = (HttpObjectAggregator) handlers[3];
        assertEquals(1048576, aggregator.maxContentLength());
    }

    @Test
    void testDetectWithShortBuffer() {
        ByteBuf buf = Unpooled.copiedBuffer("GET", StandardCharsets.UTF_8);
        assertFalse(httpDetector.detect(buf));
        buf.release();
    }

    @Test
    void testStartsWithNegativeBranch() {
        ByteBuf buf = Unpooled.copiedBuffer("GE", StandardCharsets.UTF_8);
        try {
            java.lang.reflect.Method m =
                    HttpDetector.class.getDeclaredMethod("startsWith", ByteBuf.class, String.class);
            m.setAccessible(true);
            boolean result = (boolean) m.invoke(httpDetector, buf, "GET");
            assertFalse(result);
        } catch (Exception e) {
            fail(e);
        }
        buf.release();
    }

    @Test
    void testUpgradeHandlerNonHttp2() {
        try {
            java.lang.reflect.Method m = HttpDetector.class.getDeclaredMethod(
                    "getHttpServerUpgradeHandler", io.netty.handler.codec.http.HttpServerCodec.class);
            m.setAccessible(true);
            io.netty.handler.codec.http.HttpServerCodec codec = new io.netty.handler.codec.http.HttpServerCodec();
            Object handler = m.invoke(null, codec);
            assertNotNull(handler);
            assertTrue(handler instanceof io.netty.handler.codec.http.HttpServerUpgradeHandler);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    void testUpgradeCleanupHandlerEvent() throws Exception {
        ChannelHandler[] handlers = httpDetector.getHandlers();
        ChannelInboundHandlerAdapter upgradeCleanupHandler = (ChannelInboundHandlerAdapter) handlers[2];
        HttpServerUpgradeHandler.UpgradeEvent mockEvent = mock(HttpServerUpgradeHandler.UpgradeEvent.class);
        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
        ChannelPipeline mockPipeline = mock(ChannelPipeline.class);
        when(mockCtx.pipeline()).thenReturn(mockPipeline);
        upgradeCleanupHandler.userEventTriggered(mockCtx, mockEvent);
        verify(mockPipeline).remove(HttpObjectAggregator.class);
        verify(mockPipeline).remove(HttpDispatchHandler.class);
    }

    @Test
    void testUpgradeCleanupHandlerNonUpgradeEvent() throws Exception {
        ChannelHandler[] handlers = httpDetector.getHandlers();
        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
        ChannelInboundHandlerAdapter upgradeCleanupHandler = (ChannelInboundHandlerAdapter) handlers[2];
        upgradeCleanupHandler.userEventTriggered(mockCtx, "not-upgrade-event");
    }

    @Test
    void testFinalExceptionHandler() throws Exception {
        ChannelHandler[] handlers = httpDetector.getHandlers();
        ChannelHandler finalExceptionHandler = handlers[5];
        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
        finalExceptionHandler.exceptionCaught(mockCtx, new java.io.IOException("test"));
        finalExceptionHandler.exceptionCaught(mockCtx, new RuntimeException("test"));
        verify(mockCtx, times(2)).close();
    }

    @Test
    void testFinalExceptionHandlerNonIOException() throws Exception {
        ChannelHandler[] handlers = httpDetector.getHandlers();
        ChannelInboundHandlerAdapter finalExceptionHandler = (ChannelInboundHandlerAdapter) handlers[5];
        ChannelHandlerContext mockCtx = mock(ChannelHandlerContext.class);
        finalExceptionHandler.exceptionCaught(mockCtx, new IllegalArgumentException("test"));
        verify(mockCtx).close();
    }
}
