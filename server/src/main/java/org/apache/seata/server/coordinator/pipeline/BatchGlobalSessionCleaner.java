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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Single-writer asynchronous batch cleaner for completed {@link GlobalSession} rows.
 *
 * <p>Drains a bounded completion buffer into batches triggered either when {@code batchSize}
 * sessions accumulate or when {@code flushIntervalMs} elapses after the first session in a batch,
 * then executes {@link BatchDeleteHandler#deleteBatch(List)} under a
 * {@link BackgroundDbResourceGuard}.
 *
 * <p>When a batch delete fails, the cleaner logs the error, increments failure metrics, attempts
 * safe per-item fallback deletion (when the batch has more than one item), and always releases
 * in-flight leases so the fallback recovery scanner can retry any remaining sessions without
 * killing the cleaner thread.
 */
public class BatchGlobalSessionCleaner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BatchGlobalSessionCleaner.class);

    private static final String CLEANER_THREAD_PREFIX = "recovery-cleaner";

    private final GlobalSession shutdownSentinel = new GlobalSession();

    private final int batchSize;
    private final long flushIntervalMs;
    private final long idlePollMs;
    private final BlockingQueue<GlobalSession> buffer;
    private final BatchDeleteHandler deleteHandler;
    private final BackgroundDbResourceGuard dbResourceGuard;
    private final InFlightSessionRegistry inFlightRegistry;
    private final PipelineMetrics metrics;
    private final ThreadFactory threadFactory;

    private final AtomicBoolean running = new AtomicBoolean(true);
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();
    private volatile Thread cleanerThread;

    /**
     * Creates a batch cleaner configured from {@link PipelineConfig}.
     *
     * @param config           pipeline configuration
     * @param deleteHandler    storage deletion handler
     * @param dbResourceGuard  background DB concurrency guard
     * @param inFlightRegistry optional in-flight lease registry; leases are released after each flush
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
     * Starts the single-writer cleaner daemon thread if not already started and not shut down.
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
            Thread thread = threadFactory.newThread(this::runLoop);
            this.cleanerThread = thread;
            thread.start();
        }
    }

    /**
     * Offers a completed {@link GlobalSession} to the bounded completion buffer without blocking.
     *
     * <p>If the buffer is full, increments the cleaner drop counter and returns {@code false}
     * so callers release any in-flight lease and let fallback scanning clean the row later.
     *
     * @param session completed global session
     * @return {@code true} if accepted into the buffer; {@code false} if rejected or full
     */
    public boolean offer(GlobalSession session) {
        if (session == null || !running.get()) {
            return false;
        }
        ensureStarted();
        if (!running.get()) {
            return false;
        }
        boolean accepted = buffer.offer(session);
        if (!accepted) {
            metrics.recordCleanerDropped();
            return false;
        }
        if (!running.get() && buffer.remove(session)) {
            return false;
        }
        return true;
    }

    private void runLoop() {
        List<GlobalSession> batch = new ArrayList<>(batchSize);
        while (running.get() || !buffer.isEmpty()) {
            batch.clear();
            GlobalSession first;
            try {
                if (running.get()) {
                    first = buffer.poll(idlePollMs, TimeUnit.MILLISECONDS);
                } else {
                    first = buffer.poll();
                }
            } catch (InterruptedException e) {
                if (!running.get()) {
                    first = buffer.poll();
                } else {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            if (first == null) {
                if (!running.get()) {
                    break;
                }
                if (inFlightRegistry != null) {
                    inFlightRegistry.evictExpired();
                }
                continue;
            }

            boolean sawShutdownSentinel = false;
            if (first == shutdownSentinel) {
                sawShutdownSentinel = true;
            } else {
                batch.add(first);
            }

            if (batch.size() < batchSize) {
                sawShutdownSentinel |= drainExcludingSentinel(batch, batchSize - batch.size());
            }

            if (!batch.isEmpty() && batch.size() < batchSize && running.get() && !sawShutdownSentinel) {
                long deadlineNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(flushIntervalMs);
                while (batch.size() < batchSize && running.get()) {
                    long remainingNanos = deadlineNanos - System.nanoTime();
                    if (remainingNanos <= 0L) {
                        break;
                    }
                    try {
                        GlobalSession next = buffer.poll(remainingNanos, TimeUnit.NANOSECONDS);
                        if (next == null) {
                            break;
                        }
                        if (next == shutdownSentinel) {
                            break;
                        }
                        batch.add(next);
                        if (batch.size() < batchSize) {
                            if (drainExcludingSentinel(batch, batchSize - batch.size())) {
                                break;
                            }
                        }
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }

            if (!batch.isEmpty()) {
                flushBatch(batch);
            }
        }
    }

    private boolean drainExcludingSentinel(List<GlobalSession> target, int maxElements) {
        if (maxElements <= 0) {
            return false;
        }
        List<GlobalSession> drained = new ArrayList<>(Math.min(maxElements, 64));
        int count = buffer.drainTo(drained, maxElements);
        if (count == 0) {
            return false;
        }
        boolean sawSentinel = false;
        for (GlobalSession session : drained) {
            if (session == shutdownSentinel) {
                sawSentinel = true;
            } else {
                target.add(session);
            }
        }
        return sawSentinel;
    }

    private void flushBatch(List<GlobalSession> batch) {
        long startNanos = System.nanoTime();
        try {
            dbResourceGuard.run(() -> deleteHandler.deleteBatch(Collections.unmodifiableList(batch)));
            metrics.recordCleanerBatch(batch.size(), System.nanoTime() - startNanos);
        } catch (InterruptedException e) {
            metrics.recordCleanerBatchFailure();
            Thread.currentThread().interrupt();
            LOGGER.warn("Batch global session cleaner interrupted while flushing {} sessions", batch.size(), e);
        } catch (Throwable t) {
            metrics.recordCleanerBatchFailure();
            LOGGER.error(
                    "Batch global session delete failed for {} sessions, attempting fallback recovery",
                    batch.size(),
                    t);
            if (batch.size() > 1) {
                fallbackDeleteIndividually(batch);
            }
        } finally {
            releaseLeases(batch);
        }
    }

    private void fallbackDeleteIndividually(List<GlobalSession> batch) {
        int recovered = 0;
        for (GlobalSession session : batch) {
            try {
                dbResourceGuard.run(() -> deleteHandler.deleteBatch(Collections.singletonList(session)));
                recovered++;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn(
                        "Batch global session cleaner interrupted during single-item fallback for xid={}",
                        session.getXid(),
                        e);
                break;
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

    private void releaseLeases(List<GlobalSession> sessions) {
        if (inFlightRegistry == null) {
            return;
        }
        for (GlobalSession session : sessions) {
            if (session != null && session != shutdownSentinel) {
                inFlightRegistry.release(session.getXid());
            }
        }
    }

    /**
     * Gracefully shuts down the cleaner, stopping new submissions and flushing all remaining
     * buffered sessions within {@code timeoutMs}.
     *
     * @param timeoutMs maximum time in milliseconds to wait for drain and flush completion
     * @return {@code true} if the cleaner thread exited cleanly within the timeout
     */
    public boolean shutdown(long timeoutMs) {
        Thread thread;
        synchronized (lifecycleLock) {
            running.set(false);
            thread = this.cleanerThread;
        }
        if (thread == null) {
            releaseBufferedSessions();
            return true;
        }
        buffer.offer(shutdownSentinel);
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
        List<GlobalSession> remaining = new ArrayList<>();
        buffer.drainTo(remaining);
        releaseLeases(remaining);
    }

    public int getBufferSize() {
        return running.get() ? buffer.size() : 0;
    }

    public boolean isRunning() {
        return running.get();
    }

    public PipelineMetrics getMetrics() {
        return metrics;
    }

    /**
     * Storage SPI invoked by {@link BatchGlobalSessionCleaner} to delete a batch of completed
     * {@link GlobalSession} records.
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
         * @param sessionManager target session manager
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
