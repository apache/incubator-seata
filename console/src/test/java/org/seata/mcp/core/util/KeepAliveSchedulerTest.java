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
package org.seata.mcp.core.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.seata.mcp.core.protocol.ProtocolDefinition;
import org.apache.seata.mcp.core.protocol.RuntimeSession;
import org.apache.seata.mcp.core.protocol.StreamableServerRuntimeSession;
import org.apache.seata.mcp.core.util.KeepAliveScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for KeepAliveScheduler
 */
@ExtendWith(MockitoExtension.class)
class KeepAliveSchedulerTest {

    @Mock
    private RuntimeSession mockSession;

    @Mock
    private StreamableServerRuntimeSession mockStreamableSession;

    private Scheduler testScheduler;
    private Supplier<Flux<RuntimeSession>> sessionSupplier;
    private List<RuntimeSession> sessions;

    @BeforeEach
    void setUp() {
        testScheduler = Schedulers.newSingle("test");
        sessions = new ArrayList<>();
        sessionSupplier = () -> Flux.fromIterable(sessions);
    }

    @AfterEach
    void tearDown() {
        if (testScheduler != null) {
            testScheduler.dispose();
        }
    }

    @Test
    void testBuilder() {
        KeepAliveScheduler.Builder builder = KeepAliveScheduler.builder(sessionSupplier);
        assertNotNull(builder);
        Scheduler customScheduler = Schedulers.newSingle("builder-test");
        Duration initialDelay = Duration.ofSeconds(1);
        Duration interval = Duration.ofSeconds(5);
        Consumer<RuntimeSession> handler = session -> {};

        KeepAliveScheduler scheduler = builder.scheduler(customScheduler)
                .initialDelay(initialDelay)
                .interval(interval)
                .sessionFailureHandler(handler)
                .build();

        assertNotNull(scheduler);
        assertEquals(handler, scheduler.getSessionFailureHandler());
        customScheduler.dispose();
    }

    @Test
    void testStartAndStop() {
        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ofMillis(100))
                .interval(Duration.ofMillis(200))
                .build();

        assertFalse(scheduler.isRunning());
        Disposable disposable = scheduler.start();
        assertNotNull(disposable);
        assertTrue(scheduler.isRunning());

        scheduler.stop();
        assertFalse(scheduler.isRunning());
        assertTrue(disposable.isDisposed());
    }

    @Test
    void testStartWhenAlreadyRunning() {
        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ofMillis(100))
                .interval(Duration.ofMillis(200))
                .build();

        scheduler.start();
        assertThrows(IllegalStateException.class, scheduler::start);
    }

    @Test
    void testShutdown() {
        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ofMillis(100))
                .interval(Duration.ofMillis(200))
                .build();

        scheduler.start();
        assertTrue(scheduler.isRunning());
        scheduler.shutdown();
        assertFalse(scheduler.isRunning());
    }

    @Test
    void testProcessKeepAliveSuccess() {
        when(mockSession.toString()).thenReturn("session-1");
        when(mockSession.sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class)))
                .thenReturn(Mono.just("pong"));
        sessions.add(mockSession);

        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ZERO)
                .interval(Duration.ofMillis(100))
                .build();

        Disposable disposable = scheduler.start();
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.stop();

        verify(mockSession, atLeastOnce())
                .sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class));
    }

    @Test
    void testProcessKeepAliveFailure() {
        when(mockSession.toString()).thenReturn("session-1");
        when(mockSession.sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));
        sessions.add(mockSession);

        AtomicInteger failureCount = new AtomicInteger(0);
        Consumer<RuntimeSession> failureHandler = session -> failureCount.incrementAndGet();

        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ZERO)
                .interval(Duration.ofMillis(50))
                .sessionFailureHandler(failureHandler)
                .build();

        Disposable disposable = scheduler.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.stop();

        verify(mockSession, atLeast(3))
                .sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class));
        assertTrue(failureCount.get() >= 1);
    }

    @Test
    void testProcessKeepAliveWithStreamableSession() {
        when(mockStreamableSession.toString()).thenReturn("streamable-session-1");
        when(mockStreamableSession.sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));
        sessions.add(mockStreamableSession);

        AtomicInteger failureCount = new AtomicInteger(0);
        Consumer<RuntimeSession> failureHandler = session -> failureCount.incrementAndGet();

        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ZERO)
                .interval(Duration.ofMillis(50))
                .sessionFailureHandler(failureHandler)
                .build();

        Disposable disposable = scheduler.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.stop();

        verify(mockStreamableSession, atLeast(3))
                .sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class));
        verify(mockStreamableSession, atLeastOnce()).setHealthy(false);
        assertTrue(failureCount.get() >= 1);
    }

    @Test
    void testProcessKeepAliveWithMultipleSessions() {
        RuntimeSession session1 = mock(RuntimeSession.class);
        RuntimeSession session2 = mock(RuntimeSession.class);
        when(session1.toString()).thenReturn("session-1");
        when(session2.toString()).thenReturn("session-2");
        when(session1.sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class)))
                .thenReturn(Mono.just("pong"));
        when(session2.sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class)))
                .thenReturn(Mono.just("pong"));
        sessions.add(session1);
        sessions.add(session2);

        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ZERO)
                .interval(Duration.ofMillis(100))
                .build();

        Disposable disposable = scheduler.start();
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.stop();

        verify(session1, atLeastOnce())
                .sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class));
        verify(session2, atLeastOnce())
                .sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class));
    }

    @Test
    void testProcessKeepAliveWithEmptySessions() {
        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ZERO)
                .interval(Duration.ofMillis(100))
                .build();

        Disposable disposable = scheduler.start();
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.stop();

        assertTrue(sessions.isEmpty());
    }

    @Test
    void testProcessKeepAliveFailureHandlerException() {
        when(mockSession.toString()).thenReturn("session-1");
        when(mockSession.sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class)))
                .thenReturn(Mono.error(new RuntimeException("Connection failed")));
        sessions.add(mockSession);

        Consumer<RuntimeSession> failureHandler = session -> {
            throw new RuntimeException("Handler error");
        };

        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .scheduler(testScheduler)
                .initialDelay(Duration.ZERO)
                .interval(Duration.ofMillis(50))
                .sessionFailureHandler(failureHandler)
                .build();

        Disposable disposable = scheduler.start();
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        scheduler.stop();

        verify(mockSession, atLeast(3))
                .sendRequest(eq(ProtocolDefinition.METHOD_PING), any(), any(TypeReference.class));
    }

    @Test
    void testGetSessionFailureHandler() {
        Consumer<RuntimeSession> handler = session -> {};
        KeepAliveScheduler scheduler = KeepAliveScheduler.builder(sessionSupplier)
                .sessionFailureHandler(handler)
                .build();
        assertEquals(handler, scheduler.getSessionFailureHandler());
    }

    @Test
    void testGetSessionFailureHandlerNull() {
        KeepAliveScheduler scheduler =
                KeepAliveScheduler.builder(sessionSupplier).build();
        assertNull(scheduler.getSessionFailureHandler());
    }
}
