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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

/**
 * Immutable configuration holder for the two-phase transaction cleanup pipeline.
 *
 * <p>All switches default to disabled ({@code false}) so existing 2.x recovery behavior
 * remains unchanged unless operators explicitly opt in.
 */
public final class PipelineConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(PipelineConfig.class);

    private final boolean enabled;
    private final boolean pushEnabled;
    private final int queueCapacity;
    private final int rpcPoolSize;
    private final int maxConcurrentPerRm;
    private final int cleanerBatchSize;
    private final long cleanerFlushIntervalMs;
    private final int cleanerBufferCapacity;
    private final int dbMaxConcurrency;
    private final long inFlightLeaseMs;
    private final long scanQuietWindowMs;
    private final int scanMaxPagesPerTick;

    private PipelineConfig(Builder builder) {
        this.enabled = builder.enabled;
        this.pushEnabled = builder.pushEnabled;
        this.queueCapacity = clampMinInt(ConfigurationKeys.RECOVERY_PIPELINE_QUEUE_CAPACITY, builder.queueCapacity, 1);
        this.rpcPoolSize = clampMinInt(ConfigurationKeys.RECOVERY_PIPELINE_RPC_POOL_SIZE, builder.rpcPoolSize, 1);
        this.maxConcurrentPerRm =
                clampMinInt(ConfigurationKeys.RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM, builder.maxConcurrentPerRm, 0);
        this.cleanerBatchSize =
                clampMinInt(ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_BATCH_SIZE, builder.cleanerBatchSize, 1);
        this.cleanerFlushIntervalMs = clampMinLong(
                ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS, builder.cleanerFlushIntervalMs, 1L);
        this.cleanerBufferCapacity = clampMinInt(
                ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY, builder.cleanerBufferCapacity, 1);
        this.dbMaxConcurrency =
                clampMinInt(ConfigurationKeys.RECOVERY_PIPELINE_DB_MAX_CONCURRENCY, builder.dbMaxConcurrency, 1);
        this.inFlightLeaseMs =
                clampMinLong(ConfigurationKeys.RECOVERY_PIPELINE_INFLIGHT_LEASE_MS, builder.inFlightLeaseMs, 1L);
        this.scanQuietWindowMs =
                clampMinLong(ConfigurationKeys.RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS, builder.scanQuietWindowMs, 0L);
        this.scanMaxPagesPerTick = clampMinInt(
                ConfigurationKeys.RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK, builder.scanMaxPagesPerTick, 1);
    }

    /**
     * Returns a {@link PipelineConfig} populated with all safe default values.
     *
     * @return default pipeline configuration
     */
    public static PipelineConfig defaults() {
        return builder().build();
    }

    /**
     * Loads a {@link PipelineConfig} from the active {@link ConfigurationFactory} instance.
     *
     * @return loaded pipeline configuration
     */
    public static PipelineConfig fromConfiguration() {
        return fromConfiguration(ConfigurationFactory.getInstance());
    }

    /**
     * Loads a {@link PipelineConfig} from the supplied {@link Configuration}.
     *
     * @param configuration configuration source; returns {@link #defaults()} if {@code null}
     * @return loaded pipeline configuration
     */
    public static PipelineConfig fromConfiguration(Configuration configuration) {
        if (configuration == null) {
            return defaults();
        }
        return builder()
                .enabled(configuration.getBoolean(
                        ConfigurationKeys.RECOVERY_PIPELINE_ENABLE, DefaultValues.DEFAULT_RECOVERY_PIPELINE_ENABLE))
                .pushEnabled(configuration.getBoolean(
                        ConfigurationKeys.RECOVERY_PIPELINE_PUSH_ENABLE,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_PUSH_ENABLE))
                .queueCapacity(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_QUEUE_CAPACITY,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_QUEUE_CAPACITY))
                .rpcPoolSize(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_RPC_POOL_SIZE,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_RPC_POOL_SIZE))
                .maxConcurrentPerRm(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM))
                .cleanerBatchSize(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_BATCH_SIZE,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BATCH_SIZE))
                .cleanerFlushIntervalMs(configuration.getLong(
                        ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS))
                .cleanerBufferCapacity(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY))
                .dbMaxConcurrency(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_DB_MAX_CONCURRENCY,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_DB_MAX_CONCURRENCY))
                .inFlightLeaseMs(configuration.getLong(
                        ConfigurationKeys.RECOVERY_PIPELINE_INFLIGHT_LEASE_MS,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_INFLIGHT_LEASE_MS))
                .scanQuietWindowMs(configuration.getLong(
                        ConfigurationKeys.RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS))
                .scanMaxPagesPerTick(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK))
                .build();
    }

    /**
     * Creates a new {@link Builder} initialized with default values.
     *
     * @return builder instance
     */
    public static Builder builder() {
        return new Builder();
    }

    private static int clampMinInt(String key, int value, int min) {
        if (value < min) {
            LOGGER.warn("Invalid pipeline config {}={}, clamping to {}", key, value, min);
            return min;
        }
        return value;
    }

    private static long clampMinLong(String key, long value, long min) {
        if (value < min) {
            LOGGER.warn("Invalid pipeline config {}={}, clamping to {}", key, value, min);
            return min;
        }
        return value;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isPushEnabled() {
        return pushEnabled;
    }

    /**
     * Returns {@code true} only when both the main pipeline switch and the push switch are enabled.
     *
     * @return whether push mode is effectively active
     */
    public boolean isEffectivePushEnabled() {
        return enabled && pushEnabled;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public int getRpcPoolSize() {
        return rpcPoolSize;
    }

    /**
     * Maximum concurrent phase-two RPCs permitted per {@code resourceId}.
     * Zero means unlimited.
     *
     * @return per-RM concurrency cap, or {@code 0} when unlimited
     */
    public int getMaxConcurrentPerRm() {
        return maxConcurrentPerRm;
    }

    public boolean isPerRmConcurrencyLimited() {
        return maxConcurrentPerRm > 0;
    }

    public int getCleanerBatchSize() {
        return cleanerBatchSize;
    }

    public long getCleanerFlushIntervalMs() {
        return cleanerFlushIntervalMs;
    }

    public int getCleanerBufferCapacity() {
        return cleanerBufferCapacity;
    }

    public int getDbMaxConcurrency() {
        return dbMaxConcurrency;
    }

    public long getInFlightLeaseMs() {
        return inFlightLeaseMs;
    }

    public long getScanQuietWindowMs() {
        return scanQuietWindowMs;
    }

    public int getScanMaxPagesPerTick() {
        return scanMaxPagesPerTick;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PipelineConfig)) {
            return false;
        }
        PipelineConfig that = (PipelineConfig) o;
        return enabled == that.enabled
                && pushEnabled == that.pushEnabled
                && queueCapacity == that.queueCapacity
                && rpcPoolSize == that.rpcPoolSize
                && maxConcurrentPerRm == that.maxConcurrentPerRm
                && cleanerBatchSize == that.cleanerBatchSize
                && cleanerFlushIntervalMs == that.cleanerFlushIntervalMs
                && cleanerBufferCapacity == that.cleanerBufferCapacity
                && dbMaxConcurrency == that.dbMaxConcurrency
                && inFlightLeaseMs == that.inFlightLeaseMs
                && scanQuietWindowMs == that.scanQuietWindowMs
                && scanMaxPagesPerTick == that.scanMaxPagesPerTick;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                enabled,
                pushEnabled,
                queueCapacity,
                rpcPoolSize,
                maxConcurrentPerRm,
                cleanerBatchSize,
                cleanerFlushIntervalMs,
                cleanerBufferCapacity,
                dbMaxConcurrency,
                inFlightLeaseMs,
                scanQuietWindowMs,
                scanMaxPagesPerTick);
    }

    @Override
    public String toString() {
        return "PipelineConfig{"
                + "enabled=" + enabled
                + ", pushEnabled=" + pushEnabled
                + ", queueCapacity=" + queueCapacity
                + ", rpcPoolSize=" + rpcPoolSize
                + ", maxConcurrentPerRm=" + maxConcurrentPerRm
                + ", cleanerBatchSize=" + cleanerBatchSize
                + ", cleanerFlushIntervalMs=" + cleanerFlushIntervalMs
                + ", cleanerBufferCapacity=" + cleanerBufferCapacity
                + ", dbMaxConcurrency=" + dbMaxConcurrency
                + ", inFlightLeaseMs=" + inFlightLeaseMs
                + ", scanQuietWindowMs=" + scanQuietWindowMs
                + ", scanMaxPagesPerTick=" + scanMaxPagesPerTick
                + '}';
    }

    /**
     * Mutable builder for {@link PipelineConfig}.
     */
    public static final class Builder {
        private boolean enabled = DefaultValues.DEFAULT_RECOVERY_PIPELINE_ENABLE;
        private boolean pushEnabled = DefaultValues.DEFAULT_RECOVERY_PIPELINE_PUSH_ENABLE;
        private int queueCapacity = DefaultValues.DEFAULT_RECOVERY_PIPELINE_QUEUE_CAPACITY;
        private int rpcPoolSize = DefaultValues.DEFAULT_RECOVERY_PIPELINE_RPC_POOL_SIZE;
        private int maxConcurrentPerRm = DefaultValues.DEFAULT_RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM;
        private int cleanerBatchSize = DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BATCH_SIZE;
        private long cleanerFlushIntervalMs = DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS;
        private int cleanerBufferCapacity = DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY;
        private int dbMaxConcurrency = DefaultValues.DEFAULT_RECOVERY_PIPELINE_DB_MAX_CONCURRENCY;
        private long inFlightLeaseMs = DefaultValues.DEFAULT_RECOVERY_PIPELINE_INFLIGHT_LEASE_MS;
        private long scanQuietWindowMs = DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS;
        private int scanMaxPagesPerTick = DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK;

        private Builder() {}

        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder pushEnabled(boolean pushEnabled) {
            this.pushEnabled = pushEnabled;
            return this;
        }

        public Builder queueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
            return this;
        }

        public Builder rpcPoolSize(int rpcPoolSize) {
            this.rpcPoolSize = rpcPoolSize;
            return this;
        }

        public Builder maxConcurrentPerRm(int maxConcurrentPerRm) {
            this.maxConcurrentPerRm = maxConcurrentPerRm;
            return this;
        }

        public Builder cleanerBatchSize(int cleanerBatchSize) {
            this.cleanerBatchSize = cleanerBatchSize;
            return this;
        }

        public Builder cleanerFlushIntervalMs(long cleanerFlushIntervalMs) {
            this.cleanerFlushIntervalMs = cleanerFlushIntervalMs;
            return this;
        }

        public Builder cleanerBufferCapacity(int cleanerBufferCapacity) {
            this.cleanerBufferCapacity = cleanerBufferCapacity;
            return this;
        }

        public Builder dbMaxConcurrency(int dbMaxConcurrency) {
            this.dbMaxConcurrency = dbMaxConcurrency;
            return this;
        }

        public Builder inFlightLeaseMs(long inFlightLeaseMs) {
            this.inFlightLeaseMs = inFlightLeaseMs;
            return this;
        }

        public Builder scanQuietWindowMs(long scanQuietWindowMs) {
            this.scanQuietWindowMs = scanQuietWindowMs;
            return this;
        }

        public Builder scanMaxPagesPerTick(int scanMaxPagesPerTick) {
            this.scanMaxPagesPerTick = scanMaxPagesPerTick;
            return this;
        }

        public PipelineConfig build() {
            return new PipelineConfig(this);
        }
    }
}
