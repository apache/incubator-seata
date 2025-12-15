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
package org.apache.seata.server.cluster.manager;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import org.apache.seata.common.rpc.http.HttpContext;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.watch.Watcher;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ClusterWatcherManagerTest extends BaseSpringBootTest {

    private ClusterWatcherManager clusterWatcherManager;
    private ChannelHandlerContext mockChannelHandlerContext;
    private Channel mockChannel;
    private HttpContext<Object> httpContext;

    private static final String TEST_GROUP = "test-group";
    private static final int TEST_TIMEOUT = 5000;
    private static final long TEST_TERM = 1000L;

    @BeforeEach
    void setUp() {
        clusterWatcherManager = new ClusterWatcherManager();

        mockChannel = mock(Channel.class);
        mockChannelHandlerContext = mock(ChannelHandlerContext.class);
        when(mockChannelHandlerContext.channel()).thenReturn(mockChannel);

        Object mockRequest = new Object();
        httpContext = new HttpContext<>(mockRequest, mockChannelHandlerContext, true, HttpContext.HTTP_1_1);

        Map<String, Queue<Watcher<HttpContext>>> watchers = (Map<String, Queue<Watcher<HttpContext>>>)
                ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        Map<String, Long> groupUpdateTerm =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");
        if (watchers != null) {
            watchers.clear();
        }
        if (groupUpdateTerm != null) {
            groupUpdateTerm.clear();
        }
    }

    @AfterEach
    void tearDown() {
        ScheduledThreadPoolExecutor executor = (ScheduledThreadPoolExecutor)
                ReflectionTestUtils.getField(clusterWatcherManager, "scheduledThreadPoolExecutor");
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
            try {
                executor.awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    @Test
    void testSendWatcherResponseWithInactiveChannel() {
        when(mockChannel.isActive()).thenReturn(false);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, TEST_TIMEOUT, TEST_TERM);

        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(clusterWatcherManager, "notifyWatcher", watcher);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());
        verify(mockChannelHandlerContext, never()).flush();

        assertTrue(watcher.isDone());
    }

    @Test
    void testSendWatcherResponseWithActiveChannel_Http1() {
        when(mockChannel.isActive()).thenReturn(true);
        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, TEST_TIMEOUT, TEST_TERM);

        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(clusterWatcherManager, "notifyWatcher", watcher);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, atLeastOnce()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testSendWatcherResponseWithActiveChannel_Http2() {
        // Test normal flow with active channel (HTTP/2)
        when(mockChannel.isActive()).thenReturn(true);

        // Mock writeAndFlush to return a non-null ChannelFuture
        ChannelFuture mockChannelFuture = mock(ChannelFuture.class);
        when(mockChannelHandlerContext.writeAndFlush(any())).thenReturn(mockChannelFuture);
        when(mockChannelFuture.addListener(any())).thenReturn(mockChannelFuture);

        // Create HTTP/2 context
        HttpContext<Object> http2Context =
                new HttpContext<>(new Object(), mockChannelHandlerContext, true, HttpContext.HTTP_2_0);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, http2Context, TEST_TIMEOUT, TEST_TERM);

        assertDoesNotThrow(() -> {
            ReflectionTestUtils.invokeMethod(clusterWatcherManager, "notifyWatcher", watcher);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, atLeastOnce()).write(any());
        verify(mockChannelHandlerContext, atLeastOnce()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testOnChangeEventWithInactiveChannel() {
        when(mockChannel.isActive()).thenReturn(false);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, TEST_TIMEOUT, TEST_TERM);

        clusterWatcherManager.registryWatcher(watcher);

        Map<String, Queue<Watcher<HttpContext>>> watchers = (Map<String, Queue<Watcher<HttpContext>>>)
                ReflectionTestUtils.getField(clusterWatcherManager, "WATCHERS");
        assertTrue(watchers.containsKey(TEST_GROUP));
        assertEquals(1, watchers.get(TEST_GROUP).size());

        ClusterChangeEvent event = new ClusterChangeEvent(this, TEST_GROUP, TEST_TERM + 1, true);

        assertDoesNotThrow(() -> {
            clusterWatcherManager.onChangeEvent(event);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testTimeoutWithInactiveChannel() throws InterruptedException {
        when(mockChannel.isActive()).thenReturn(false);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, 1000, TEST_TERM);

        clusterWatcherManager.registryWatcher(watcher);

        clusterWatcherManager.init();

        Thread.sleep(2500);

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testRegistryWatcherWithInactiveChannel() {
        when(mockChannel.isActive()).thenReturn(false);

        Map<String, Long> groupUpdateTerm =
                (Map<String, Long>) ReflectionTestUtils.getField(clusterWatcherManager, "GROUP_UPDATE_TERM");
        groupUpdateTerm.put(TEST_GROUP, TEST_TERM + 10);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, httpContext, TEST_TIMEOUT, TEST_TERM);

        assertDoesNotThrow(() -> {
            clusterWatcherManager.registryWatcher(watcher);
        });

        verify(mockChannel, atLeastOnce()).isActive();

        verify(mockChannelHandlerContext, never()).write(any());
        verify(mockChannelHandlerContext, never()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }

    @Test
    void testHttp2WriteAndFlushFailedShouldTriggerListener() throws Exception {
        when(mockChannel.isActive()).thenReturn(true);

        ChannelFuture mockFuture = mock(ChannelFuture.class);
        when(mockChannelHandlerContext.writeAndFlush(any())).thenReturn(mockFuture);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<GenericFutureListener<? extends Future<? super Void>>> listenerCaptor =
                ArgumentCaptor.forClass(GenericFutureListener.class);

        when(mockFuture.addListener(listenerCaptor.capture())).thenReturn(mockFuture);

        RuntimeException cause = new RuntimeException("mock http2 write failed");
        when(mockFuture.isSuccess()).thenReturn(false);
        when(mockFuture.cause()).thenReturn(cause);

        HttpContext<Object> http2Context =
                new HttpContext<>(new Object(), mockChannelHandlerContext, true, HttpContext.HTTP_2_0);

        Watcher<HttpContext> watcher = new Watcher<>(TEST_GROUP, http2Context, TEST_TIMEOUT, TEST_TERM);

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(clusterWatcherManager, "notifyWatcher", watcher));

        GenericFutureListener<? extends Future<? super Void>> listener = listenerCaptor.getValue();
        assertNotNull(listener);

        verify(mockChannel, atLeastOnce()).isActive();
        verify(mockChannelHandlerContext, atLeastOnce()).write(any());
        verify(mockChannelHandlerContext, atLeastOnce()).writeAndFlush(any());

        assertTrue(watcher.isDone());
    }
}
