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

import org.apache.seata.metrics.Measurement;
import org.apache.seata.metrics.registry.compact.CompactRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RocksDBStoreDiagnosticsTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void afterEach() {
        RocksDBStoreEngineFactory.destroy();
        RocksDBStoreMetrics.unregister(null);
    }

    @Test
    void testDiagnosticsSnapshotContainsRocksDBProperties() {
        try (RocksDBStoreEngine engine = open("diagnostics")) {
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("xid-diagnostics"),
                    "global".getBytes(StandardCharsets.UTF_8));
            engine.put(
                    RocksDBColumnFamily.BRANCH_SESSION,
                    RocksDBKeyCodec.encodeBranch("xid-diagnostics", 1L),
                    "branch".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            RocksDBStoreDiagnostics diagnostics = engine.diagnostics();

            Assertions.assertFalse(diagnostics.isClosed());
            Assertions.assertEquals(tempDir.resolve("diagnostics").toString(), diagnostics.getDbPath());
            Assertions.assertEquals(RocksDBStoreEngine.FORMAT_VERSION, diagnostics.getFormatVersion());
            Assertions.assertNotNull(diagnostics.getRocksDBVersion());
            Assertions.assertTrue(
                    diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.ESTIMATE_LIVE_DATA_SIZE));
            Assertions.assertTrue(
                    diagnostics.getColumnFamilyProperties().containsKey(RocksDBColumnFamily.GLOBAL_SESSION));
            Assertions.assertTrue(diagnostics
                    .getColumnFamilyProperties()
                    .get(RocksDBColumnFamily.GLOBAL_SESSION)
                    .containsKey(RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS));
        }
    }

    @Test
    void testDiagnosticsAfterCloseIsSafe() {
        RocksDBStoreEngine engine = open("closed");
        engine.close();

        RocksDBStoreDiagnostics diagnostics = engine.diagnostics();

        Assertions.assertTrue(diagnostics.isClosed());
        Assertions.assertEquals(tempDir.resolve("closed").toString(), diagnostics.getDbPath());
        Assertions.assertEquals(0L, diagnostics.getProperty(RocksDBStoreDiagnostics.ESTIMATE_LIVE_DATA_SIZE));
    }

    @Test
    void testMetricsRegistryCanCollectGaugesAndStaySafeAfterClose() {
        CompactRegistry registry = new CompactRegistry();
        try {
            RocksDBStoreEngine engine = open("metrics");
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("xid-metrics"),
                    "global".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            RocksDBStoreMetrics.register(registry, engine);
            List<Measurement> measurements = measurements(registry);

            Assertions.assertFalse(measurements.isEmpty());
            Assertions.assertTrue(measurements.stream()
                    .anyMatch(measurement -> measurement.getId().toString().contains("estimateLiveDataSizeBytes")));

            engine.close();

            Assertions.assertDoesNotThrow(() -> measurements(registry));
        } finally {
            registry.clearUp();
        }
    }

    @Test
    void testMetricsRegisterWithNullRegistryIsNoop() {
        try (RocksDBStoreEngine engine = open("metrics-disabled")) {
            Assertions.assertDoesNotThrow(() -> RocksDBStoreMetrics.register(null, engine));
        }
    }

    @Test
    void testExpandedDiagnosticsProperties() {
        try (RocksDBStoreEngine engine = open("diagnostics-expanded")) {
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("xid-expanded"),
                    "global".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            RocksDBStoreDiagnostics diagnostics = engine.diagnostics();

            // G1: new DB-level properties
            Assertions.assertTrue(diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.SIZE_ALL_MEM_TABLES));
            Assertions.assertTrue(
                    diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.ESTIMATE_TABLE_READERS_MEM));
            Assertions.assertTrue(diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.BACKGROUND_ERRORS));
            Assertions.assertTrue(
                    diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.NUM_RUNNING_COMPACTIONS));
            Assertions.assertTrue(diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.NUM_RUNNING_FLUSHES));
            Assertions.assertTrue(
                    diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.ACTUAL_DELAYED_WRITE_RATE));
            Assertions.assertTrue(diagnostics.getProperties().containsKey(RocksDBStoreDiagnostics.IS_WRITE_STOPPED));

            // G2: level 1-6 file counts
            Map<String, Long> cfProps = diagnostics.getColumnFamilyProperties().get(RocksDBColumnFamily.GLOBAL_SESSION);
            Assertions.assertTrue(cfProps.containsKey(RocksDBStoreDiagnostics.NUM_FILES_AT_LEVEL0));
            Assertions.assertTrue(cfProps.containsKey(RocksDBStoreDiagnostics.NUM_FILES_AT_LEVEL1));
            Assertions.assertTrue(cfProps.containsKey(RocksDBStoreDiagnostics.NUM_FILES_AT_LEVEL6));
        }
    }

    @Test
    void testPublicPropertyAccessMethods() {
        try (RocksDBStoreEngine engine = open("public-props")) {
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("xid-public"),
                    "global".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            // G7: public getLongProperty
            long liveDataSize = engine.getLongProperty(RocksDBStoreDiagnostics.ESTIMATE_LIVE_DATA_SIZE);
            Assertions.assertTrue(liveDataSize >= 0L);

            long cfKeys = engine.getLongProperty(
                    RocksDBColumnFamily.GLOBAL_SESSION, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS);
            Assertions.assertTrue(cfKeys >= 0L);

            // G7: public getProperty (string)
            String cfstats = engine.getProperty("cfstats-no-file-histogram");
            // may be null if property not available, should not throw
            Assertions.assertDoesNotThrow(
                    () -> engine.getProperty(RocksDBColumnFamily.GLOBAL_SESSION, "cfstats-no-file-histogram"));

            // unknown property returns 0 / null gracefully
            Assertions.assertEquals(0L, engine.getLongProperty("rocksdb.nonexistent-property"));
            Assertions.assertNull(engine.getProperty("rocksdb.nonexistent-property"));
        }
    }

    @Test
    void testPublicPropertyAccessAfterClose() {
        RocksDBStoreEngine engine = open("props-closed");
        engine.close();

        Assertions.assertEquals(0L, engine.getLongProperty(RocksDBStoreDiagnostics.ESTIMATE_LIVE_DATA_SIZE));
        Assertions.assertEquals(
                0L,
                engine.getLongProperty(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBStoreDiagnostics.ESTIMATE_NUM_KEYS));
        Assertions.assertNull(engine.getProperty("cfstats-no-file-histogram"));
        Assertions.assertNull(engine.getProperty(RocksDBColumnFamily.GLOBAL_SESSION, "cfstats-no-file-histogram"));
    }

    @Test
    void testBlockCacheMetricsWhenEnabled() {
        long blockCacheSize = 8L * 1024 * 1024; // 8 MB
        RocksDBStoreConfig config = new RocksDBStoreConfig(
                tempDir.resolve("block-cache").toString(),
                true,
                blockCacheSize,
                0L,
                0,
                0,
                0,
                0,
                0L,
                0,
                0,
                0,
                false,
                false,
                null,
                false);
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("xid-bc"),
                    "global".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            // block cache access methods
            Assertions.assertEquals(blockCacheSize, engine.getBlockCacheCapacity());
            Assertions.assertTrue(engine.getBlockCacheUsage() >= 0L);
            Assertions.assertTrue(engine.getBlockCachePinnedUsage() >= 0L);

            // diagnostics snapshot includes block cache
            RocksDBStoreDiagnostics diagnostics = engine.diagnostics();
            Assertions.assertTrue(diagnostics.isBlockCacheEnabled());
            Assertions.assertEquals(blockCacheSize, diagnostics.getBlockCacheCapacity());
            Assertions.assertTrue(diagnostics.getBlockCacheUsage() >= 0L);
            Assertions.assertTrue(diagnostics.getBlockCachePinnedUsage() >= 0L);
        }
    }

    @Test
    void testBlockCacheMetricsWhenDisabled() {
        try (RocksDBStoreEngine engine = open("no-block-cache")) {
            engine.put(
                    RocksDBColumnFamily.GLOBAL_SESSION,
                    RocksDBKeyCodec.encodeXid("xid-nbc"),
                    "global".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            // block cache is not configured
            Assertions.assertEquals(0L, engine.getBlockCacheCapacity());
            Assertions.assertEquals(0L, engine.getBlockCacheUsage());
            Assertions.assertEquals(0L, engine.getBlockCachePinnedUsage());

            RocksDBStoreDiagnostics diagnostics = engine.diagnostics();
            Assertions.assertFalse(diagnostics.isBlockCacheEnabled());
            Assertions.assertEquals(0L, diagnostics.getBlockCacheCapacity());
        }
    }

    @Test
    void testBlockCacheMetricsAfterClose() {
        long blockCacheSize = 4L * 1024 * 1024;
        RocksDBStoreConfig config = new RocksDBStoreConfig(
                tempDir.resolve("bc-closed").toString(),
                true,
                blockCacheSize,
                0L,
                0,
                0,
                0,
                0,
                0L,
                0,
                0,
                0,
                false,
                false,
                null,
                false);
        RocksDBStoreEngine engine = RocksDBStoreEngine.open(config);
        engine.close();

        Assertions.assertEquals(0L, engine.getBlockCacheUsage());
        Assertions.assertEquals(0L, engine.getBlockCachePinnedUsage());
        Assertions.assertEquals(0L, engine.getBlockCacheCapacity());
    }

    @Test
    void testBlockCacheMetricsGauges() {
        CompactRegistry registry = new CompactRegistry();
        long blockCacheSize = 8L * 1024 * 1024;
        RocksDBStoreConfig config = new RocksDBStoreConfig(
                tempDir.resolve("bc-metrics").toString(),
                true,
                blockCacheSize,
                0L,
                0,
                0,
                0,
                0,
                0L,
                0,
                0,
                0,
                false,
                false,
                null,
                false);
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            RocksDBStoreMetrics.register(registry, engine);
            List<Measurement> measurements = measurements(registry);

            Assertions.assertTrue(
                    measurements.stream().anyMatch(m -> m.getId().toString().contains("blockCacheUsageBytes")));
            Assertions.assertTrue(
                    measurements.stream().anyMatch(m -> m.getId().toString().contains("blockCachePinnedUsageBytes")));
            Assertions.assertTrue(
                    measurements.stream().anyMatch(m -> m.getId().toString().contains("blockCacheCapacityBytes")));
        } finally {
            registry.clearUp();
        }
    }

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true));
    }

    private List<Measurement> measurements(CompactRegistry registry) {
        List<Measurement> measurements = new ArrayList<>();
        for (Measurement measurement : registry.measure()) {
            measurements.add(measurement);
        }
        return measurements;
    }
}
