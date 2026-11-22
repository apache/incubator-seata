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
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Fallback recovery scanner that continuously pages through eligible global sessions within a
 * single distributed-lock tick and dispatches them into {@link PhaseTwoPipelineExecutor}.
 *
 * <p>Supports two storage pagination contracts:
 * <ul>
 *   <li>{@link SessionMode#DB}: continuous composite keyset pagination ordered by
 *       {@code (gmt_modified, xid)} with an explicit cursor-advancement guard against custom
 *       stores that ignore keyset cursors.</li>
 *   <li>{@link SessionMode#REDIS}: continuous {@code LRANGE} offset pagination that terminates
 *       only on an empty page (never on a short page) and deduplicates XIDs within the tick.</li>
 * </ul>
 *
 * <p>All storage queries execute under {@link BackgroundDbResourceGuard}, skip XIDs already active
 * in {@link InFlightSessionRegistry}, optionally honor {@code scanQuietWindowMs}, and stop
 * scanning immediately when {@link PhaseTwoPipelineExecutor} signals backpressure
 * ({@link OfferResult#QUEUE_FULL} or {@link OfferResult#NOT_RUNNING}).
 */
public class FallbackRecoveryScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackRecoveryScanner.class);

    private static final long MIN_TICK_DURATION_MS = 1000L;

    private final PipelineConfig config;
    private final Supplier<SessionManager> sessionManagerSupplier;
    private final PhaseTwoPipelineExecutor executor;
    private final BackgroundDbResourceGuard dbResourceGuard;
    private final InFlightSessionRegistry inFlightRegistry;
    private final SessionMode sessionMode;
    private final int pageSize;
    private final long maxTickDurationMs;
    private final LongSupplier wallClockMs;
    private final LongSupplier nanoClock;

    /**
     * Creates a fallback recovery scanner using the system wall-clock and monotonic nano-clock.
     *
     * @param config                 pipeline configuration
     * @param sessionManagerSupplier supplier providing the root {@link SessionManager}
     * @param executor               target phase-two pipeline executor
     * @param dbResourceGuard        background DB concurrency guard protecting store queries
     * @param inFlightRegistry       shared in-flight XID lease registry
     * @param sessionMode            active session store mode ({@link SessionMode#DB} or {@link SessionMode#REDIS})
     * @param pageSize               number of rows requested per page; falls back to
     *                               {@link DefaultValues#DEFAULT_QUERY_LIMIT} if non-positive
     * @param maxTickDurationMs      maximum time budget in milliseconds for one scan tick; falls back
     *                               to {@value #MIN_TICK_DURATION_MS}ms if non-positive
     */
    public FallbackRecoveryScanner(
            PipelineConfig config,
            Supplier<SessionManager> sessionManagerSupplier,
            PhaseTwoPipelineExecutor executor,
            BackgroundDbResourceGuard dbResourceGuard,
            InFlightSessionRegistry inFlightRegistry,
            SessionMode sessionMode,
            int pageSize,
            long maxTickDurationMs) {
        this(
                config,
                sessionManagerSupplier,
                executor,
                dbResourceGuard,
                inFlightRegistry,
                sessionMode,
                pageSize,
                maxTickDurationMs,
                System::currentTimeMillis,
                System::nanoTime);
    }

    /**
     * Creates a fallback recovery scanner with injectable clocks for deterministic unit testing.
     *
     * @param config                 pipeline configuration
     * @param sessionManagerSupplier supplier providing the root {@link SessionManager}
     * @param executor               target phase-two pipeline executor
     * @param dbResourceGuard        background DB concurrency guard protecting store queries
     * @param inFlightRegistry       shared in-flight XID lease registry
     * @param sessionMode            active session store mode ({@link SessionMode#DB} or {@link SessionMode#REDIS})
     * @param pageSize               number of rows requested per page; falls back to
     *                               {@link DefaultValues#DEFAULT_QUERY_LIMIT} if non-positive
     * @param maxTickDurationMs      maximum time budget in milliseconds for one scan tick; falls back
     *                               to {@value #MIN_TICK_DURATION_MS}ms if non-positive
     * @param wallClockMs            wall-clock supplier in epoch milliseconds for quiet-window checks
     * @param nanoClock              monotonic nanosecond clock supplier for tick budget enforcement
     */
    public FallbackRecoveryScanner(
            PipelineConfig config,
            Supplier<SessionManager> sessionManagerSupplier,
            PhaseTwoPipelineExecutor executor,
            BackgroundDbResourceGuard dbResourceGuard,
            InFlightSessionRegistry inFlightRegistry,
            SessionMode sessionMode,
            int pageSize,
            long maxTickDurationMs,
            LongSupplier wallClockMs,
            LongSupplier nanoClock) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.sessionManagerSupplier =
                Objects.requireNonNull(sessionManagerSupplier, "sessionManagerSupplier must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.dbResourceGuard = Objects.requireNonNull(dbResourceGuard, "dbResourceGuard must not be null");
        this.inFlightRegistry = Objects.requireNonNull(inFlightRegistry, "inFlightRegistry must not be null");
        this.sessionMode = Objects.requireNonNull(sessionMode, "sessionMode must not be null");
        if (sessionMode != SessionMode.DB && sessionMode != SessionMode.REDIS) {
            throw new IllegalArgumentException(
                    "FallbackRecoveryScanner supports only DB and REDIS session modes, got " + sessionMode);
        }
        this.pageSize = pageSize > 0 ? pageSize : DefaultValues.DEFAULT_QUERY_LIMIT;
        this.maxTickDurationMs = maxTickDurationMs > 0L ? maxTickDurationMs : MIN_TICK_DURATION_MS;
        this.wallClockMs = Objects.requireNonNull(wallClockMs, "wallClockMs must not be null");
        this.nanoClock = Objects.requireNonNull(nanoClock, "nanoClock must not be null");
    }

    /**
     * Scans {@link GlobalStatus#AsyncCommitting} sessions with eager branch loading
     * ({@code lazyLoadBranch = false}), matching legacy {@code DefaultCoordinator#handleAsyncCommitting()}.
     *
     * @return immutable summary of this scan tick
     */
    public ScanSummary scanAsyncCommitting() {
        return scan(new GlobalStatus[] {GlobalStatus.AsyncCommitting}, false);
    }

    /**
     * Continuously scans sessions matching {@code statuses} within one distributed-lock tick and
     * dispatches eligible sessions into {@link PhaseTwoPipelineExecutor}.
     *
     * @param statuses       target global transaction statuses to scan
     * @param lazyLoadBranch whether branch sessions should be lazy-loaded
     * @return immutable summary of this scan tick
     */
    public ScanSummary scan(GlobalStatus[] statuses, boolean lazyLoadBranch) {
        if (statuses == null || statuses.length == 0) {
            return ScanSummary.empty();
        }
        if (!executor.isRunning()) {
            return new ScanSummary(0, 0, 0, 0, 0, true, false, false);
        }
        SessionManager sessionManager = sessionManagerSupplier.get();
        if (sessionManager == null) {
            LOGGER.warn("Skipping fallback recovery scan because root SessionManager is null");
            return ScanSummary.empty();
        }

        long startNanos = nanoClock.getAsLong();
        long maxDurationNanos = TimeUnit.MILLISECONDS.toNanos(maxTickDurationMs);
        ScanState state = new ScanState();

        if (sessionMode == SessionMode.DB) {
            scanDb(sessionManager, statuses, lazyLoadBranch, startNanos, maxDurationNanos, state);
        } else {
            scanRedis(sessionManager, statuses, lazyLoadBranch, startNanos, maxDurationNanos, state);
        }
        return state.toSummary();
    }

    private void scanDb(
            SessionManager sessionManager,
            GlobalStatus[] statuses,
            boolean lazyLoadBranch,
            long startNanos,
            long maxDurationNanos,
            ScanState state) {
        Date cursorGmtModified = null;
        String cursorXid = null;

        while (true) {
            SessionCondition condition = new SessionCondition(statuses);
            condition.setLazyLoadBranch(lazyLoadBranch);
            condition.setPageSize(pageSize);
            if (cursorGmtModified != null && StringUtils.isNotBlank(cursorXid)) {
                condition.setCursorGmtModified(cursorGmtModified);
                condition.setCursorXid(cursorXid);
            }

            List<GlobalSession> page;
            try {
                page = fetchPageGuarded(sessionManager, condition);
            } catch (InterruptedException e) {
                LOGGER.warn("Fallback recovery DB scan interrupted while awaiting resource guard; aborting tick", e);
                Thread.currentThread().interrupt();
                break;
            }
            if (page == null || page.isEmpty()) {
                break;
            }

            GlobalSession last = page.get(page.size() - 1);
            if (cursorGmtModified != null
                    && last != null
                    && last.getGmtModified() != null
                    && cursorGmtModified.getTime() == last.getGmtModified().getTime()
                    && Objects.equals(cursorXid, last.getXid())) {
                LOGGER.warn(
                        "DB keyset cursor did not advance (cursorGmtModified={}, cursorXid={}); "
                                + "stopping scan tick to prevent infinite loop",
                        cursorGmtModified,
                        cursorXid);
                state.stoppedByCursorGuard = true;
                break;
            }

            state.pagesScanned++;
            dispatchPage(page, null, state);
            if (state.stoppedByBackpressure) {
                break;
            }
            if (page.size() < pageSize) {
                break;
            }
            if (last == null || last.getGmtModified() == null || StringUtils.isBlank(last.getXid())) {
                LOGGER.warn(
                        "DB keyset cursor incomplete on trailing row (last={}); stopping scan tick cleanly",
                        last != null ? last.getXid() : null);
                state.stoppedByCursorGuard = true;
                break;
            }
            cursorGmtModified = last.getGmtModified();
            cursorXid = last.getXid();

            if (!executor.isRunning()) {
                state.stoppedByBackpressure = true;
                break;
            }
            if (hasReachedScanLimit(state.pagesScanned, startNanos, maxDurationNanos)) {
                state.stoppedByLimit = true;
                break;
            }
        }
    }

    private void scanRedis(
            SessionManager sessionManager,
            GlobalStatus[] statuses,
            boolean lazyLoadBranch,
            long startNanos,
            long maxDurationNanos,
            ScanState state) {
        int offset = 0;
        Set<String> seenXids = new HashSet<>();

        while (true) {
            SessionCondition condition = new SessionCondition(statuses);
            condition.setLazyLoadBranch(lazyLoadBranch);
            condition.setOffset(offset);
            condition.setPageSize(pageSize);

            List<GlobalSession> page;
            try {
                page = fetchPageGuarded(sessionManager, condition);
            } catch (InterruptedException e) {
                LOGGER.warn("Fallback recovery Redis scan interrupted while awaiting resource guard; aborting tick", e);
                Thread.currentThread().interrupt();
                break;
            }
            // Redis status lists mutate concurrently and may return short pages; stop only on empty page.
            if (page == null || page.isEmpty()) {
                break;
            }

            state.pagesScanned++;
            dispatchPage(page, seenXids, state);
            if (state.stoppedByBackpressure) {
                break;
            }
            if (!executor.isRunning()) {
                state.stoppedByBackpressure = true;
                break;
            }
            if (hasReachedScanLimit(state.pagesScanned, startNanos, maxDurationNanos)) {
                state.stoppedByLimit = true;
                break;
            }
            if (offset > Integer.MAX_VALUE - pageSize) {
                state.stoppedByLimit = true;
                break;
            }
            offset += pageSize;
        }
    }

    private List<GlobalSession> fetchPageGuarded(SessionManager sessionManager, SessionCondition condition)
            throws InterruptedException {
        return dbResourceGuard.execute(() -> sessionManager.findGlobalSessions(condition));
    }

    private void dispatchPage(List<GlobalSession> page, Set<String> seenXids, ScanState state) {
        long quietWindowMs = config.getScanQuietWindowMs();
        for (GlobalSession session : page) {
            if (session == null) {
                continue;
            }
            state.rowsExamined++;
            String xid = session.getXid();
            if (StringUtils.isBlank(xid)) {
                continue;
            }
            if (seenXids != null && !seenXids.add(xid)) {
                continue;
            }
            if (inFlightRegistry.isInFlight(xid)) {
                state.skippedInFlight++;
                continue;
            }
            if (quietWindowMs > 0L && session.getGmtModified() != null) {
                long ageMs = wallClockMs.getAsLong() - session.getGmtModified().getTime();
                if (ageMs >= 0L && ageMs < quietWindowMs) {
                    state.skippedQuietWindow++;
                    continue;
                }
            }
            OfferResult result = executor.tryOffer(session);
            switch (result) {
                case ACCEPTED:
                    state.dispatched++;
                    break;
                case DUPLICATE:
                    state.skippedInFlight++;
                    break;
                case QUEUE_FULL:
                case NOT_RUNNING:
                    state.stoppedByBackpressure = true;
                    return;
                case INVALID:
                default:
                    break;
            }
        }
    }

    private boolean hasReachedScanLimit(int pagesScanned, long startNanos, long maxDurationNanos) {
        if (pagesScanned >= config.getScanMaxPagesPerTick()) {
            return true;
        }
        return (nanoClock.getAsLong() - startNanos) >= maxDurationNanos;
    }

    public int getPageSize() {
        return pageSize;
    }

    public long getMaxTickDurationMs() {
        return maxTickDurationMs;
    }

    public SessionMode getSessionMode() {
        return sessionMode;
    }

    private static final class ScanState {
        private int pagesScanned;
        private int rowsExamined;
        private int dispatched;
        private int skippedInFlight;
        private int skippedQuietWindow;
        private boolean stoppedByBackpressure;
        private boolean stoppedByLimit;
        private boolean stoppedByCursorGuard;

        private ScanSummary toSummary() {
            return new ScanSummary(
                    pagesScanned,
                    rowsExamined,
                    dispatched,
                    skippedInFlight,
                    skippedQuietWindow,
                    stoppedByBackpressure,
                    stoppedByLimit,
                    stoppedByCursorGuard);
        }
    }

    /**
     * Immutable summary describing the outcome of a single {@link FallbackRecoveryScanner} tick.
     */
    public static final class ScanSummary {

        private final int pagesScanned;
        private final int rowsExamined;
        private final int dispatched;
        private final int skippedInFlight;
        private final int skippedQuietWindow;
        private final boolean stoppedByBackpressure;
        private final boolean stoppedByLimit;
        private final boolean stoppedByCursorGuard;

        public ScanSummary(
                int pagesScanned,
                int rowsExamined,
                int dispatched,
                int skippedInFlight,
                int skippedQuietWindow,
                boolean stoppedByBackpressure,
                boolean stoppedByLimit,
                boolean stoppedByCursorGuard) {
            this.pagesScanned = pagesScanned;
            this.rowsExamined = rowsExamined;
            this.dispatched = dispatched;
            this.skippedInFlight = skippedInFlight;
            this.skippedQuietWindow = skippedQuietWindow;
            this.stoppedByBackpressure = stoppedByBackpressure;
            this.stoppedByLimit = stoppedByLimit;
            this.stoppedByCursorGuard = stoppedByCursorGuard;
        }

        public static ScanSummary empty() {
            return new ScanSummary(0, 0, 0, 0, 0, false, false, false);
        }

        public int getPagesScanned() {
            return pagesScanned;
        }

        public int getRowsExamined() {
            return rowsExamined;
        }

        public int getDispatched() {
            return dispatched;
        }

        public int getSkippedInFlight() {
            return skippedInFlight;
        }

        public int getSkippedQuietWindow() {
            return skippedQuietWindow;
        }

        public boolean isStoppedByBackpressure() {
            return stoppedByBackpressure;
        }

        public boolean isStoppedByLimit() {
            return stoppedByLimit;
        }

        public boolean isStoppedByCursorGuard() {
            return stoppedByCursorGuard;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof ScanSummary)) {
                return false;
            }
            ScanSummary that = (ScanSummary) o;
            return pagesScanned == that.pagesScanned
                    && rowsExamined == that.rowsExamined
                    && dispatched == that.dispatched
                    && skippedInFlight == that.skippedInFlight
                    && skippedQuietWindow == that.skippedQuietWindow
                    && stoppedByBackpressure == that.stoppedByBackpressure
                    && stoppedByLimit == that.stoppedByLimit
                    && stoppedByCursorGuard == that.stoppedByCursorGuard;
        }

        @Override
        public int hashCode() {
            return Objects.hash(
                    pagesScanned,
                    rowsExamined,
                    dispatched,
                    skippedInFlight,
                    skippedQuietWindow,
                    stoppedByBackpressure,
                    stoppedByLimit,
                    stoppedByCursorGuard);
        }

        @Override
        public String toString() {
            return "ScanSummary{"
                    + "pagesScanned=" + pagesScanned
                    + ", rowsExamined=" + rowsExamined
                    + ", dispatched=" + dispatched
                    + ", skippedInFlight=" + skippedInFlight
                    + ", skippedQuietWindow=" + skippedQuietWindow
                    + ", stoppedByBackpressure=" + stoppedByBackpressure
                    + ", stoppedByLimit=" + stoppedByLimit
                    + ", stoppedByCursorGuard=" + stoppedByCursorGuard
                    + '}';
        }
    }
}
