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
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PipelineConfigTest {

    @Test
    void defaultsDisablePipelineAndUseSafeConstants() {
        PipelineConfig config = PipelineConfig.defaults();

        assertFalse(config.isEnabled());
        assertFalse(config.isPushEnabled());
        assertFalse(config.isEffectivePushEnabled());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_QUEUE_CAPACITY, config.getQueueCapacity());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_RPC_POOL_SIZE, config.getRpcPoolSize());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM, config.getMaxConcurrentPerRm());
        assertTrue(config.isPerRmConcurrencyLimited());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BATCH_SIZE, config.getCleanerBatchSize());
        assertEquals(
                DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS, config.getCleanerFlushIntervalMs());
        assertEquals(
                DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY, config.getCleanerBufferCapacity());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_DB_MAX_CONCURRENCY, config.getDbMaxConcurrency());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_INFLIGHT_LEASE_MS, config.getInFlightLeaseMs());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS, config.getScanQuietWindowMs());
        assertEquals(DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK, config.getScanMaxPagesPerTick());
    }

    @Test
    void fromConfigurationParsesAllKeys() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getBoolean(
                        ConfigurationKeys.RECOVERY_PIPELINE_ENABLE, DefaultValues.DEFAULT_RECOVERY_PIPELINE_ENABLE))
                .thenReturn(true);
        when(configuration.getBoolean(
                        ConfigurationKeys.RECOVERY_PIPELINE_PUSH_ENABLE,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_PUSH_ENABLE))
                .thenReturn(true);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_QUEUE_CAPACITY,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_QUEUE_CAPACITY))
                .thenReturn(2048);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_RPC_POOL_SIZE,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_RPC_POOL_SIZE))
                .thenReturn(32);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_MAX_CONCURRENT_PER_RM))
                .thenReturn(6);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_BATCH_SIZE,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BATCH_SIZE))
                .thenReturn(200);
        when(configuration.getLong(
                        ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_FLUSH_INTERVAL_MS))
                .thenReturn(75L);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_CLEANER_BUFFER_CAPACITY))
                .thenReturn(4096);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_DB_MAX_CONCURRENCY,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_DB_MAX_CONCURRENCY))
                .thenReturn(12);
        when(configuration.getLong(
                        ConfigurationKeys.RECOVERY_PIPELINE_INFLIGHT_LEASE_MS,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_INFLIGHT_LEASE_MS))
                .thenReturn(45000L);
        when(configuration.getLong(
                        ConfigurationKeys.RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_QUIET_WINDOW_MS))
                .thenReturn(3500L);
        when(configuration.getInt(
                        ConfigurationKeys.RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK,
                        DefaultValues.DEFAULT_RECOVERY_PIPELINE_SCAN_MAX_PAGES_PER_TICK))
                .thenReturn(80);

        PipelineConfig config = PipelineConfig.fromConfiguration(configuration);

        assertTrue(config.isEnabled());
        assertTrue(config.isPushEnabled());
        assertTrue(config.isEffectivePushEnabled());
        assertEquals(2048, config.getQueueCapacity());
        assertEquals(32, config.getRpcPoolSize());
        assertEquals(6, config.getMaxConcurrentPerRm());
        assertEquals(200, config.getCleanerBatchSize());
        assertEquals(75L, config.getCleanerFlushIntervalMs());
        assertEquals(4096, config.getCleanerBufferCapacity());
        assertEquals(12, config.getDbMaxConcurrency());
        assertEquals(45000L, config.getInFlightLeaseMs());
        assertEquals(3500L, config.getScanQuietWindowMs());
        assertEquals(80, config.getScanMaxPagesPerTick());
    }

    @Test
    void fromNullConfigurationReturnsDefaults() {
        assertEquals(PipelineConfig.defaults(), PipelineConfig.fromConfiguration(null));
    }

    @Test
    void effectivePushRequiresMainSwitchEnabled() {
        PipelineConfig pushOnly =
                PipelineConfig.builder().enabled(false).pushEnabled(true).build();
        assertFalse(pushOnly.isEnabled());
        assertTrue(pushOnly.isPushEnabled());
        assertFalse(pushOnly.isEffectivePushEnabled());
    }

    @Test
    void invalidValuesAreClampedToSafeBounds() {
        PipelineConfig config = PipelineConfig.builder()
                .queueCapacity(0)
                .rpcPoolSize(-4)
                .maxConcurrentPerRm(-3)
                .cleanerBatchSize(0)
                .cleanerFlushIntervalMs(-10L)
                .cleanerBufferCapacity(-1)
                .dbMaxConcurrency(0)
                .inFlightLeaseMs(0L)
                .scanQuietWindowMs(-500L)
                .scanMaxPagesPerTick(-2)
                .build();

        assertEquals(1, config.getQueueCapacity());
        assertEquals(1, config.getRpcPoolSize());
        assertEquals(0, config.getMaxConcurrentPerRm());
        assertFalse(config.isPerRmConcurrencyLimited());
        assertEquals(1, config.getCleanerBatchSize());
        assertEquals(1L, config.getCleanerFlushIntervalMs());
        assertEquals(1, config.getCleanerBufferCapacity());
        assertEquals(1, config.getDbMaxConcurrency());
        assertEquals(1L, config.getInFlightLeaseMs());
        assertEquals(0L, config.getScanQuietWindowMs());
        assertEquals(1, config.getScanMaxPagesPerTick());
    }

    @Test
    void equalsAndHashCodeReflectAllFields() {
        PipelineConfig a = PipelineConfig.builder().enabled(true).rpcPoolSize(8).build();
        PipelineConfig b = PipelineConfig.builder().enabled(true).rpcPoolSize(8).build();
        PipelineConfig c =
                PipelineConfig.builder().enabled(true).rpcPoolSize(16).build();

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
        assertNotEquals(a, c);
        assertTrue(a.toString().contains("rpcPoolSize=8"));
    }
}
