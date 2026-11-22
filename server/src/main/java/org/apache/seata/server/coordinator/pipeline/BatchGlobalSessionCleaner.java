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
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Single-writer asynchronous batch cleaner for completed {@link GlobalSession} rows.
 *
 * <p>Lifecycle is explicit: callers must invoke {@link #start()} before submitting sessions via
 * {@link #offer(GlobalSession)} or {@link #offer(GlobalSession, long)}. Submissions before
 * {@link #start()} or after {@link #shutdown(long)} return {@code false} without lazily spawning
 * threads on caller threads.
 *
 * <p>Drains a bounded completion buffer into batches triggered either when {@code batchSize}
 * sessions accumulate or when {@code flushIntervalMs} elapses after the first session in a batch,
 * then executes {@link BatchDeleteHandler#deleteBatch(List)} under a
 * {@link BackgroundDbResourceGuard}. Each enqueued session carries its owning lease token so
 * lease releases in {@link InFlightSessionRegistry} are strictly owner-checked.
 *
 * <p>When a batch delete fails, the cleaner logs the error, increments failure metrics, attempts
 * safe per-item fallback deletion (when the batch has more than one item), and always releases
 * owning in-flight leases so the fallback recovery scanner can retry any remaining sessions without
 * killing the cleaner thread.
 */
public class BatchGlobalSessionCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchGlobalSessionCleaner.class);

    private static final String CLEANER_THREAD_PREFIX = "recovery-cleaner";

    private enum State {
        NEW,
        RUNNING,
        STOPPED
    }

    private final int batchSize;
    private final long flushIntervalMs;
    private final long idlePollMs;
    private final BlockingQueue<QueuedSession> buffer;
    private final AtomicInteger bufferedCount = new AtomicInteger(0);
    private final BatchDeleteHandler deleteHandler;
    private final BackgroundDbResourceGuard dbResourceGuard;
    private final InFlightSessionRegistry inFlightRegistry;
    private final PipelineMetrics metrics;
    private final ThreadFactory threadFactory;

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);
    private final Object lifecycleLock = new Object();
    private volatile Thread cleanerThread;

    /**
     * Creates a batch cleaner configured from {@link PipelineConfig}.
     * Call {@link #start()} before submitting sessions.
     *
     * @param config           pipeline configuration
     * @param deleteHandler    storage deletion handler
     * @param dbResourceGuard  background DB concurrency guard
     * @param inFlightRegistry optional in-flight lease registry; leases are released after each flush
     *                         using each entry's owning token
     * @param metrics          optional metrics collector
     */
    public BatchGlobalSessionCleaner(
            PipelineConfig config,
            BatchDeleteHandler deleteHandler,
            BackgroundDbResourceGuard dbResourceGuard,
            InFlightSessionRegistry inFlightRegistry,
            PipelineMetrics metrics) {
        Objects.requireNonNull(config, "config must not be null");
        this.batchSize = config.getCleanerBatchSize();
        this.flushIntervalMs = config.getCleanerFlushIntervalMs();
        this.idlePollMs = Math.min(Math.max(this.flushIntervalMs, 50L), 1000L);
        this.buffer = new ArrayBlockingQueue<>(config.getCleanerBufferCapacity());
        this.deleteHandler = Objects.requireNonNull(deleteHandler, "deleteHandler must not be null");
        this.dbResourceGuard = Objects.requireNonNull(dbResourceGuard, "dbResourceGuard must not be null");
        this.inFlightRegistry = inFlightRegistry;
        this.metrics = metrics != null ? metrics : new PipelineMetrics();
        this.threadFactory = ThreadPoolExecutorFactory.newThreadFactory(CLEANER_THREAD_PREFIX, 1, true);
        this.metrics.bindCleanerBufferDepthSupplier(this::getBufferSize);
    }

    /**
     * Explicitly starts the single-writer cleaner daemon thread.
     * Idempotent when already running; no-op if already shut down.
     */
    public void start() {
        if (state.get() != State.NEW) {
            return;
        }
        synchronized (lifecycleLock) {
            if (state.get() != State.NEW) {
                return;
            }
            Thread thread = threadFactory.newThread(this::runLoop);
            this.cleanerThread = thread;
            state.set(State.RUNNING);
            thread.start();
        }
    }

    /**
     * Offers a completed {@link GlobalSession} (without an owning lease token) to the bounded
     * completion buffer without blocking.
     *
     * @param session completed global session
     * @return {@code true} if accepted into the buffer; {@code false} if rejected, not running,
     *         cleaner thread is not alive, or buffer is full
     */
    public boolean offer(GlobalSession session) {
        return offer(session, InFlightSessionRegistry.INVALID_TOKEN);
    }

    /**
     * Offers a completed {@link GlobalSession} along with its owning lease token to the bounded
     * completion buffer without blocking.
     *
     * <p>If the buffer is full, increments the cleaner drop counter and returns {@code false}
     * so callers release the in-flight lease with their token and let fallback scanning clean
     * the row later.
     *
     * @param session    completed global session
     * @param leaseToken owning lease token from {@link InFlightSessionRegistry}, or
     *                   {@link InFlightSessionRegistry#INVALID_TOKEN} if unmanaged
     * @return {@code true} if accepted into the buffer; {@code false} if rejected, not running,
     *         cleaner thread is not alive, or buffer is full
     */
    public boolean offer(GlobalSession session, long leaseToken) {
        if (session == null || !isAliveAndRunning()) {
            return false;
        }
        QueuedSession envelope = QueuedSession.of(session, leaseToken);
        boolean accepted = buffer.offer(envelope);
        if (!accepted) {
            metrics.recordCleanerDropped();
            return false;
        }
        bufferedCount.incrementAndGet();
        if (state.get() != State.RUNNING && buffer.remove(envelope)) {
            bufferedCount.decrementAndGet();
            return false;
        }
        return true;
    }

    private boolean isAliveAndRunning() {
        if (state.get() != State.RUNNING) {
            return false;
        }
        Thread thread = this.cleanerThread;
        return thread != null && thread.isAlive();
    }

    private void runLoop() {
        List<QueuedSession> batch = new ArrayList<>(batchSize);
        while (state.get() == State.RUNNING || !buffer.isEmpty()) {
            if (state.get() != State.RUNNING && Thread.currentThread().isInterrupted()) {
                releaseBufferedSessions();
                break;
            }
            batch.clear();
            QueuedSession first;
            try {
                if (state.get() == State.RUNNING) {
                    first = buffer.poll(idlePollMs, TimeUnit.MILLISECONDS);
                } else {
                    first = buffer.poll();
                }
            } catch (InterruptedException e) {
                if (state.get() == State.RUNNING) {
                    LOGGER.warn("Batch global session cleaner thread interrupted while running; continuing loop");
                    continue;
                }
                Thread.currentThread().interrupt();
                releaseBufferedSessions();
                break;
            }

            if (first == null) {
                if (state.get() != State.RUNNING) {
                    break;
                }
                if (inFlightRegistry != null) {
                    inFlightRegistry.maybeEvictExpired();
                }
                continue;
            }

            boolean sawShutdownSentinel = false;
            if (first.isSentinel()) {
                sawShutdownSentinel = true;
            } else {
                bufferedCount.decrementAndGet();
                batch.add(first);
            }

            if (batch.size() < batchSize) {
                sawShutdownSentinel |= drainExcludingSentinel(batch, batchSize - batch.size());
            }

            if (!batch.isEmpty() && batch.size() < batchSize && state.get() == State.RUNNING && !sawShutdownSentinel) {
                long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushIntervalMs);
                while (batch.size() < batchSize && state.get() == State.RUNNING) {
                    long remainingNanos = deadlineNanos - System.nanoTime();
                    if (remainingNanos <= 0L) {
                        break;
                    }
                    try {
                        QueuedSession next = buffer.poll(remainingNanos, TimeUnit.NANOSECONDS);
                        if (next == null) {
                            break;
                        }
                        if (next.isSentinel()) {
                            break;
                        }
                        bufferedCount.decrementAndGet();
                        batch.add(next);
                        if (batch.size() < batchSize) {
                            if (drainExcludingSentinel(batch, batchSize - batch.size())) {
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        if (state.get() == State.RUNNING) {
                            LOGGER.warn(
                                    "Batch global session cleaner interrupted during flush window; flushing partial batch");
                            break;
                        }
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            if (!batch.isEmpty()) {
                if (state.get() != State.RUNNING && Thread.currentThread().isInterrupted()) {
                    releaseLeases(batch);
                    releaseBufferedSessions();
                    break;
                }
                flushBatch(batch);
            }
            if (inFlightRegistry != null) {
                inFlightRegistry.maybeEvictExpired();
            }
        }
    }

    private boolean drainExcludingSentinel(List<QueuedSession> target, int maxElements) {
        if (maxElements <= 0) {
            return false;
        }
        List<QueuedSession> drained = new ArrayList<>(Math.min(maxElements, 64));
        int count = buffer.drainTo(drained, maxElements);
        if (count == 0) {
            return false;
        }
        boolean sawSentinel = false;
        for (QueuedSession envelope : drained) {
            if (envelope.isSentinel()) {
                sawSentinel = true;
            } else {
                bufferedCount.decrementAndGet();
                target.add(envelope);
            }
        }
        return sawSentinel;
    }

    private void flushBatch(List<QueuedSession> batch) {
        List<GlobalSession> sessions = extractSessions(batch);
        long startNanos = System.nanoTime();
        try {
            dbResourceGuard.run(() -> deleteHandler.deleteBatch(Collections.unmodifiableList(sessions)));
            metrics.recordCleanerBatch(sessions.size(), System.nanoTime() - startNanos);
        } catch (InterruptedException e) {
            metrics.recordCleanerBatchFailure();
            if (state.get() != State.RUNNING) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("Batch global session cleaner interrupted while flushing {} sessions", sessions.size(), e);
        } catch (Throwable t) {
            metrics.recordCleanerBatchFailure();
            LOGGER.error(
                    "Batch global session delete failed for {} sessions, attempting fallback recovery",
                    sessions.size(),
                    t);
            if (sessions.size() > 1) {
                fallbackDeleteIndividually(sessions);
            }
        } finally {
            releaseLeases(batch);
            if (state.get() == State.RUNNING && Thread.interrupted()) {
                LOGGER.warn("Cleared stray interrupt flag on batch cleaner thread after flushing batch");
            }
        }
    }

    private void fallbackDeleteIndividually(List<GlobalSession> sessions) {
        int recovered = 0;
        for (GlobalSession session : sessions) {
            if (state.get() != State.RUNNING && Thread.currentThread().isInterrupted()) {
                break;
            }
            try {
                dbResourceGuard.run(() -> deleteHandler.deleteBatch(Collections.singletonList(session)));
                recovered++;
            } catch (InterruptedException e) {
                if (state.get() != State.RUNNING) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn(
                            "Batch global session cleaner interrupted during single-item fallback for xid={}",
                            session.getXid(),
                            e);
                    break;
                }
                LOGGER.warn(
                        "Batch global session cleaner interrupted during single-item fallback for xid={} while running; continuing",
                        session.getXid(),
                        e);
            } catch (Throwable ex) {
                LOGGER.error(
                        "Single-item fallback delete failed for xid={}; fallback scanner will retry",
                        session.getXid(),
                        ex);
            }
        }
        if (recovered > 0) {
            metrics.recordCleanerDeletedSessions(recovered);
        }
    }

    private static List<GlobalSession> extractSessions(List<QueuedSession> batch) {
        List<GlobalSession> sessions = new ArrayList<>(batch.size());
        for (QueuedSession envelope : batch) {
            sessions.add(envelope.getSession());
        }
        return sessions;
    }

    private void releaseLeases(List<QueuedSession> batch) {
        if (inFlightRegistry == null) {
            return;
        }
        for (QueuedSession envelope : batch) {
            if (envelope != null && !envelope.isSentinel()) {
                long token = envelope.getLeaseToken();
                GlobalSession session = envelope.getSession();
                if (session != null && token != InFlightSessionRegistry.INVALID_TOKEN) {
                    inFlightRegistry.release(session.getXid(), token);
                }
            }
        }
    }

    /**
     * Gracefully shuts down the cleaner, stopping new submissions and flushing all remaining
     * buffered sessions within {@code timeoutMs}.
     *
     * <p>If the cleaner thread does not finish within {@code timeoutMs}, it is interrupted so it
     * stops issuing new DB calls after timeout and releases remaining in-flight leases.
     *
     * @param timeoutMs maximum time in milliseconds to wait for drain and flush completion
     * @return {@code true} if the cleaner thread exited cleanly within the timeout
     */
    public boolean shutdown(long timeoutMs) {
        Thread thread;
        synchronized (lifecycleLock) {
            state.set(State.STOPPED);
            thread = this.cleanerThread;
        }
        if (thread == null) {
            releaseBufferedSessions();
            return true;
        }
        buffer.offer(QueuedSession.SHUTDOWN_SENTINEL);
        long effectiveTimeoutMs = Math.max(0L, timeoutMs);
        if (effectiveTimeoutMs > 0L) {
            try {
                thread.join(effectiveTimeoutMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        if (thread.isAlive()) {
            thread.interrupt();
            releaseBufferedSessions();
            return false;
        }
        releaseBufferedSessions();
        return true;
    }

    private void releaseBufferedSessions() {
        List<QueuedSession> remaining = new ArrayList<>();
        buffer.drainTo(remaining);
        int removedReal = 0;
        for (QueuedSession envelope : remaining) {
            if (!envelope.isSentinel()) {
                removedReal++;
            }
        }
        if (removedReal > 0) {
            bufferedCount.addAndGet(-removedReal);
        }
        releaseLeases(remaining);
    }

    /**
     * Returns the current number of real buffered sessions awaiting flush (including during
     * graceful shutdown draining).
     *
     * @return buffered session count
     */
    public int getBufferSize() {
        return Math.max(0, bufferedCount.get());
    }

    public boolean isRunning() {
        return state.get() == State.RUNNING;
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

    /**
     * Storage SPI invoked by {@link BatchGlobalSessionCleaner} to delete a batch of completed
     * {@link GlobalSession} records.
     *
     * <p>Implementations should treat deleting an already-removed row as idempotent success so
     * concurrent recovery or per-item fallback retries do not generate spurious errors.
     */
    @FunctionalInterface
    public interface BatchDeleteHandler {

        /**
         * Deletes the given batch of global sessions from the underlying store.
         *
         * @param sessions non-empty batch of sessions to remove
         * @throws TransactionException if deletion fails
         */
        void deleteBatch(List<GlobalSession> sessions) throws TransactionException;

        /**
         * Adapts an existing {@link SessionManager} by invoking
         * {@link SessionManager#removeGlobalSession(GlobalSession)} per item so Wave 1 compiles
         * independently of batch storage APIs.
         *
         * <p><b>Scope & Wave 2 note:</b> Intended strictly for {@code db} and {@code redis} store
         * modes (must NOT be used with {@code file} or {@code raft} session managers). Wave 2
         * (#8358) should wire the batch deletion API ({@code SessionManager#removeGlobalSessions}
         * from #8354 / PR #8361) directly so deletions execute in a single batch statement and
         * treat already-deleted rows idempotently.
         *
         * @param sessionManager target {@code db}/{@code redis} session manager
         * @return batch delete handler delegating per session
         */
        static BatchDeleteHandler fromSessionManager(SessionManager sessionManager) {
            Objects.requireNonNull(sessionManager, "sessionManager must not be null");
            return sessions -> {
                if (sessions == null || sessions.isEmpty()) {
                    return;
                }
                for (GlobalSession session : sessions) {
                    sessionManager.removeGlobalSession(session);
                }
            };
        }
    }
}
