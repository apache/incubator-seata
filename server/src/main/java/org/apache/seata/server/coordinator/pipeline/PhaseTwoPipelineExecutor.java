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
import org.apache.seata.server.session.GlobalSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Bounded, decoupled two-phase cleanup execution engine.
 *
 * <p>Separates concurrency-oriented phase-two RM RPCs (handled by a dedicated {@code recovery-rpc}
 * worker pool with per-RM concurrency limits) from batch-oriented global session row deletion
 * (handed off to {@link BatchGlobalSessionCleaner}).
 *
 * <p>Submissions via {@link #offer(GlobalSession)} are strictly non-blocking: duplicate in-flight
 * XIDs are deduplicated via {@link InFlightSessionRegistry}, and queue overflow immediately
 * releases the lease, increments the drop metric, and returns {@code false} so fallback recovery
 * scanning handles overflow safely without ever blocking foreground Netty threads.
 */
public class PhaseTwoPipelineExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(PhaseTwoPipelineExecutor.class);

    private static final String WORKER_THREAD_PREFIX = "recovery-rpc";
    private static final long WORKER_POLL_INTERVAL_MS = 50L;

    private final GlobalSession shutdownSentinel = new GlobalSession();

    private final PipelineConfig config;
    private final PhaseTwoCommitHandler commitHandler;
    private final BatchGlobalSessionCleaner cleaner;
    private final InFlightSessionRegistry inFlightRegistry;
    private final RmConcurrencyLimiter rmConcurrencyLimiter;
    private final PipelineMetrics metrics;
    private final BlockingQueue<GlobalSession> taskQueue;
    private final ThreadPoolExecutor workerExecutor;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    /**
     * Creates a {@link PhaseTwoPipelineExecutor}.
     *
     * @param config           pipeline configuration
     * @param commitHandler    phase-two commit SPI (must use {@code retrying = true} when delegating to
     *                         {@code DefaultCore#doGlobalCommit})
     * @param cleaner          optional batch cleaner to hand completed sessions to; may be {@code null}
     * @param inFlightRegistry optional in-flight lease registry; a default registry is created if {@code null}
     * @param metrics          optional metrics collector; a local collector is created if {@code null}
     */
    public PhaseTwoPipelineExecutor(
            PipelineConfig config,
            PhaseTwoCommitHandler commitHandler,
            BatchGlobalSessionCleaner cleaner,
            InFlightSessionRegistry inFlightRegistry,
            PipelineMetrics metrics) {
        this.config = Objects.requireNonNull(config, "config must not be null");
        this.commitHandler = Objects.requireNonNull(commitHandler, "commitHandler must not be null");
        this.cleaner = cleaner;
        this.inFlightRegistry =
                inFlightRegistry != null ? inFlightRegistry : new InFlightSessionRegistry(config.getInFlightLeaseMs());
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
     * Starts the {@code recovery-rpc} worker pool and underlying batch cleaner (if configured).
     * Idempotent when called multiple times.
     */
    public void start() {
        ensureStarted();
    }

    private void ensureStarted() {
        if (started.get() || !running.get()) {
            return;
        }
        synchronized (lifecycleLock) {
            if (!running.get() || !started.compareAndSet(false, true)) {
                return;
            }
            if (cleaner != null) {
                cleaner.start();
            }
            int poolSize = config.getRpcPoolSize();
            for (int i = 0; i < poolSize; i++) {
                workerExecutor.execute(this::workerLoop);
            }
        }
    }

    /**
     * Non-blocking submission of a {@link GlobalSession} into the phase-two pipeline.
     *
     * <p>Rejects {@code null}/blank-XID sessions and submissions after shutdown. Deduplicates via
     * {@link InFlightSessionRegistry#tryAcquire(String)} before attempting a non-blocking offer
     * into the bounded task queue; on queue overflow, immediately releases the in-flight lease
     * and increments the dropped counter.
     *
     * @param session global session to process
     * @return {@code true} if accepted into the bounded queue; {@code false} if rejected,
     *         deduplicated, or dropped due to queue overflow
     */
    public boolean offer(GlobalSession session) {
        if (session == null || !running.get()) {
            return false;
        }
        String xid = session.getXid();
        if (xid == null || xid.isEmpty()) {
            return false;
        }
        if (!inFlightRegistry.tryAcquire(xid)) {
            metrics.recordDedupSkipped();
            return false;
        }
        ensureStarted();
        if (!running.get()) {
            inFlightRegistry.release(xid);
            return false;
        }
        if (!taskQueue.offer(session)) {
            inFlightRegistry.release(xid);
            metrics.recordDropped();
            return false;
        }
        if (!running.get() && taskQueue.remove(session)) {
            inFlightRegistry.release(xid);
            return false;
        }
        metrics.recordEnqueued();
        return true;
    }

    private void workerLoop() {
        while (running.get() || !taskQueue.isEmpty()) {
            GlobalSession session;
            try {
                if (running.get()) {
                    session = taskQueue.poll(WORKER_POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
                } else {
                    session = taskQueue.poll();
                }
            } catch (InterruptedException e) {
                if (!running.get()) {
                    session = taskQueue.poll();
                } else {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (session == null) {
                if (!running.get()) {
                    break;
                }
                continue;
            }
            if (session == shutdownSentinel) {
                continue;
            }
            processSession(session);
        }
    }

    private void processSession(GlobalSession session) {
        String xid = session.getXid();
        boolean handedOffToCleaner = false;
        String previousGroup = SeataClusterContext.getGroup();
        String previousMdcXid = MDC.get(RootContext.MDC_KEY_XID);
        RmConcurrencyLimiter.Permit rmPermit = null;
        long startNanos = System.nanoTime();
        try {
            SeataClusterContext.bindGroup();
            if (xid != null) {
                MDC.put(RootContext.MDC_KEY_XID, xid);
            }
            rmPermit = rmConcurrencyLimiter.acquire(session);
            startNanos = System.nanoTime();
            boolean completed = commitHandler.executePhaseTwo(session);
            metrics.recordRpcSuccess(System.nanoTime() - startNanos);
            if (completed && cleaner != null) {
                handedOffToCleaner = cleaner.offer(session);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            metrics.recordRpcFailure(System.nanoTime() - startNanos);
            LOGGER.warn("Phase-two pipeline worker interrupted while processing xid={}", xid, e);
        } catch (Throwable t) {
            metrics.recordRpcFailure(System.nanoTime() - startNanos);
            LOGGER.error("Phase-two pipeline worker failed to process xid={}", xid, t);
        } finally {
            if (rmPermit != null) {
                rmPermit.close();
            }
            if (!handedOffToCleaner) {
                inFlightRegistry.release(xid);
            }
            restoreContext(previousGroup, previousMdcXid);
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
     * Gracefully shuts down the executor: rejects new submissions, drains in-flight workers within
     * {@code timeoutMs}, and then shuts down the {@link BatchGlobalSessionCleaner} so completed
     * sessions are flushed before returning.
     *
     * @param timeoutMs total maximum shutdown wait budget in milliseconds
     * @return {@code true} if both workers and cleaner terminated cleanly within the timeout
     */
    public boolean shutdown(long timeoutMs) {
        long effectiveTimeoutMs = Math.max(0L, timeoutMs);
        long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(effectiveTimeoutMs);
        boolean wasStarted;
        synchronized (lifecycleLock) {
            running.set(false);
            wasStarted = started.get();
        }

        boolean workersTerminated = true;
        if (wasStarted) {
            for (int i = 0; i < config.getRpcPoolSize(); i++) {
                taskQueue.offer(shutdownSentinel);
            }
            workerExecutor.shutdown();
            long remainingNanos = Math.max(0L, deadlineNanos - System.nanoTime());
            try {
                workersTerminated = workerExecutor.awaitTermination(remainingNanos, TimeUnit.NANOSECONDS);
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
        List<GlobalSession> remaining = new ArrayList<>();
        taskQueue.drainTo(remaining);
        for (GlobalSession session : remaining) {
            if (session != null && session != shutdownSentinel) {
                inFlightRegistry.release(session.getXid());
            }
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

    public BatchGlobalSessionCleaner getCleaner() {
        return cleaner;
    }

    public RmConcurrencyLimiter getRmConcurrencyLimiter() {
        return rmConcurrencyLimiter;
    }

    public int getQueueSize() {
        return running.get() ? taskQueue.size() : 0;
    }

    public boolean isRunning() {
        return running.get();
    }

    /**
     * Functional SPI invoked by {@link PhaseTwoPipelineExecutor} workers to execute phase-two
     * branch commits for a {@link GlobalSession}.
     *
     * <p><b>IMPORTANT (Discussion #8211 Section 7.1):</b> Any implementation delegating to
     * {@code DefaultCore#doGlobalCommit(GlobalSession, boolean)} MUST always pass
     * {@code retrying = true}. Passing {@code retrying = false} silently skips branches where
     * {@code canBeCommittedAsync()} returns {@code true}.
     */
    @FunctionalInterface
    public interface PhaseTwoCommitHandler {

        /**
         * Executes phase-two commit logic for the supplied global session.
         *
         * @param session global session to commit
         * @return {@code true} if phase-two branch commits completed and the session should be
         *         handed off to {@link BatchGlobalSessionCleaner} for asynchronous deletion;
         *         {@code false} if retry is still needed or the handler already finalized deletion
         * @throws TransactionException if phase-two execution fails
         */
        boolean executePhaseTwo(GlobalSession session) throws TransactionException;
    }
}
