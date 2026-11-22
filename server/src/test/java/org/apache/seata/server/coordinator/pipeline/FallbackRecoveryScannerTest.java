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
package org.apache.seata.server.coordinator.pipeline;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.coordinator.pipeline.FallbackRecoveryScanner.ScanSummary;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionManager;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.BooleanSupplier;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class FallbackRecoveryScannerTest {

    @BeforeAll
    static void initEnvironment() {
        if (ObjectHolder.INSTANCE.getObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT) == null) {
            ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        }
    }

    private static GlobalSession session(String xid, long gmtModifiedMs) {
        return session(xid, new Date(gmtModifiedMs));
    }

    private static GlobalSession session(String xid, Date gmtModified) {
        GlobalSession session = new GlobalSession("app", "group", "tx", 3000, false);
        session.setXid(xid);
        session.setStatus(GlobalStatus.AsyncCommitting);
        session.setGmtModified(gmtModified);
        return session;
    }

    private static ConditionSnapshot snapshot(SessionCondition condition) {
        return new ConditionSnapshot(
                condition.getStatuses(),
                condition.isLazyLoadBranch(),
                condition.getCursorGmtModified(),
                condition.getCursorXid(),
                condition.getPageSize(),
                condition.getOffset());
    }

    private static void awaitCondition(BooleanSupplier condition, long timeoutMs) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) {
            Thread.sleep(5L);
        }
        assertTrue(condition.getAsBoolean(), "Condition not satisfied within " + timeoutMs + "ms");
    }

    @Test
    void rejectsUnsupportedModesAndFallsBackNonPositiveLimits() {
        PipelineConfig config = PipelineConfig.builder().enabled(true).build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor =
                new PhaseTwoPipelineExecutor(config, s -> false, null, registry, guard, null);
        SessionManager manager = mock(SessionManager.class);

        assertThrows(
                IllegalArgumentException.class,
                () -> new FallbackRecoveryScanner(
                        config, () -> manager, executor, guard, registry, SessionMode.FILE, 10, 1000L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new FallbackRecoveryScanner(
                        config, () -> manager, executor, guard, registry, SessionMode.RAFT, 10, 1000L));

        FallbackRecoveryScanner scanner =
                new FallbackRecoveryScanner(config, () -> manager, executor, guard, registry, SessionMode.DB, 0, -5L);
        assertEquals(DefaultValues.DEFAULT_QUERY_LIMIT, scanner.getPageSize());
        assertEquals(1000L, scanner.getMaxTickDurationMs());
        assertEquals(SessionMode.DB, scanner.getSessionMode());
        assertTrue(executor.shutdown(1000L));
    }

    @Test
    void dbModeContinuouslyPagesAcrossMultiplePagesWithTieBreaking() throws InterruptedException {
        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .rpcPoolSize(2)
                .queueCapacity(32)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        Set<String> committed = ConcurrentHashMap.newKeySet();
        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                s -> {
                    committed.add(s.getXid());
                    return false;
                },
                null,
                registry,
                guard,
                null);
        executor.start();

        long sameSecond = 1_000_000L;
        List<GlobalSession> storeRows = Arrays.asList(
                session("10.0.0.1:8091:1001", sameSecond),
                session("10.0.0.1:8091:1002", sameSecond),
                session("10.0.0.1:8091:1003", sameSecond),
                session("10.0.0.1:8091:1004", sameSecond + 1000L),
                session("10.0.0.1:8091:1005", sameSecond + 2000L));
        storeRows.sort(
                Comparator.comparing((GlobalSession s) -> s.getGmtModified().getTime())
                        .thenComparing(GlobalSession::getXid));

        List<ConditionSnapshot> queries = new ArrayList<>();
        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition cond = invocation.getArgument(0);
            queries.add(snapshot(cond));
            List<GlobalSession> page = new ArrayList<>();
            int limit = cond.getPageSize() != null ? cond.getPageSize() : 2;
            for (GlobalSession row : storeRows) {
                if (cond.hasCursor()) {
                    long rowTs = row.getGmtModified().getTime();
                    long curTs = cond.getCursorGmtModified().getTime();
                    boolean after =
                            rowTs > curTs || (rowTs == curTs && row.getXid().compareTo(cond.getCursorXid()) > 0);
                    if (!after) {
                        continue;
                    }
                }
                page.add(row);
                if (page.size() >= limit) {
                    break;
                }
            }
            return page;
        });

        try {
            FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                    config, () -> manager, executor, guard, registry, SessionMode.DB, 2, 5000L);
            ScanSummary summary = scanner.scanAsyncCommitting();

            assertEquals(3, summary.getPagesScanned());
            assertEquals(5, summary.getRowsExamined());
            assertEquals(5, summary.getDispatched());
            assertEquals(0, summary.getSkippedInFlight());
            assertEquals(0, summary.getSkippedQuietWindow());
            assertFalse(summary.isStoppedByBackpressure());
            assertFalse(summary.isStoppedByLimit());
            assertFalse(summary.isStoppedByCursorGuard());

            assertEquals(3, queries.size());
            assertNull(queries.get(0).cursorGmtModified);
            assertNull(queries.get(0).cursorXid);
            assertFalse(queries.get(0).lazyLoadBranch);
            assertEquals(2, queries.get(0).pageSize);
            assertEquals(GlobalStatus.AsyncCommitting, queries.get(0).statuses[0]);

            assertEquals(new Date(sameSecond), queries.get(1).cursorGmtModified);
            assertEquals("10.0.0.1:8091:1002", queries.get(1).cursorXid);

            assertEquals(new Date(sameSecond + 1000L), queries.get(2).cursorGmtModified);
            assertEquals("10.0.0.1:8091:1004", queries.get(2).cursorXid);

            awaitCondition(() -> committed.size() == 5, 2000L);
        } finally {
            assertTrue(executor.shutdown(2000L));
        }
    }

    @Test
    void dbModeCursorGuardStopsWhenCustomStoreIgnoresCursor() {
        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .rpcPoolSize(1)
                .queueCapacity(16)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor =
                new PhaseTwoPipelineExecutor(config, s -> false, null, registry, guard, null);
        executor.start();

        List<GlobalSession> constantPage =
                Arrays.asList(session("10.0.0.1:8091:2001", 1000L), session("10.0.0.1:8091:2002", 2000L));
        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenReturn(constantPage);

        try {
            FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                    config, () -> manager, executor, guard, registry, SessionMode.DB, 2, 5000L);
            ScanSummary summary = scanner.scanAsyncCommitting();

            assertEquals(1, summary.getPagesScanned());
            assertEquals(2, summary.getRowsExamined());
            assertEquals(2, summary.getDispatched());
            assertTrue(summary.isStoppedByCursorGuard());
            assertFalse(summary.isStoppedByLimit());
            assertFalse(summary.isStoppedByBackpressure());
            verify(manager, times(2)).findGlobalSessions(any(SessionCondition.class));
        } finally {
            assertTrue(executor.shutdown(2000L));
        }
    }

    @Test
    void dbModeCursorGuardStopsWhenTrailingRowHasNullCursorField() {
        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .rpcPoolSize(1)
                .queueCapacity(16)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(2, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor =
                new PhaseTwoPipelineExecutor(config, s -> false, null, registry, guard, null);
        executor.start();

        List<GlobalSession> pageWithNullDate =
                Arrays.asList(session("10.0.0.1:8091:2101", 1000L), session("10.0.0.1:8091:2102", null));
        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenReturn(pageWithNullDate);

        try {
            FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                    config, () -> manager, executor, guard, registry, SessionMode.DB, 2, 5000L);
            ScanSummary summary = scanner.scanAsyncCommitting();

            assertEquals(1, summary.getPagesScanned());
            assertEquals(2, summary.getDispatched());
            assertTrue(summary.isStoppedByCursorGuard());
            verify(manager, times(1)).findGlobalSessions(any(SessionCondition.class));
        } finally {
            assertTrue(executor.shutdown(2000L));
        }
    }

    @Test
    void skipsInFlightSessionsAndExecutorDuplicates() {
        PipelineConfig config =
                PipelineConfig.builder().enabled(true).scanQuietWindowMs(0L).build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        long token = registry.tryAcquire("10.0.0.1:8091:3001");
        assertTrue(token > 0L);

        PhaseTwoPipelineExecutor executor = mock(PhaseTwoPipelineExecutor.class);
        when(executor.isRunning()).thenReturn(true);
        when(executor.tryOffer(any(GlobalSession.class))).thenAnswer(invocation -> {
            GlobalSession s = invocation.getArgument(0);
            return "10.0.0.1:8091:3002".equals(s.getXid()) ? OfferResult.DUPLICATE : OfferResult.ACCEPTED;
        });

        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class)))
                .thenReturn(Arrays.asList(
                        session("10.0.0.1:8091:3001", 1000L),
                        session("10.0.0.1:8091:3002", 2000L),
                        session("10.0.0.1:8091:3003", 3000L)));

        FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                config, () -> manager, executor, guard, registry, SessionMode.DB, 10, 5000L);
        ScanSummary summary = scanner.scanAsyncCommitting();

        assertEquals(1, summary.getPagesScanned());
        assertEquals(3, summary.getRowsExamined());
        assertEquals(1, summary.getDispatched());
        assertEquals(2, summary.getSkippedInFlight());
        verify(executor, times(2)).tryOffer(any(GlobalSession.class));
    }

    @Test
    void filtersRecentSessionsWithinQuietWindowUsingInjectedClock() {
        long nowMs = 10_000L;
        long quietWindowMs = 2_000L;
        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .scanQuietWindowMs(quietWindowMs)
                .build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor = mock(PhaseTwoPipelineExecutor.class);
        when(executor.isRunning()).thenReturn(true);
        when(executor.tryOffer(any(GlobalSession.class))).thenReturn(OfferResult.ACCEPTED);

        List<GlobalSession> page = Arrays.asList(
                session("10.0.0.1:8091:4001", nowMs - 500L),
                session("10.0.0.1:8091:4002", nowMs - quietWindowMs),
                session("10.0.0.1:8091:4003", nowMs - 5_000L),
                session("10.0.0.1:8091:4004", nowMs + 1_000L),
                session("10.0.0.1:8091:4005", null));

        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenReturn(page);

        FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                config, () -> manager, executor, guard, registry, SessionMode.DB, 10, 5000L, () -> nowMs, () -> 0L);
        ScanSummary summary = scanner.scanAsyncCommitting();

        assertEquals(1, summary.getPagesScanned());
        assertEquals(5, summary.getRowsExamined());
        assertEquals(4, summary.getDispatched());
        assertEquals(1, summary.getSkippedQuietWindow());
        verify(executor, times(4)).tryOffer(any(GlobalSession.class));
    }

    @Test
    void stopsImmediatelyOnBackpressureWhenQueueFullOrNotRunning() throws InterruptedException {
        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .rpcPoolSize(1)
                .queueCapacity(1)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        CountDownLatch workerStarted = new CountDownLatch(1);
        CountDownLatch releaseWorker = new CountDownLatch(1);
        PhaseTwoPipelineExecutor executor = new PhaseTwoPipelineExecutor(
                config,
                s -> {
                    workerStarted.countDown();
                    try {
                        releaseWorker.await(5, TimeUnit.SECONDS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return false;
                },
                null,
                registry,
                guard,
                null);
        executor.start();

        try {
            assertEquals(OfferResult.ACCEPTED, executor.tryOffer(session("10.0.0.1:8091:5000", 100L)));
            assertTrue(workerStarted.await(2, TimeUnit.SECONDS));

            SessionManager manager = mock(SessionManager.class);
            when(manager.findGlobalSessions(any(SessionCondition.class)))
                    .thenReturn(Arrays.asList(
                            session("10.0.0.1:8091:5001", 1000L),
                            session("10.0.0.1:8091:5002", 2000L),
                            session("10.0.0.1:8091:5003", 3000L)));

            FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                    config, () -> manager, executor, guard, registry, SessionMode.DB, 3, 5000L);
            ScanSummary summary = scanner.scanAsyncCommitting();

            // 5001 fills queueCapacity=1, 5002 triggers QUEUE_FULL and stops immediately before 5003 or page 2.
            assertEquals(1, summary.getPagesScanned());
            assertEquals(2, summary.getRowsExamined());
            assertEquals(1, summary.getDispatched());
            assertTrue(summary.isStoppedByBackpressure());
            verify(manager, times(1)).findGlobalSessions(any(SessionCondition.class));
        } finally {
            releaseWorker.countDown();
            assertTrue(executor.shutdown(2000L));
        }

        SessionManager unusedManager = mock(SessionManager.class);
        FallbackRecoveryScanner stoppedScanner = new FallbackRecoveryScanner(
                config, () -> unusedManager, executor, guard, registry, SessionMode.DB, 2, 5000L);
        ScanSummary stoppedSummary = stoppedScanner.scanAsyncCommitting();
        assertTrue(stoppedSummary.isStoppedByBackpressure());
        assertEquals(0, stoppedSummary.getPagesScanned());
        verifyNoInteractions(unusedManager);
    }

    @Test
    void enforcesMaxPagesPerTickAndTickDurationBudget() {
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor = mock(PhaseTwoPipelineExecutor.class);
        when(executor.isRunning()).thenReturn(true);
        when(executor.tryOffer(any(GlobalSession.class))).thenReturn(OfferResult.ACCEPTED);

        PipelineConfig maxPagesConfig = PipelineConfig.builder()
                .enabled(true)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(2)
                .build();

        AtomicInteger callCount = new AtomicInteger(0);
        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            int pageNum = callCount.incrementAndGet();
            long base = pageNum * 100L;
            return Arrays.asList(
                    session("10.0.0.1:8091:" + (base + 1), base + 1), session("10.0.0.1:8091:" + (base + 2), base + 2));
        });

        FallbackRecoveryScanner pageLimitedScanner = new FallbackRecoveryScanner(
                maxPagesConfig, () -> manager, executor, guard, registry, SessionMode.DB, 2, 5000L);
        ScanSummary pageLimitSummary = pageLimitedScanner.scanAsyncCommitting();

        assertEquals(2, pageLimitSummary.getPagesScanned());
        assertEquals(4, pageLimitSummary.getDispatched());
        assertTrue(pageLimitSummary.isStoppedByLimit());
        assertFalse(pageLimitSummary.isStoppedByBackpressure());
        assertFalse(pageLimitSummary.isStoppedByCursorGuard());
        verify(manager, times(2)).findGlobalSessions(any(SessionCondition.class));

        PipelineConfig highPageConfig = PipelineConfig.builder()
                .enabled(true)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(50)
                .build();
        AtomicLong nanoClock = new AtomicLong(1_000_000_000L);
        SessionManager slowManager = mock(SessionManager.class);
        when(slowManager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            nanoClock.addAndGet(TimeUnit.MILLISECONDS.toNanos(500L));
            return Arrays.asList(session("10.0.0.1:8091:6001", 1000L), session("10.0.0.1:8091:6002", 2000L));
        });

        FallbackRecoveryScanner timeLimitedScanner = new FallbackRecoveryScanner(
                highPageConfig,
                () -> slowManager,
                executor,
                guard,
                registry,
                SessionMode.DB,
                2,
                500L,
                () -> 100_000L,
                nanoClock::get);
        ScanSummary timeLimitSummary = timeLimitedScanner.scanAsyncCommitting();

        assertEquals(1, timeLimitSummary.getPagesScanned());
        assertEquals(2, timeLimitSummary.getDispatched());
        assertTrue(timeLimitSummary.isStoppedByLimit());
        verify(slowManager, times(1)).findGlobalSessions(any(SessionCondition.class));
    }

    @Test
    void redisModeContinuesPastShortPagesUntilEmptyAndDeduplicatesSameTick() {
        PipelineConfig config = PipelineConfig.builder()
                .enabled(true)
                .scanQuietWindowMs(0L)
                .scanMaxPagesPerTick(10)
                .build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor = mock(PhaseTwoPipelineExecutor.class);
        when(executor.isRunning()).thenReturn(true);
        when(executor.tryOffer(any(GlobalSession.class))).thenReturn(OfferResult.ACCEPTED);

        Map<Integer, List<GlobalSession>> pagesByOffset = new HashMap<>();
        pagesByOffset.put(0, Arrays.asList(session("10.0.0.1:8091:7001", 1000L), session("10.0.0.1:8091:7002", 2000L)));
        pagesByOffset.put(3, Arrays.asList(session("10.0.0.1:8091:7002", 2000L), session("10.0.0.1:8091:7003", 3000L)));
        pagesByOffset.put(6, Collections.emptyList());

        List<ConditionSnapshot> queries = new ArrayList<>();
        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            SessionCondition cond = invocation.getArgument(0);
            queries.add(snapshot(cond));
            return pagesByOffset.getOrDefault(cond.getOffset(), Collections.emptyList());
        });

        FallbackRecoveryScanner scanner = new FallbackRecoveryScanner(
                config, () -> manager, executor, guard, registry, SessionMode.REDIS, 3, 5000L);
        ScanSummary summary = scanner.scanAsyncCommitting();

        assertEquals(2, summary.getPagesScanned());
        assertEquals(4, summary.getRowsExamined());
        assertEquals(3, summary.getDispatched());
        assertFalse(summary.isStoppedByLimit());
        assertFalse(summary.isStoppedByBackpressure());
        assertFalse(summary.isStoppedByCursorGuard());

        assertEquals(3, queries.size());
        assertEquals(Integer.valueOf(0), queries.get(0).offset);
        assertEquals(Integer.valueOf(3), queries.get(1).offset);
        assertEquals(Integer.valueOf(6), queries.get(2).offset);
        for (ConditionSnapshot q : queries) {
            assertNull(q.cursorGmtModified);
            assertNull(q.cursorXid);
            assertEquals(Integer.valueOf(3), q.pageSize);
        }
        verify(executor, times(3)).tryOffer(any(GlobalSession.class));
    }

    @Test
    void executesQueriesUnderDbResourceGuardAndHandlesInterruptionCleanly() throws InterruptedException {
        PipelineConfig config =
                PipelineConfig.builder().enabled(true).scanQuietWindowMs(0L).build();
        BackgroundDbResourceGuard guard = new BackgroundDbResourceGuard(1, null);
        InFlightSessionRegistry registry = new InFlightSessionRegistry(5000L);
        PhaseTwoPipelineExecutor executor = mock(PhaseTwoPipelineExecutor.class);
        when(executor.isRunning()).thenReturn(true);
        when(executor.tryOffer(any(GlobalSession.class))).thenReturn(OfferResult.ACCEPTED);

        SessionManager manager = mock(SessionManager.class);
        when(manager.findGlobalSessions(any(SessionCondition.class))).thenAnswer(invocation -> {
            assertEquals(0, guard.availablePermits());
            assertEquals(1, guard.getActiveCount());
            return Collections.singletonList(session("10.0.0.1:8091:8001", 1000L));
        });

        FallbackRecoveryScanner scanner =
                new FallbackRecoveryScanner(config, () -> manager, executor, guard, registry, SessionMode.DB, 5, 5000L);
        ScanSummary normalSummary = scanner.scanAsyncCommitting();

        assertEquals(1, normalSummary.getDispatched());
        assertEquals(1, guard.availablePermits());
        assertEquals(0, guard.getActiveCount());

        // Hold the only guard permit and interrupt the calling thread to verify deterministic abort.
        try (BackgroundDbResourceGuard.Permit held = guard.tryAcquire(0L).orElseThrow(AssertionError::new)) {
            assertNotNull(held);
            Thread.currentThread().interrupt();
            ScanSummary interruptedSummary = scanner.scanAsyncCommitting();
            assertTrue(Thread.interrupted());
            assertEquals(0, interruptedSummary.getPagesScanned());
            assertEquals(0, interruptedSummary.getDispatched());
        }
        assertEquals(1, guard.availablePermits());
    }

    private static final class ConditionSnapshot {
        private final GlobalStatus[] statuses;
        private final boolean lazyLoadBranch;
        private final Date cursorGmtModified;
        private final String cursorXid;
        private final Integer pageSize;
        private final Integer offset;

        private ConditionSnapshot(
                GlobalStatus[] statuses,
                boolean lazyLoadBranch,
                Date cursorGmtModified,
                String cursorXid,
                Integer pageSize,
                Integer offset) {
            this.statuses = statuses;
            this.lazyLoadBranch = lazyLoadBranch;
            this.cursorGmtModified = cursorGmtModified;
            this.cursorXid = cursorXid;
            this.pageSize = pageSize;
            this.offset = offset;
        }
    }
}
