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

import org.apache.seata.common.thread.ThreadPoolExecutorFactory;
import org.apache.seata.core.context.RootContext;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.coordinator.Core;
import org.apache.seata.server.coordinator.DefaultCore;
import org.apache.seata.server.session.GlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Bounded, decoupled two-phase cleanup execution engine.
 *
 * <p>Separates concurrency-oriented phase-two RM RPCs (handled by a dedicated {@code recovery-rpc}
 * worker pool with non-blocking per-RM concurrency limits) from batch-oriented global session row
 * deletion (handed off to {@link BatchGlobalSessionCleaner}).
 *
 * <p>Lifecycle is explicit: callers must invoke {@link #start()} before calling
 * {@link #tryOffer(GlobalSession)} or {@link #offer(GlobalSession)}. Submissions are strictly
 * non-blocking and never spawn threads on foreground Netty threads: duplicate in-flight XIDs
 * return {@link OfferResult#DUPLICATE}, queue overflow immediately releases the acquired lease and
 * returns {@link OfferResult#QUEUE_FULL} (enabling scanner backpressure), and per-RM throttling
 * releases the lease without blocking worker threads so healthy RMs are never starved.
 */
public class PhaseTwoPipelineExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseTwoPipelineExecutor.class);

    private static final String WORKER_THREAD_PREFIX = "recovery-rpc";
    private static final long WORKER_POLL_INTERVAL_MS = 50L;

    private enum State {
        NEW,
        RUNNING,
        STOPPED
    }

    private final PipelineConfig config;
    private final PhaseTwoCommitHandler commitHandler;
    private final BatchGlobalSessionCleaner cleaner;
    private final BackgroundDbResourceGuard dbResourceGuard;
    private final InFlightSessionRegistry inFlightRegistry;
    private final RmConcurrencyLimiter rmConcurrencyLimiter;
    private final PipelineMetrics metrics;
    private final BlockingQueue<QueuedSession> taskQueue;
    private final AtomicInteger queuedCount = new AtomicInteger(0);
    private final ThreadPoolExecutor workerExecutor;

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    private final Object lifecycleLock = new Object();

    /**
     * Creates a {@link PhaseTwoPipelineExecutor}. Call {@link #start()} before submitting tasks.
     *
     * <p>When {@code cleaner} is non-null, {@code cleaner} MUST have a non-null
     * {@link InFlightSessionRegistry}, and if {@code inFlightRegistry} is also passed, both MUST
     * reference the exact same {@link InFlightSessionRegistry} instance (otherwise
     * {@link IllegalArgumentException} is thrown) so handoff lease ownership cannot diverge.
     *
     * @param config           pipeline configuration
     * @param commitHandler    phase-two commit SPI (must use {@code retrying = true} when delegating to
     *                         {@code Core#doGlobalCommit}; see {@link PhaseTwoCommitHandler#fromCore(Core)})
     * @param cleaner          optional batch cleaner to hand completed sessions to; may be {@code null}
     * @param inFlightRegistry optional in-flight lease registry; if {@code null} and {@code cleaner}
     *                         is present, reuses {@code cleaner.getInFlightRegistry()}; otherwise a
     *                         new registry is created
     * @param metrics          optional metrics collector; a local collector is created if {@code null}
     * @throws IllegalArgumentException if {@code cleaner} has a null or mismatched
     *                                  {@link InFlightSessionRegistry}
     */
    public PhaseTwoPipelineExecutor(
            PipelineConfig config,
            PhaseTwoCommitHandler commitHandler,
            BatchGlobalSessionCleaner cleaner,
            InFlightSessionRegistry inFlightRegistry,
            PipelineMetrics metrics) {
        this(config, commitHandler, cleaner, inFlightRegistry, null, metrics);
    }

    /**
     * Creates a {@link PhaseTwoPipelineExecutor} with an explicitly shared
     * {@link BackgroundDbResourceGuard} (useful when {@code cleaner} is {@code null} and Wave 2
     * scanners want to obtain the guard via {@link #getDbResourceGuard()}).
     *
     * @param config           pipeline configuration
     * @param commitHandler    phase-two commit SPI
     * @param cleaner          optional batch cleaner
     * @param inFlightRegistry optional shared in-flight registry
     * @param dbResourceGuard  optional shared background DB guard
     * @param metrics          optional metrics collector
     */
    public PhaseTwoPipelineExecutor(
            PipelineConfig config,
            PhaseTwoCommitHandler commitHandler,
            BatchGlobalSessionCleaner cleaner,
            InFlightSessionRegistry inFlightRegistry,
            BackgroundDbResourceGuard dbResourceGuard,
            PipelineMetrics metrics) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.commitHandler = Objects.requireNonNull(commitHandler, "commitHandler must not be null");
        this.cleaner = cleaner;
        if (cleaner != null) {
            InFlightSessionRegistry cleanerRegistry = cleaner.getInFlightRegistry();
            if (cleanerRegistry == null) {
                throw new IllegalArgumentException(
                        "cleaner must be configured with a non-null InFlightSessionRegistry shared with the executor");
            }
            if (inFlightRegistry != null && cleanerRegistry != inFlightRegistry) {
                throw new IllegalArgumentException(
                        "cleaner and PhaseTwoPipelineExecutor must share the exact same InFlightSessionRegistry instance");
            }
            if (dbResourceGuard != null && cleaner.getDbResourceGuard() != dbResourceGuard) {
                throw new IllegalArgumentException(
                        "cleaner and PhaseTwoPipelineExecutor must share the exact same BackgroundDbResourceGuard instance");
            }
            this.inFlightRegistry = cleanerRegistry;
            this.dbResourceGuard = cleaner.getDbResourceGuard();
        } else {
            this.inFlightRegistry = inFlightRegistry != null
                    ? inFlightRegistry
                    : new InFlightSessionRegistry(config.getInFlightLeaseMs());
            this.dbResourceGuard = dbResourceGuard;
        }
        this.rmConcurrencyLimiter = new RmConcurrencyLimiter(config.getMaxConcurrentPerRm());
        this.metrics = metrics != null ? metrics : new PipelineMetrics();
        this.taskQueue = new ArrayBlockingQueue<>(config.getQueueCapacity());
        int poolSize = config.getRpcPoolSize();
        this.workerExecutor = ThreadPoolExecutorFactory.newThreadPoolExecutor(
                WORKER_THREAD_PREFIX,
                poolSize,
                poolSize,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(poolSize),
                true);
        this.metrics.bindQueueDepthSupplier(this::getQueueSize);
        this.metrics.bindInFlightCountSupplier(this.inFlightRegistry::size);
    }

    /**
     * Explicitly starts the {@code recovery-rpc} worker pool and underlying batch cleaner
     * (if configured). Idempotent when already running; no-op if already shut down.
     */
    public void start() {
        if (state.get() != State.NEW) {
            return;
        }
        synchronized (lifecycleLock) {
            if (state.get() != State.NEW) {
                return;
            }
            if (cleaner != null) {
                cleaner.start();
            }
            state.set(State.RUNNING);
            int poolSize = config.getRpcPoolSize();
            for (int i = 0; i < poolSize; i++) {
                workerExecutor.execute(this::workerLoop);
            }
        }
    }

    /**
     * Non-blocking submission returning a detailed {@link OfferResult} suitable for caller
     * backpressure decisions.
     *
     * <p>Note: returning {@link OfferResult#ACCEPTED} confirms the session was placed into the
     * bounded queue, but does not guarantee execution if {@link #shutdown(long)} runs concurrently
     * before a worker dequeues it (in which case shutdown releases its lease for fallback recovery).
     *
     * @param session global session to process
     * @return explicit {@link OfferResult}
     */
    public OfferResult tryOffer(GlobalSession session) {
        if (session == null) {
            return OfferResult.INVALID;
        }
        String xid = session.getXid();
        if (xid == null || xid.isEmpty()) {
            return OfferResult.INVALID;
        }
        if (state.get() != State.RUNNING) {
            return OfferResult.NOT_RUNNING;
        }
        long leaseToken = inFlightRegistry.tryAcquireQueued(xid);
        if (leaseToken == InFlightSessionRegistry.INVALID_TOKEN) {
            metrics.recordDedupSkipped();
            return OfferResult.DUPLICATE;
        }
        if (state.get() != State.RUNNING) {
            inFlightRegistry.release(xid, leaseToken);
            return OfferResult.NOT_RUNNING;
        }
        QueuedSession envelope = QueuedSession.of(session, leaseToken);
        if (!taskQueue.offer(envelope)) {
            inFlightRegistry.release(xid, leaseToken);
            metrics.recordDropped();
            return OfferResult.QUEUE_FULL;
        }
        queuedCount.incrementAndGet();
        if (state.get() != State.RUNNING && taskQueue.remove(envelope)) {
            queuedCount.decrementAndGet();
            inFlightRegistry.release(xid, leaseToken);
            return OfferResult.NOT_RUNNING;
        }
        metrics.recordEnqueued();
        return OfferResult.ACCEPTED;
    }

    /**
     * Convenience wrapper over {@link #tryOffer(GlobalSession)} returning {@code true} iff
     * the result is {@link OfferResult#ACCEPTED}.
     *
     * <p>Returning {@code true} confirms queue acceptance but does not guarantee execution if
     * {@link #shutdown(long)} occurs concurrently before dequeue.
     *
     * @param session global session to process
     * @return {@code true} if accepted into the bounded queue; {@code false} otherwise
     */
    public boolean offer(GlobalSession session) {
        return tryOffer(session) == OfferResult.ACCEPTED;
    }

    private void workerLoop() {
        while (state.get() == State.RUNNING) {
            QueuedSession envelope;
            try {
                envelope = taskQueue.poll(WORKER_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                if (state.get() == State.RUNNING) {
                    LOGGER.warn("Phase-two pipeline worker interrupted while polling; continuing loop");
                    continue;
                }
                break;
            }

            if (envelope == null) {
                inFlightRegistry.maybeEvictExpired();
                continue;
            }
            if (envelope.isSentinel()) {
                continue;
            }
            queuedCount.decrementAndGet();
            if (state.get() != State.RUNNING) {
                releaseEnvelopeLease(envelope);
                break;
            }
            processSession(envelope);
            inFlightRegistry.maybeEvictExpired();
        }
    }

    private void processSession(QueuedSession envelope) {
        GlobalSession session = envelope.getSession();
        String xid = session.getXid();
        long leaseToken = envelope.getLeaseToken();

        // Activate execution TTL upon dequeue so queue wait time does not consume execution lease.
        if (!inFlightRegistry.renew(xid, leaseToken)) {
            LOGGER.warn(
                    "Skipping phase-two execution for xid={} because lease token={} is no longer valid",
                    xid,
                    leaseToken);
            return;
        }

        boolean handedOffToCleaner = false;
        String previousGroup = SeataClusterContext.getGroup();
        String previousMdcXid = MDC.get(RootContext.MDC_KEY_XID);
        RmConcurrencyLimiter.Permit rmPermit = null;
        long startNanos = System.nanoTime();
        try {
            Optional<RmConcurrencyLimiter.Permit> permitOpt = rmConcurrencyLimiter.tryAcquire(session);
            if (!permitOpt.isPresent()) {
                metrics.recordRmThrottled();
                return;
            }
            rmPermit = permitOpt.get();

            SeataClusterContext.bindGroup();
            if (xid != null) {
                MDC.put(RootContext.MDC_KEY_XID, xid);
            }
            startNanos = System.nanoTime();
            boolean completed = commitHandler.executePhaseTwo(session);
            long elapsedNanos = System.nanoTime() - startNanos;
            if (completed) {
                metrics.recordRpcCompleted(elapsedNanos);
                if (cleaner != null) {
                    // Refresh lease TTL before cleaner handoff so batching window has full lease budget.
                    if (inFlightRegistry.renew(xid, leaseToken)) {
                        handedOffToCleaner = cleaner.offer(session, leaseToken);
                    }
                }
            } else {
                metrics.recordRpcRetryNeeded(elapsedNanos);
            }
        } catch (Throwable t) {
            metrics.recordRpcFailure(System.nanoTime() - startNanos);
            LOGGER.error("Phase-two pipeline worker failed to process xid={}", xid, t);
        } finally {
            if (rmPermit != null) {
                rmPermit.close();
            }
            if (!handedOffToCleaner) {
                inFlightRegistry.release(xid, leaseToken);
            }
            restoreContext(previousGroup, previousMdcXid);
            if (state.get() == State.RUNNING && Thread.interrupted()) {
                LOGGER.warn("Cleared stray interrupt flag on worker thread after processing xid={}", xid);
            }
        }
    }

    private void releaseEnvelopeLease(QueuedSession envelope) {
        if (envelope != null && !envelope.isSentinel() && envelope.getSession() != null) {
            inFlightRegistry.release(envelope.getSession().getXid(), envelope.getLeaseToken());
        }
    }

    private static void restoreContext(String previousGroup, String previousMdcXid) {
        if (previousGroup == null) {
            SeataClusterContext.unbindGroup();
        } else {
            SeataClusterContext.bindGroup(previousGroup);
        }
        if (previousMdcXid == null) {
            MDC.remove(RootContext.MDC_KEY_XID);
        } else {
            MDC.put(RootContext.MDC_KEY_XID, previousMdcXid);
        }
    }

    /**
     * Gracefully shuts down the executor:
     * <ol>
     *   <li>Stops accepting new submissions.</li>
     *   <li>Immediately drains and releases leases for queued (not-yet-started) sessions so they
     *       return to fallback recovery without consuming shutdown budget.</li>
     *   <li>Waits for currently executing in-flight workers to complete (reserving at least 20% of
     *       positive timeout budget for the batch cleaner when configured).</li>
     *   <li>Shuts down {@link BatchGlobalSessionCleaner} with the remaining budget so completed
     *       sessions are flushed before returning.</li>
     * </ol>
     *
     * @param timeoutMs total maximum shutdown wait budget in milliseconds
     * @return {@code true} if both workers and cleaner terminated cleanly within the timeout
     */
    public boolean shutdown(long timeoutMs) {
        long effectiveTimeoutMs = Math.max(0L, timeoutMs);
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeoutMs);
        boolean wasRunning;
        synchronized (lifecycleLock) {
            wasRunning = state.getAndSet(State.STOPPED) == State.RUNNING;
        }

        // Release not-yet-started queued sessions immediately so workers only finish in-flight RPCs.
        releaseRemainingQueuedSessions();

        boolean workersTerminated = true;
        if (wasRunning) {
            for (int i = 0; i < config.getRpcPoolSize(); i++) {
                taskQueue.offer(QueuedSession.SHUTDOWN_SENTINEL);
            }
            workerExecutor.shutdown();
            long remainingTotalNanos = Math.max(0L, deadlineNanos - System.nanoTime());
            long workerWaitNanos = cleaner != null && remainingTotalNanos > 0L
                    ? Math.max(1L, (remainingTotalNanos * 4L) / 5L)
                    : remainingTotalNanos;
            try {
                workersTerminated = workerExecutor.awaitTermination(workerWaitNanos, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                workersTerminated = false;
            }
            if (!workersTerminated) {
                workerExecutor.shutdownNow();
            }
            releaseRemainingQueuedSessions();
        } else {
            workerExecutor.shutdown();
            releaseRemainingQueuedSessions();
        }

        boolean cleanerTerminated = true;
        if (cleaner != null) {
            long remainingMs = Math.max(0L, TimeUnit.NANOSECONDS.toMillis(deadlineNanos - System.nanoTime()));
            cleanerTerminated = cleaner.shutdown(remainingMs);
        }
        return workersTerminated && cleanerTerminated;
    }

    private void releaseRemainingQueuedSessions() {
        List<QueuedSession> remaining = new ArrayList<>();
        taskQueue.drainTo(remaining);
        int removedReal = 0;
        for (QueuedSession envelope : remaining) {
            if (!envelope.isSentinel()) {
                removedReal++;
                releaseEnvelopeLease(envelope);
            }
        }
        if (removedReal > 0) {
            queuedCount.addAndGet(-removedReal);
        }
    }

    public PipelineConfig getConfig() {
        return config;
    }

    public PipelineMetrics getMetrics() {
        return metrics;
    }

    public InFlightSessionRegistry getInFlightRegistry() {
        return inFlightRegistry;
    }

    public BackgroundDbResourceGuard getDbResourceGuard() {
        return dbResourceGuard;
    }

    public BatchGlobalSessionCleaner getCleaner() {
        return cleaner;
    }

    public RmConcurrencyLimiter getRmConcurrencyLimiter() {
        return rmConcurrencyLimiter;
    }

    /**
     * Returns the current number of real queued sessions awaiting worker pickup (including during
     * shutdown draining).
     *
     * @return queued session count
     */
    public int getQueueSize() {
        return Math.max(0, queuedCount.get());
    }

    /**
     * Returns the remaining free slots in the bounded task queue, useful for scanner backpressure.
     *
     * @return remaining queue capacity
     */
    public int remainingCapacity() {
        return taskQueue.remainingCapacity();
    }

    public boolean isRunning() {
        return state.get() == State.RUNNING;
    }

    /**
     * Functional SPI invoked by {@link PhaseTwoPipelineExecutor} workers to execute phase-two
     * branch commits for a {@link GlobalSession}.
     *
     * <p><b>IMPORTANT (Discussion #8211 Section 7.1):</b> Any implementation delegating to
     * {@code Core#doGlobalCommit(GlobalSession, boolean)} MUST always pass {@code retrying = true}.
     * Passing {@code retrying = false} silently skips branches where {@code canBeCommittedAsync()}
     * returns {@code true}. Use {@link #fromCore(Core)} for the standard {@code retrying = true}
     * delegation.
     *
     * <p><b>Return value contract:</b>
     * <ul>
     *   <li>Return {@code true} ONLY when all phase-two branch commits have succeeded AND the
     *       handler has already completed global lock cleanup and completion metrics/events,
     *       leaving ONLY asynchronous batch deletion of the global session row for
     *       {@link BatchGlobalSessionCleaner}.</li>
     *   <li>Return {@code false} if the session still requires retry OR if the handler has
     *       already removed the global session row inline (for example via
     *       {@link #fromCore(Core)}).</li>
     * </ul>
     */
    @FunctionalInterface
    public interface PhaseTwoCommitHandler {

        /**
         * Executes phase-two commit logic for the supplied global session.
         *
         * @param session global session to commit
         * @return {@code true} if the session is ready to be handed off to
         *         {@link BatchGlobalSessionCleaner} for asynchronous global-row deletion;
         *         {@code false} if retry is still needed or the handler already finalized deletion
         * @throws TransactionException if phase-two execution fails
         */
        boolean executePhaseTwo(GlobalSession session) throws TransactionException;

        /**
         * Official adapter delegating to {@link Core#doGlobalCommit(GlobalSession, boolean)}
         * with {@code retrying = true} enforced (Discussion #8211 Section 7.1).
         *
         * <p>Because {@code DefaultCore#doGlobalCommit(session, true)} currently finalizes and
         * deletes the global session inline via {@code SessionHelper.endCommitted(session, true)},
         * this adapter returns {@code false} so {@link BatchGlobalSessionCleaner} does not issue a
         * duplicate delete. Use {@link #fromCoreForPipeline(DefaultCore)} to defer global-row
         * deletion to the batch cleaner.
         *
         * @param core transaction core
         * @return commit handler enforcing {@code retrying = true}
         */
        static PhaseTwoCommitHandler fromCore(Core core) {
            Objects.requireNonNull(core, "core must not be null");
            return session -> {
                core.doGlobalCommit(session, true);
                return false;
            };
        }

        /**
         * Pipeline-aware adapter delegating to
         * {@link DefaultCore#doGlobalCommitForPipeline(GlobalSession)}, enforcing
         * {@code retrying = true}, isolating Saga sessions, eliminating the redundant
         * {@code Committed} status update, and deferring global-row deletion to
         * {@link BatchGlobalSessionCleaner}.
         *
         * @param core transaction core
         * @return commit handler returning {@code true} only when ready for batch deletion
         */
        static PhaseTwoCommitHandler fromCoreForPipeline(DefaultCore core) {
            Objects.requireNonNull(core, "core must not be null");
            return core::doGlobalCommitForPipeline;
        }
    }
}
