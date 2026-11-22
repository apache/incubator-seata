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

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Concurrency guard bounding background database operations (batch cleaner and fallback scanner)
 * so they cannot exhaust the shared DB connection pool or starve foreground Netty requests.
 *
 * <p>Foreground transaction paths must never acquire permits from this guard. Permits obtained
 * via {@link #tryAcquire(long)} or {@link #tryAcquire(long, TimeUnit)} are returned as idempotent
 * {@link AutoCloseable} {@link Permit} handles to prevent double-release permit inflation.
 */
public class BackgroundDbResourceGuard {

    private final int maxConcurrency;
    private final Semaphore semaphore;
    private final PipelineMetrics metrics;

    /**
     * Creates a fair concurrency guard with the given permit limit.
     *
     * @param maxConcurrency maximum concurrent background DB operations (must be {@code >= 1})
     * @param metrics        optional metrics collector
     */
    public BackgroundDbResourceGuard(int maxConcurrency, PipelineMetrics metrics) {
        this(maxConcurrency, true, metrics);
    }

    /**
     * Creates a concurrency guard with configurable semaphore fairness.
     *
     * @param maxConcurrency maximum concurrent background DB operations (must be {@code >= 1})
     * @param fair           whether the underlying {@link Semaphore} uses FIFO fairness
     * @param metrics        optional metrics collector
     */
    public BackgroundDbResourceGuard(int maxConcurrency, boolean fair, PipelineMetrics metrics) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be >= 1, got " + maxConcurrency);
        }
        this.maxConcurrency = maxConcurrency;
        this.semaphore = new Semaphore(maxConcurrency, fair);
        this.metrics = metrics != null ? metrics : new PipelineMetrics();
    }

    /**
     * Acquires a permit, runs the supplied action, and always releases the permit in {@code finally}.
     *
     * @param action background DB operation returning a value
     * @param <T>    return type
     * @param <E>    checked exception type
     * @return result of {@code action}
     * @throws E                    if {@code action} throws
     * @throws InterruptedException if interrupted while waiting for a permit
     */
    public <T, E extends Throwable> T execute(ThrowingSupplier<T, E> action) throws E, InterruptedException {
        Objects.requireNonNull(action, "action must not be null");
        try (Permit ignored = acquirePermit()) {
            return action.get();
        }
    }

    /**
     * Acquires a permit, runs the supplied action, and always releases the permit in {@code finally}.
     *
     * @param action background DB operation
     * @param <E>    checked exception type
     * @throws E                    if {@code action} throws
     * @throws InterruptedException if interrupted while waiting for a permit
     */
    public <E extends Throwable> void run(ThrowingRunnable<E> action) throws E, InterruptedException {
        Objects.requireNonNull(action, "action must not be null");
        execute(() -> {
            action.run();
            return null;
        });
    }

    /**
     * Attempts to acquire a permit within the specified millisecond timeout.
     *
     * @param timeoutMs maximum wait duration in milliseconds
     * @return an {@link Optional} containing an idempotent {@link Permit} handle if acquired,
     *         or {@link Optional#empty()} if the timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    public Optional<Permit> tryAcquire(long timeoutMs) throws InterruptedException {
        return tryAcquire(timeoutMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Attempts to acquire a permit within the specified timeout.
     *
     * @param timeout maximum wait duration
     * @param unit    time unit of {@code timeout}
     * @return an {@link Optional} containing an idempotent {@link Permit} handle if acquired,
     *         or {@link Optional#empty()} if the timeout elapsed
     * @throws InterruptedException if interrupted while waiting
     */
    public Optional<Permit> tryAcquire(long timeout, TimeUnit unit) throws InterruptedException {
        Objects.requireNonNull(unit, "unit must not be null");
        metrics.incrementDbGuardWaiting();
        long startNanos = System.nanoTime();
        boolean acquired = false;
        try {
            acquired = semaphore.tryAcquire(Math.max(0L, timeout), unit);
        } finally {
            recordPostAcquireMetrics(System.nanoTime() - startNanos, acquired);
        }
        return acquired ? Optional.of(new Permit(this)) : Optional.empty();
    }

    private Permit acquirePermit() throws InterruptedException {
        metrics.incrementDbGuardWaiting();
        long startNanos = System.nanoTime();
        boolean acquired = false;
        try {
            semaphore.acquire();
            acquired = true;
        } finally {
            recordPostAcquireMetrics(System.nanoTime() - startNanos, acquired);
        }
        return new Permit(this);
    }

    private void recordPostAcquireMetrics(long waitNanos, boolean acquired) {
        try {
            metrics.decrementDbGuardWaiting();
            metrics.recordDbGuardWait(waitNanos);
            if (acquired) {
                metrics.incrementDbGuardActive();
            }
        } catch (Throwable metricsError) {
            if (acquired) {
                semaphore.release();
            }
            throw metricsError;
        }
    }

    private void releasePermit() {
        try {
            metrics.decrementDbGuardActive();
        } finally {
            semaphore.release();
        }
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    public int availablePermits() {
        return semaphore.availablePermits();
    }

    public int getActiveCount() {
        return metrics.getDbGuardActiveCount();
    }

    public int getWaitingCount() {
        return metrics.getDbGuardWaitingCount();
    }

    /**
     * Idempotent {@link AutoCloseable} handle releasing a single guard permit upon {@link #close()}.
     */
    public static final class Permit implements AutoCloseable {

        private final BackgroundDbResourceGuard guard;
        private final AtomicBoolean released = new AtomicBoolean(false);

        private Permit(BackgroundDbResourceGuard guard) {
            this.guard = guard;
        }

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                guard.releasePermit();
            }
        }
    }

    /**
     * Supplier capable of throwing a typed exception.
     *
     * @param <T> result type
     * @param <E> exception type
     */
    @FunctionalInterface
    public interface ThrowingSupplier<T, E extends Throwable> {
        T get() throws E;
    }

    /**
     * Runnable capable of throwing a typed exception.
     *
     * @param <E> exception type
     */
    @FunctionalInterface
    public interface ThrowingRunnable<E extends Throwable> {
        void run() throws E;
    }
}
