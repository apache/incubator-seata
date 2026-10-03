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
package org.apache.seata.server.storage.rocksdb;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.thread.ThreadPoolExecutorFactory;
import org.rocksdb.RocksDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Periodic RocksDB WAL sync controller used by file mode.
 */
final class RocksDBWalSyncController implements AutoCloseable {

    interface WalSyncer {
        void flushWal(boolean sync) throws Exception;

        long latestSequenceNumber();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBWalSyncController.class);
    private static final RocksDBWalSyncController DISABLED = new RocksDBWalSyncController();
    private static final ThreadFactory WAL_SYNC_THREAD_FACTORY =
            ThreadPoolExecutorFactory.newThreadFactory("rocksdb-wal-sync", 1, true);
    private static final ThreadFactory WAL_SHUTDOWN_THREAD_FACTORY =
            ThreadPoolExecutorFactory.newThreadFactory("rocksdb-wal-shutdown", 1, true);
    private static final ThreadFactory WAL_CLEANUP_THREAD_FACTORY =
            ThreadPoolExecutorFactory.newThreadFactory("rocksdb-wal-cleanup", 1, true);

    private final RocksDBWalSyncMode mode;
    private final WalSyncer syncer;
    private final long intervalMillis;
    private final long writeThreshold;
    private final boolean syncOnShutdown;
    private final long warnThresholdMillis;
    private final long shutdownTimeoutMillis;
    private final ScheduledExecutorService executor;
    private final boolean shutdownExecutor;
    private final LongSupplier currentTimeMillis;
    private final LongSupplier nanoTime;
    private final AtomicLong writeRequests = new AtomicLong();
    private final AtomicLong lastSyncedWriteRequests = new AtomicLong();
    private final AtomicLong firstUnsyncedWriteTimeMillis = new AtomicLong();
    private final AtomicLong lastSyncTimeMillis = new AtomicLong();
    private final AtomicLong syncCount = new AtomicLong();
    private final AtomicLong syncFailureCount = new AtomicLong();
    private final AtomicLong totalSyncNanos = new AtomicLong();
    private final AtomicLong lastSyncCostNanos = new AtomicLong();
    private final AtomicLong maxSyncNanos = new AtomicLong();
    private final AtomicLong maxUnsyncedWriteRequests = new AtomicLong();
    private final AtomicLong maxUnsyncedMillis = new AtomicLong();
    private final AtomicLong latestSequenceNumber = new AtomicLong();
    private final AtomicLong lastSyncedSequenceNumber = new AtomicLong();
    private final AtomicBoolean syncScheduled = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean shutdownStarted = new AtomicBoolean();
    private final CompletableFuture<Void> shutdownCompletion = new CompletableFuture<>();

    private volatile String lastSyncError;

    private RocksDBWalSyncController() {
        this.mode = RocksDBWalSyncMode.NONE;
        this.syncer = null;
        this.intervalMillis = 0L;
        this.writeThreshold = 0L;
        this.syncOnShutdown = false;
        this.warnThresholdMillis = 0L;
        this.shutdownTimeoutMillis = 0L;
        this.executor = null;
        this.shutdownExecutor = false;
        this.currentTimeMillis = System::currentTimeMillis;
        this.nanoTime = System::nanoTime;
        this.shutdownStarted.set(true);
        this.shutdownCompletion.complete(null);
    }

    RocksDBWalSyncController(
            RocksDBWalSyncMode mode,
            WalSyncer syncer,
            long intervalMillis,
            long writeThreshold,
            boolean syncOnShutdown,
            long warnThresholdMillis,
            ScheduledExecutorService executor,
            boolean shutdownExecutor,
            LongSupplier currentTimeMillis,
            LongSupplier nanoTime) {
        this(
                mode,
                syncer,
                intervalMillis,
                writeThreshold,
                syncOnShutdown,
                warnThresholdMillis,
                RocksDBStoreConfig.DEFAULT_WAL_SYNC_SHUTDOWN_TIMEOUT_MILLIS,
                executor,
                shutdownExecutor,
                currentTimeMillis,
                nanoTime);
    }

    RocksDBWalSyncController(
            RocksDBWalSyncMode mode,
            WalSyncer syncer,
            long intervalMillis,
            long writeThreshold,
            boolean syncOnShutdown,
            long warnThresholdMillis,
            long shutdownTimeoutMillis,
            ScheduledExecutorService executor,
            boolean shutdownExecutor,
            LongSupplier currentTimeMillis,
            LongSupplier nanoTime) {
        this.mode = mode == null ? RocksDBWalSyncMode.NONE : mode;
        this.syncer = syncer;
        this.intervalMillis = intervalMillis;
        this.writeThreshold = writeThreshold;
        this.syncOnShutdown = syncOnShutdown;
        this.warnThresholdMillis = warnThresholdMillis;
        this.shutdownTimeoutMillis = shutdownTimeoutMillis;
        this.executor = executor;
        this.shutdownExecutor = shutdownExecutor;
        this.currentTimeMillis = currentTimeMillis == null ? System::currentTimeMillis : currentTimeMillis;
        this.nanoTime = nanoTime == null ? System::nanoTime : nanoTime;
        if (isPeriodic()) {
            long now = this.currentTimeMillis.getAsLong();
            lastSyncTimeMillis.set(now);
            latestSequenceNumber.set(syncer.latestSequenceNumber());
            lastSyncedSequenceNumber.set(latestSequenceNumber.get());
            executor.scheduleWithFixedDelay(
                    () -> syncIfIntervalElapsed("interval"), intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    static RocksDBWalSyncController disabled() {
        return DISABLED;
    }

    static RocksDBWalSyncController create(RocksDB db, RocksDBStoreConfig config) {
        if (db == null || config == null || !config.isPeriodicWalSyncEnabled()) {
            return disabled();
        }
        WalSyncer syncer = new RocksDBWalSyncer(db);
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1, WAL_SYNC_THREAD_FACTORY);
        return new RocksDBWalSyncController(
                config.getWalSyncMode(),
                syncer,
                config.getWalSyncIntervalMillis(),
                config.getWalSyncWriteThreshold(),
                config.isWalSyncOnShutdown(),
                config.getWalSyncWarnThresholdMillis(),
                config.getWalSyncShutdownTimeoutMillis(),
                executor,
                true,
                System::currentTimeMillis,
                System::nanoTime);
    }

    boolean isPeriodic() {
        return mode.isPeriodic() && syncer != null && executor != null;
    }

    void afterWrite() {
        if (!isPeriodic() || closed.get()) {
            return;
        }
        long now = currentTimeMillis.getAsLong();
        long writes = writeRequests.incrementAndGet();
        latestSequenceNumber.set(syncer.latestSequenceNumber());
        long unsyncedWrites = Math.max(0L, writes - lastSyncedWriteRequests.get());
        if (unsyncedWrites > 0L) {
            firstUnsyncedWriteTimeMillis.compareAndSet(0L, now);
        }
        updateMax(maxUnsyncedWriteRequests, unsyncedWrites);
        updateUnsyncedMillis(now);
        if (unsyncedWrites >= writeThreshold) {
            requestSync("write-threshold");
        }
    }

    RocksDBWalSyncStats stats() {
        if (!isPeriodic()) {
            return RocksDBWalSyncStats.NONE;
        }
        long writes = writeRequests.get();
        long syncedWrites = lastSyncedWriteRequests.get();
        long now = currentTimeMillis.getAsLong();
        long unsyncedMillis = unsyncedMillis(now);
        long count = syncCount.get();
        long totalNanos = totalSyncNanos.get();
        long avgMillis = count == 0 ? 0L : nanosToMillis(totalNanos / count);
        return new RocksDBWalSyncStats(
                mode,
                count,
                syncFailureCount.get(),
                lastSyncTimeMillis.get(),
                nanosToMillis(lastSyncCostNanos.get()),
                avgMillis,
                nanosToMillis(maxSyncNanos.get()),
                Math.max(0L, writes - syncedWrites),
                maxUnsyncedWriteRequests.get(),
                unsyncedMillis,
                maxUnsyncedMillis.get(),
                latestSequenceNumber.get(),
                lastSyncedSequenceNumber.get(),
                lastSyncError);
    }

    void requestSync(String reason) {
        if (!isPeriodic() || closed.get() || !syncScheduled.compareAndSet(false, true)) {
            return;
        }
        try {
            executor.execute(() -> syncIfNeeded(reason));
        } catch (RejectedExecutionException e) {
            syncScheduled.set(false);
            if (closed.get() || executor.isShutdown()) {
                LOGGER.debug("skip RocksDB WAL sync request because executor is shutting down, reason:{}", reason, e);
                return;
            }
            LOGGER.warn("defer RocksDB WAL sync because background scheduling was rejected, reason:{}", reason, e);
        }
    }

    void syncNow(String reason) {
        if (!isPeriodic()) {
            return;
        }
        syncIfNeeded(reason);
    }

    private void syncIfIntervalElapsed(String reason) {
        if (!isPeriodic() || closed.get()) {
            return;
        }
        long now = currentTimeMillis.getAsLong();
        updateUnsyncedMillis(now);
        if (writeRequests.get() > lastSyncedWriteRequests.get() && now - lastSyncTimeMillis.get() >= intervalMillis) {
            requestSync(reason);
        }
    }

    private synchronized void syncIfNeeded(String reason) {
        syncIfNeeded(reason, false);
    }

    private synchronized void syncIfNeeded(String reason, boolean strict) {
        if (!isPeriodic()) {
            return;
        }
        try {
            if (closed.get() && !strict) {
                return;
            }
            long writes = writeRequests.get();
            if (writes <= lastSyncedWriteRequests.get()) {
                return;
            }
            long startedAt = nanoTime.getAsLong();
            syncer.flushWal(true);
            long cost = Math.max(0L, nanoTime.getAsLong() - startedAt);
            long now = currentTimeMillis.getAsLong();
            latestSequenceNumber.set(syncer.latestSequenceNumber());
            lastSyncedSequenceNumber.set(latestSequenceNumber.get());
            lastSyncedWriteRequests.set(writes);
            if (writeRequests.get() <= writes) {
                firstUnsyncedWriteTimeMillis.set(0L);
            } else {
                firstUnsyncedWriteTimeMillis.set(now);
            }
            lastSyncTimeMillis.set(now);
            lastSyncCostNanos.set(cost);
            totalSyncNanos.addAndGet(cost);
            updateMax(maxSyncNanos, cost);
            syncCount.incrementAndGet();
            lastSyncError = null;
            if (warnThresholdMillis > 0 && TimeUnit.NANOSECONDS.toMillis(cost) > warnThresholdMillis) {
                LOGGER.warn(
                        "periodic RocksDB WAL sync is slow, reason:{}, cost:{}ms, threshold:{}ms",
                        reason,
                        TimeUnit.NANOSECONDS.toMillis(cost),
                        warnThresholdMillis);
            }
        } catch (Exception e) {
            syncFailureCount.incrementAndGet();
            lastSyncError = e.getMessage();
            LOGGER.error("periodic RocksDB WAL sync failed, reason:{}", reason, e);
            if (strict) {
                throw new StoreException(e, "periodic RocksDB WAL sync failed, reason:" + reason);
            }
        } finally {
            syncScheduled.set(false);
        }
    }

    private void updateUnsyncedMillis(long now) {
        long value = unsyncedMillis(now);
        updateMax(maxUnsyncedMillis, value);
    }

    private long unsyncedMillis(long now) {
        if (writeRequests.get() <= lastSyncedWriteRequests.get()) {
            return 0L;
        }
        long firstUnsyncedWrite = firstUnsyncedWriteTimeMillis.get();
        if (firstUnsyncedWrite <= 0L) {
            return 0L;
        }
        return Math.max(0L, now - firstUnsyncedWrite);
    }

    private static void updateMax(AtomicLong target, long value) {
        long current;
        do {
            current = target.get();
            if (value <= current) {
                return;
            }
        } while (!target.compareAndSet(current, value));
    }

    private static long nanosToMillis(long nanos) {
        return TimeUnit.NANOSECONDS.toMillis(nanos);
    }

    @Override
    public void close() {
        if (!isPeriodic()) {
            return;
        }
        startShutdown();
        awaitShutdownCompletion();
    }

    void executeAfterShutdownAsync(Runnable action) {
        if (!isPeriodic()) {
            startCleanupThread(action);
            return;
        }
        startShutdown();
        shutdownCompletion.whenComplete((ignored, failure) -> startCleanupThread(action));
    }

    private void startCleanupThread(Runnable action) {
        Thread cleanupThread = WAL_CLEANUP_THREAD_FACTORY.newThread(() -> {
            try {
                action.run();
            } catch (Throwable cleanupFailure) {
                LOGGER.error("RocksDB cleanup after WAL shutdown failed", cleanupFailure);
            }
        });
        cleanupThread.start();
    }

    private void startShutdown() {
        if (!shutdownStarted.compareAndSet(false, true)) {
            return;
        }
        closed.set(true);
        Thread shutdownThread = WAL_SHUTDOWN_THREAD_FACTORY.newThread(this::runShutdown);
        shutdownThread.start();
    }

    private void runShutdown() {
        Throwable shutdownFailure = null;
        if (shutdownExecutor) {
            try {
                executor.shutdownNow();
            } catch (Throwable failure) {
                shutdownFailure = failure;
            }
        }
        try {
            if (syncOnShutdown) {
                syncIfNeeded("shutdown", true);
            } else {
                awaitInFlightSync();
            }
        } catch (Throwable failure) {
            if (shutdownFailure == null) {
                shutdownFailure = failure;
            } else if (shutdownFailure != failure) {
                shutdownFailure.addSuppressed(failure);
            }
        }
        if (shutdownFailure == null) {
            shutdownCompletion.complete(null);
        } else {
            shutdownCompletion.completeExceptionally(shutdownFailure);
        }
    }

    private void awaitShutdownCompletion() {
        boolean interrupted = Thread.interrupted();
        long timeoutNanos = TimeUnit.MILLISECONDS.toNanos(shutdownTimeoutMillis);
        long deadline = System.nanoTime() + timeoutNanos;
        try {
            while (true) {
                long remainingNanos = deadline - System.nanoTime();
                if (remainingNanos <= 0L) {
                    throw new ShutdownTimeoutException(shutdownTimeoutMillis);
                }
                try {
                    shutdownCompletion.get(remainingNanos, TimeUnit.NANOSECONDS);
                    return;
                } catch (InterruptedException e) {
                    interrupted = true;
                } catch (TimeoutException e) {
                    throw new ShutdownTimeoutException(shutdownTimeoutMillis);
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof RuntimeException) {
                        throw (RuntimeException) cause;
                    }
                    if (cause instanceof Error) {
                        throw (Error) cause;
                    }
                    throw new StoreException(cause, "RocksDB WAL shutdown failed");
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private synchronized void awaitInFlightSync() {
        // Acquiring the sync monitor is the shutdown barrier for an active native flushWal call.
    }

    static final class ShutdownTimeoutException extends StoreException {
        private ShutdownTimeoutException(long timeoutMillis) {
            super("RocksDB WAL shutdown did not complete within " + timeoutMillis + "ms");
        }
    }

    private static final class RocksDBWalSyncer implements WalSyncer {
        private final RocksDB db;

        private RocksDBWalSyncer(RocksDB db) {
            this.db = db;
        }

        @Override
        public void flushWal(boolean sync) throws Exception {
            db.flushWal(sync);
        }

        @Override
        public long latestSequenceNumber() {
            return db.getLatestSequenceNumber();
        }
    }
}
