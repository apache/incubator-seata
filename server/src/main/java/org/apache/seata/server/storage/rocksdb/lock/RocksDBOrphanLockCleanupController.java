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
package org.apache.seata.server.storage.rocksdb.lock;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.thread.ThreadPoolExecutorFactory;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/**
 * Background controller that incrementally cleans orphan RocksDB row locks.
 *
 * <p>A cleanup <em>cycle</em> repeatedly executes bounded cleanup <em>rounds</em> until one full pass over the lock
 * branch index completes. Every round scans at most {@code batchLimit * maxBatches} lock index entries via
 * {@link RocksDBLockManager#cleanOrphanLocksBatches(byte[], int, int)}, so a single round stays short (roughly one
 * to two seconds at million-lock scale instead of a full-pass stall of tens of seconds). Between rounds the
 * controller sleeps {@code roundSleepMillis} to limit foreground I/O interference.
 *
 * <p>The scan position (seek key cursor) is persisted in the {@code METADATA} column family after every incomplete
 * round, so a restarted server resumes a partially completed pass instead of starting over. The persisted cursor is
 * removed once a pass completes.
 */
public class RocksDBOrphanLockCleanupController implements AutoCloseable {

    public static final boolean DEFAULT_ORPHAN_LOCK_CLEAN_ENABLED = true;
    public static final long DEFAULT_ORPHAN_LOCK_CLEAN_INTERVAL_MILLIS = 60_000L;
    public static final int DEFAULT_ORPHAN_LOCK_CLEAN_BATCH_LIMIT = 1000;
    public static final int DEFAULT_ORPHAN_LOCK_CLEAN_MAX_BATCHES = 2;
    public static final long DEFAULT_ORPHAN_LOCK_CLEAN_ROUND_SLEEP_MILLIS = 100L;

    static final byte[] ORPHAN_LOCK_CLEAN_CURSOR_KEY = "orphan_lock_clean_cursor".getBytes(StandardCharsets.UTF_8);

    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBOrphanLockCleanupController.class);
    private static final RocksDBOrphanLockCleanupController DISABLED = new RocksDBOrphanLockCleanupController();

    private final RocksDBLockManager lockManager;
    private final RocksDBStoreEngine storeEngine;
    private final long intervalMillis;
    private final int batchLimit;
    private final int maxBatches;
    private final long roundSleepMillis;
    private final ScheduledExecutorService executor;
    private final boolean shutdownExecutor;
    private final Sleeper sleeper;
    private final LongSupplier nanoTime;

    private final AtomicBoolean started = new AtomicBoolean();
    private final AtomicBoolean closed = new AtomicBoolean();
    private final AtomicBoolean cycleRunning = new AtomicBoolean();
    private final Object closeMonitor = new Object();
    private volatile StoreException closeFailure;

    private final AtomicLong completedCycles = new AtomicLong();
    private final AtomicLong completedPasses = new AtomicLong();
    private final AtomicLong totalRounds = new AtomicLong();
    private final AtomicLong totalCleaned = new AtomicLong();
    private final AtomicLong totalScanned = new AtomicLong();
    private volatile long lastCycleRounds;
    private volatile long lastCycleCleaned;
    private volatile long lastCycleScanned;
    private volatile long lastCycleCostMillis;

    /**
     * No-op constructor for the disabled singleton.
     */
    private RocksDBOrphanLockCleanupController() {
        this.lockManager = null;
        this.storeEngine = null;
        this.intervalMillis = 0L;
        this.batchLimit = 0;
        this.maxBatches = 0;
        this.roundSleepMillis = 0L;
        this.executor = null;
        this.shutdownExecutor = false;
        this.sleeper = millis -> {};
        this.nanoTime = System::nanoTime;
    }

    RocksDBOrphanLockCleanupController(
            RocksDBLockManager lockManager,
            RocksDBStoreEngine storeEngine,
            long intervalMillis,
            int batchLimit,
            int maxBatches,
            long roundSleepMillis,
            ScheduledExecutorService executor,
            boolean shutdownExecutor,
            Sleeper sleeper,
            LongSupplier nanoTime) {
        if (lockManager == null) {
            throw new StoreException("lockManager must not be null");
        }
        if (storeEngine == null) {
            throw new StoreException("storeEngine must not be null");
        }
        if (intervalMillis <= 0) {
            throw new StoreException("intervalMillis must be positive:" + intervalMillis);
        }
        if (batchLimit <= 0) {
            throw new StoreException("batchLimit must be positive:" + batchLimit);
        }
        if (maxBatches <= 0) {
            throw new StoreException("maxBatches must be positive:" + maxBatches);
        }
        if (roundSleepMillis < 0) {
            throw new StoreException("roundSleepMillis must be non-negative:" + roundSleepMillis);
        }
        this.lockManager = lockManager;
        this.storeEngine = storeEngine;
        this.intervalMillis = intervalMillis;
        this.batchLimit = batchLimit;
        this.maxBatches = maxBatches;
        this.roundSleepMillis = roundSleepMillis;
        this.executor = executor;
        this.shutdownExecutor = shutdownExecutor;
        this.sleeper = sleeper == null ? Thread::sleep : sleeper;
        this.nanoTime = nanoTime == null ? System::nanoTime : nanoTime;
    }

    /**
     * Create a controller from configuration. Returns a disabled no-op instance when the cleanup is disabled.
     */
    public static RocksDBOrphanLockCleanupController create(
            RocksDBLockManager lockManager, RocksDBStoreEngine storeEngine) {
        if (lockManager == null || storeEngine == null) {
            return disabled();
        }
        Configuration config = ConfigurationFactory.getInstance();
        boolean enabled = config.getBoolean(
                ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_ENABLED, DEFAULT_ORPHAN_LOCK_CLEAN_ENABLED);
        if (!enabled) {
            LOGGER.info("RocksDB background orphan lock cleanup is disabled");
            return disabled();
        }
        long intervalMillis = positive(
                config.getLong(
                        ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_INTERVAL_MILLIS,
                        DEFAULT_ORPHAN_LOCK_CLEAN_INTERVAL_MILLIS),
                ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_INTERVAL_MILLIS);
        int batchLimit = positive(
                config.getInt(
                        ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_BATCH_LIMIT,
                        DEFAULT_ORPHAN_LOCK_CLEAN_BATCH_LIMIT),
                ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_BATCH_LIMIT);
        int maxBatches = positive(
                config.getInt(
                        ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_MAX_BATCHES,
                        DEFAULT_ORPHAN_LOCK_CLEAN_MAX_BATCHES),
                ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_MAX_BATCHES);
        long roundSleepMillis = nonNegative(
                config.getLong(
                        ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_ROUND_SLEEP_MILLIS,
                        DEFAULT_ORPHAN_LOCK_CLEAN_ROUND_SLEEP_MILLIS),
                ConfigurationKeys.STORE_FILE_ROCKSDB_ORPHAN_LOCK_CLEAN_ROUND_SLEEP_MILLIS);
        ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(
                1, ThreadPoolExecutorFactory.newThreadFactory("rocksdb-orphan-lock-cleanup", 1, true));
        return new RocksDBOrphanLockCleanupController(
                lockManager,
                storeEngine,
                intervalMillis,
                batchLimit,
                maxBatches,
                roundSleepMillis,
                executor,
                true,
                Thread::sleep,
                System::nanoTime);
    }

    static RocksDBOrphanLockCleanupController disabled() {
        return DISABLED;
    }

    public boolean isEnabled() {
        return lockManager != null;
    }

    /**
     * Start the periodic cleanup schedule. Has no effect on a disabled controller or when already started.
     */
    public void start() {
        if (!isEnabled() || !started.compareAndSet(false, true)) {
            return;
        }
        executor.scheduleWithFixedDelay(this::scheduledCycle, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        LOGGER.info(
                "RocksDB background orphan lock cleanup started, intervalMillis:{}, batchLimit:{}, maxBatches:{}, "
                        + "roundSleepMillis:{}",
                intervalMillis,
                batchLimit,
                maxBatches,
                roundSleepMillis);
    }

    /**
     * Submit an asynchronous cleanup cycle. Returns false when the controller is disabled, closed, or shutting down.
     */
    public boolean triggerNow() {
        if (!isEnabled() || closed.get()) {
            return false;
        }
        try {
            executor.execute(this::scheduledCycle);
            return true;
        } catch (RejectedExecutionException e) {
            return false;
        }
    }

    /**
     * Run one full cleanup cycle synchronously: bounded rounds until the pass completes, the controller is closed,
     * or the thread is interrupted. Concurrent cycles are skipped.
     */
    void runCycle() {
        if (!isEnabled() || closed.get() || !cycleRunning.compareAndSet(false, true)) {
            return;
        }
        long startNanos = nanoTime.getAsLong();
        int rounds = 0;
        long cleaned = 0;
        long scanned = 0;
        boolean passCompleted = false;
        try {
            while (!closed.get()) {
                RocksDBLockManager.CleanOrphanLocksResult result = runOneRound();
                rounds++;
                cleaned += result.getCleaned();
                scanned += result.getScanned();
                if (!result.isLimitReached() || result.getNextSeekKey() == null) {
                    passCompleted = true;
                    break;
                }
                if (roundSleepMillis > 0) {
                    sleeper.sleep(roundSleepMillis);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            // The cursor of the last incomplete round is already persisted, so the next cycle resumes.
            LOGGER.error(
                    "RocksDB orphan lock cleanup cycle aborted, rounds:{}, cleaned:{}, scanned:{}",
                    rounds,
                    cleaned,
                    scanned,
                    e);
        } finally {
            cycleRunning.set(false);
            long costMillis = TimeUnit.NANOSECONDS.toMillis(Math.max(0L, nanoTime.getAsLong() - startNanos));
            if (rounds > 0) {
                completedCycles.incrementAndGet();
            }
            totalRounds.addAndGet(rounds);
            totalCleaned.addAndGet(cleaned);
            totalScanned.addAndGet(scanned);
            if (passCompleted) {
                completedPasses.incrementAndGet();
            }
            lastCycleRounds = rounds;
            lastCycleCleaned = cleaned;
            lastCycleScanned = scanned;
            lastCycleCostMillis = costMillis;
            if (rounds > 0 && (cleaned > 0 || passCompleted)) {
                LOGGER.info(
                        "RocksDB orphan lock cleanup cycle finished, rounds:{}, cleaned:{}, scanned:{}, "
                                + "costMillis:{}, passCompleted:{}",
                        rounds,
                        cleaned,
                        scanned,
                        costMillis,
                        passCompleted);
            }
        }
    }

    /**
     * Execute a single bounded cleanup round: load the persisted cursor, run
     * {@code maxBatches} batches of at most {@code batchLimit} lock index entries, then persist the new cursor
     * (or clear it when the pass completed).
     */
    RocksDBLockManager.CleanOrphanLocksResult runOneRound() {
        byte[] cursor = loadPersistedCursor();
        RocksDBLockManager.CleanOrphanLocksResult result =
                lockManager.cleanOrphanLocksBatches(cursor, batchLimit, maxBatches);
        byte[] nextSeekKey = result.getNextSeekKey();
        if (result.isLimitReached() && nextSeekKey != null) {
            savePersistedCursor(nextSeekKey);
        } else {
            clearPersistedCursor();
        }
        return result;
    }

    byte[] loadPersistedCursor() {
        return storeEngine.get(RocksDBColumnFamily.METADATA, ORPHAN_LOCK_CLEAN_CURSOR_KEY);
    }

    private void savePersistedCursor(byte[] cursor) {
        storeEngine.put(RocksDBColumnFamily.METADATA, ORPHAN_LOCK_CLEAN_CURSOR_KEY, cursor);
    }

    private void clearPersistedCursor() {
        storeEngine.delete(RocksDBColumnFamily.METADATA, ORPHAN_LOCK_CLEAN_CURSOR_KEY);
    }

    private void scheduledCycle() {
        try {
            runCycle();
        } catch (Throwable t) {
            // Never propagate: scheduleWithFixedDelay would silently cancel all subsequent runs.
            LOGGER.error("RocksDB orphan lock cleanup cycle failed unexpectedly", t);
        }
    }

    public long getCompletedCycles() {
        return completedCycles.get();
    }

    public long getCompletedPasses() {
        return completedPasses.get();
    }

    public long getTotalRounds() {
        return totalRounds.get();
    }

    public long getTotalCleaned() {
        return totalCleaned.get();
    }

    public long getTotalScanned() {
        return totalScanned.get();
    }

    public long getLastCycleRounds() {
        return lastCycleRounds;
    }

    public long getLastCycleCleaned() {
        return lastCycleCleaned;
    }

    public long getLastCycleScanned() {
        return lastCycleScanned;
    }

    public long getLastCycleCostMillis() {
        return lastCycleCostMillis;
    }

    @Override
    public void close() {
        if (!isEnabled()) {
            return;
        }
        synchronized (closeMonitor) {
            if (closeFailure != null) {
                throw closeFailure;
            }
            if (!closed.compareAndSet(false, true) || !shutdownExecutor) {
                return;
            }
            try {
                shutdownExecutorAndAwaitTermination();
            } catch (StoreException e) {
                closeFailure = e;
                throw e;
            } catch (RuntimeException e) {
                StoreException failure = new StoreException(e, "shut down RocksDB orphan lock cleanup failed");
                closeFailure = failure;
                throw failure;
            }
        }
    }

    private void shutdownExecutorAndAwaitTermination() {
        long waitMillis = Math.min(5000L, Math.max(1000L, roundSleepMillis * 2 + 1000L));
        executor.shutdown();
        InterruptedException interruption = null;
        boolean terminated = false;
        try {
            try {
                terminated = awaitExecutorTermination(waitMillis);
            } catch (InterruptedException e) {
                interruption = e;
            }
            if (!terminated) {
                executor.shutdownNow();
                try {
                    terminated = awaitExecutorTermination(waitMillis);
                } catch (InterruptedException e) {
                    if (interruption == null) {
                        interruption = e;
                    } else {
                        interruption.addSuppressed(e);
                    }
                }
            }
            if (interruption != null) {
                String message = terminated
                        ? "interrupted while shutting down RocksDB orphan lock cleanup"
                        : "interrupted before RocksDB orphan lock cleanup executor termination was confirmed";
                throw new StoreException(interruption, message);
            }
            if (!terminated) {
                throw new StoreException("RocksDB orphan lock cleanup executor did not terminate");
            }
        } finally {
            if (interruption != null) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean awaitExecutorTermination(long waitMillis) throws InterruptedException {
        return executor.awaitTermination(waitMillis, TimeUnit.MILLISECONDS) && executor.isTerminated();
    }

    private static long positive(long value, String key) {
        if (value <= 0) {
            throw new StoreException(
                    "RocksDB orphan lock cleanup config must be positive, key:" + key + ", value:" + value);
        }
        return value;
    }

    private static int positive(int value, String key) {
        if (value <= 0) {
            throw new StoreException(
                    "RocksDB orphan lock cleanup config must be positive, key:" + key + ", value:" + value);
        }
        return value;
    }

    private static long nonNegative(long value, String key) {
        if (value < 0) {
            throw new StoreException(
                    "RocksDB orphan lock cleanup config must be non-negative, key:" + key + ", value:" + value);
        }
        return value;
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long millis) throws InterruptedException;
    }
}
