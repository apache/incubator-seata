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
import org.apache.seata.core.model.GlobalStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.RocksDB;
import org.rocksdb.RocksDBException;
import org.rocksdb.WriteBatch;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

class RocksDBStoreEngineTest {

    @TempDir
    Path tempDir;

    @AfterEach
    void afterEach() {
        RocksDBStoreEngineFactory.destroy();
    }

    @Test
    void testPutGetDelete() {
        try (RocksDBStoreEngine engine = open("db", true)) {
            byte[] key = RocksDBKeyCodec.encodeXid("xid-1");
            byte[] value = RocksDBValueCodec.encode(
                    RocksDBValueCodec.ValueType.GLOBAL_SESSION, "global".getBytes(StandardCharsets.UTF_8));

            engine.put(RocksDBColumnFamily.GLOBAL_SESSION, key, value);

            Assertions.assertArrayEquals(value, engine.get(RocksDBColumnFamily.GLOBAL_SESSION, key));

            engine.delete(RocksDBColumnFamily.GLOBAL_SESSION, key);
            Assertions.assertNull(engine.get(RocksDBColumnFamily.GLOBAL_SESSION, key));
        }
    }

    @Test
    void testPrefixScan() {
        try (RocksDBStoreEngine engine = open("scan", false)) {
            byte[] value = "branch".getBytes(StandardCharsets.UTF_8);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 1L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 2L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-2", 1L), value);

            List<RocksDBStoreEngine.RocksDBEntry> entries =
                    engine.prefixScan(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1"));

            Assertions.assertEquals(2, entries.size());
        }
    }

    @Test
    void testPrefixExistsAndDeleteByPrefix() {
        try (RocksDBStoreEngine engine = open("delete-prefix", false)) {
            byte[] value = "branch".getBytes(StandardCharsets.UTF_8);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 1L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 2L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-2", 1L), value);

            Assertions.assertTrue(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1")));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("missing")));

            engine.deleteByPrefix(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1"));

            Assertions.assertFalse(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1")));
            Assertions.assertTrue(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-2")));
            Assertions.assertEquals(
                    1,
                    engine.prefixScan(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-2"))
                            .size());
        }
    }

    @Test
    void testRangeDeleteByPrefixDeletesSamePrefixOnly() {
        try (RocksDBStoreEngine engine = open("delete-range-prefix", false, true)) {
            byte[] value = "branch".getBytes(StandardCharsets.UTF_8);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 1L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 2L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-2", 1L), value);

            engine.deleteByPrefix(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1"));

            Assertions.assertFalse(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1")));
            Assertions.assertTrue(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-2")));
        }
    }

    @Test
    void testRangeDeleteByBranchPrefixKeepsSameXidOtherBranches() {
        try (RocksDBStoreEngine engine = open("delete-range-branch-prefix", false, true)) {
            byte[] lockKey1 = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-1");
            byte[] lockKey2 = RocksDBKeyCodec.encodeRowLock("resource", "table", "pk-2");
            byte[] value = "lock".getBytes(StandardCharsets.UTF_8);
            engine.put(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndex("xid-1", 1L, lockKey1),
                    value);
            engine.put(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndex("xid-1", 2L, lockKey2),
                    value);

            engine.deleteByPrefix(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndexBranchPrefix("xid-1", 1L));

            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndexBranchPrefix("xid-1", 1L)));
            Assertions.assertTrue(engine.prefixExists(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndexBranchPrefix("xid-1", 2L)));
        }
    }

    @Test
    void testDeleteByPrefixFallsBackToScanWhenRangeUpperBoundUnavailable() {
        try (RocksDBStoreEngine engine = open("delete-range-fallback", false, true)) {
            byte[] value = "value".getBytes(StandardCharsets.UTF_8);
            byte[] matchingKey = new byte[] {(byte) 0xff, 1};
            byte[] otherKey = new byte[] {0, 1};
            engine.put(RocksDBColumnFamily.DEFAULT, matchingKey, value);
            engine.put(RocksDBColumnFamily.DEFAULT, otherKey, value);

            engine.deleteByPrefix(RocksDBColumnFamily.DEFAULT, new byte[] {(byte) 0xff});

            Assertions.assertNull(engine.get(RocksDBColumnFamily.DEFAULT, matchingKey));
            Assertions.assertArrayEquals(value, engine.get(RocksDBColumnFamily.DEFAULT, otherKey));
        }
    }

    @Test
    void testBatchDeleteByPrefixUsesRangeDeleteWhenEnabled() throws Exception {
        try (RocksDBStoreEngine engine = open("delete-range-batch", false, true)) {
            byte[] value = "branch".getBytes(StandardCharsets.UTF_8);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 1L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-1", 2L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-2", 1L), value);

            try (WriteBatch batch = new WriteBatch()) {
                Assertions.assertTrue(engine.deleteByPrefix(
                        batch, RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1")));
                engine.write(batch);
            }

            Assertions.assertFalse(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-1")));
            Assertions.assertTrue(
                    engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeXidPrefix("xid-2")));
        }
    }

    @Test
    void testMetadataInitialized() {
        try (RocksDBStoreEngine engine = open("metadata", true)) {
            byte[] formatVersion =
                    engine.get(RocksDBColumnFamily.METADATA, "format_version".getBytes(StandardCharsets.UTF_8));

            Assertions.assertEquals(
                    Integer.toString(RocksDBStoreEngine.FORMAT_VERSION),
                    new String(formatVersion, StandardCharsets.UTF_8));
        }
    }

    @Test
    void testPhase3IndexColumnFamiliesAvailable() {
        try (RocksDBStoreEngine engine = open("phase3-index-cf", true)) {
            byte[] statusKey = RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, 1L, "xid-index");
            byte[] transactionIdKey = RocksDBKeyCodec.encodeTransactionIdIndex(1L);
            byte[] value = "xid-index".getBytes(StandardCharsets.UTF_8);

            engine.put(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey, value);
            engine.put(RocksDBColumnFamily.TRANSACTION_ID_INDEX, transactionIdKey, value);

            Assertions.assertArrayEquals(value, engine.get(RocksDBColumnFamily.GLOBAL_STATUS_INDEX, statusKey));
            Assertions.assertArrayEquals(value, engine.get(RocksDBColumnFamily.TRANSACTION_ID_INDEX, transactionIdKey));
        }
    }

    @Test
    void testFlushPersistsEveryOpenedColumnFamily() throws Exception {
        try (RocksDBStoreEngine engine = open("flush-all-column-families", true)) {
            for (RocksDBColumnFamily columnFamily : RocksDBColumnFamily.values()) {
                engine.put(
                        columnFamily,
                        ("key-" + columnFamily.getName()).getBytes(StandardCharsets.UTF_8),
                        "value".getBytes(StandardCharsets.UTF_8));
            }

            engine.flush();

            for (RocksDBColumnFamily columnFamily : RocksDBColumnFamily.values()) {
                Assertions.assertTrue(
                        getLongProperty(engine, columnFamily, "rocksdb.total-sst-files-size") > 0,
                        () -> "column family was not flushed: " + columnFamily.getName());
            }
        }
    }

    @Test
    void testOpenRejectsUnsupportedFormatVersion() {
        RocksDBStoreConfig config = config("metadata-unsupported", true);
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "format_version".getBytes(StandardCharsets.UTF_8),
                    Integer.toString(RocksDBStoreEngine.FORMAT_VERSION + 1).getBytes(StandardCharsets.UTF_8));
        }

        StoreException exception = Assertions.assertThrows(StoreException.class, () -> RocksDBStoreEngine.open(config));

        Assertions.assertTrue(exception.getMessage().contains("unsupported RocksDB format version"));
    }

    @Test
    void testOpenRejectsInvalidFormatVersion() {
        RocksDBStoreConfig config = config("metadata-invalid", true);
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    "format_version".getBytes(StandardCharsets.UTF_8),
                    "invalid".getBytes(StandardCharsets.UTF_8));
        }

        StoreException exception = Assertions.assertThrows(StoreException.class, () -> RocksDBStoreEngine.open(config));

        Assertions.assertTrue(exception.getMessage().contains("invalid RocksDB format version metadata"));
    }

    @Test
    void testFactoryReturnsSharedInstanceForSamePath() {
        RocksDBStoreConfig config = config("factory", true);

        RocksDBStoreEngine first = RocksDBStoreEngineFactory.getInstance(config);
        RocksDBStoreEngine second = RocksDBStoreEngineFactory.getInstance(config);

        Assertions.assertSame(first, second);
        Assertions.assertTrue(first.isSyncWrite());
    }

    @Test
    void testFactoryRejectsDifferentPath() {
        RocksDBStoreEngineFactory.getInstance(config("factory-a", true));

        Assertions.assertThrows(
                StoreException.class, () -> RocksDBStoreEngineFactory.getInstance(config("factory-b", true)));
    }

    @Test
    void testFactoryRejectsDifferentSyncWrite() {
        RocksDBStoreConfig config = config("factory-sync", true);
        RocksDBStoreEngineFactory.getInstance(config);

        Assertions.assertThrows(
                StoreException.class,
                () -> RocksDBStoreEngineFactory.getInstance(new RocksDBStoreConfig(config.getDbPath(), false)));
    }

    @Test
    void testOpenWithTunedOptions() {
        RocksDBStoreConfig config = tunedConfig("tuned", true);

        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            byte[] key = RocksDBKeyCodec.encodeXid("xid-tuned");
            byte[] value = RocksDBValueCodec.encode(
                    RocksDBValueCodec.ValueType.GLOBAL_SESSION, "global".getBytes(StandardCharsets.UTF_8));

            engine.put(RocksDBColumnFamily.GLOBAL_SESSION, key, value);

            Assertions.assertArrayEquals(value, engine.get(RocksDBColumnFamily.GLOBAL_SESSION, key));
        }
    }

    @Test
    void testOpenRejectsInvalidCompressionType() {
        RocksDBStoreConfig config = tunedConfig("invalid-compression", true, "unknown");

        StoreException exception = Assertions.assertThrows(StoreException.class, () -> RocksDBStoreEngine.open(config));

        Assertions.assertTrue(exception.getMessage().contains("unsupported RocksDB compression type"));
    }

    @Test
    void testFactoryRejectsDifferentTuningOptions() {
        RocksDBStoreConfig config = tunedConfig("factory-tuning", true);
        RocksDBStoreEngineFactory.getInstance(config);

        Assertions.assertThrows(
                StoreException.class,
                () -> RocksDBStoreEngineFactory.getInstance(new RocksDBStoreConfig(config.getDbPath(), true)));
    }

    @Test
    void testRangeDeleteLeavesNoResidueAfterRestart() {
        String dbName = "range-delete-restart";
        RocksDBStoreConfig config =
                new RocksDBStoreConfig(tempDir.resolve(dbName).toString(), false, true);

        // Phase 1: write data, range delete, close
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            byte[] value = "data".getBytes(StandardCharsets.UTF_8);
            byte[] xidPrefix = RocksDBKeyCodec.encodeXidPrefix("xid-rd");

            engine.put(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid("xid-rd"), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-rd", 1L), value);
            engine.put(RocksDBColumnFamily.BRANCH_SESSION, RocksDBKeyCodec.encodeBranch("xid-rd", 2L), value);
            byte[] lockKey = RocksDBKeyCodec.encodeRowLock("res", "tbl", "pk-1");
            engine.put(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndex("xid-rd", 1L, lockKey),
                    value);
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Begin, 100L, "xid-rd"),
                    value);
            engine.put(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX,
                    RocksDBKeyCodec.encodeTransactionIdIndex(100L),
                    "xid-rd".getBytes(StandardCharsets.UTF_8));
            engine.flush();

            // range delete global + branch sessions + lock index
            engine.deleteByPrefix(RocksDBColumnFamily.GLOBAL_SESSION, xidPrefix);
            engine.deleteByPrefix(RocksDBColumnFamily.BRANCH_SESSION, xidPrefix);
            engine.deleteByPrefix(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX, RocksDBKeyCodec.encodeLockBranchIndexGlobalPrefix("xid-rd"));
            engine.flush();
        }

        // Phase 2: reopen and verify no residue
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            byte[] xidPrefix = RocksDBKeyCodec.encodeXidPrefix("xid-rd");
            Assertions.assertFalse(engine.prefixExists(RocksDBColumnFamily.GLOBAL_SESSION, xidPrefix));
            Assertions.assertFalse(engine.prefixExists(RocksDBColumnFamily.BRANCH_SESSION, xidPrefix));
            Assertions.assertFalse(engine.prefixExists(
                    RocksDBColumnFamily.LOCK_BRANCH_INDEX,
                    RocksDBKeyCodec.encodeLockBranchIndexGlobalPrefix("xid-rd")));
            // transaction ID index and global status index were NOT range-deleted, so they should remain
            Assertions.assertNotNull(engine.get(
                    RocksDBColumnFamily.TRANSACTION_ID_INDEX, RocksDBKeyCodec.encodeTransactionIdIndex(100L)));
        }
    }

    @Test
    void testMultipleOpenCloseCyclesDoNotLeakLocks() {
        String dbName = "open-close-cycles";
        RocksDBStoreConfig config =
                new RocksDBStoreConfig(tempDir.resolve(dbName).toString(), false);

        for (int cycle = 0; cycle < 3; cycle++) {
            try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
                byte[] key = RocksDBKeyCodec.encodeXid("xid-cycle-" + cycle);
                byte[] value = ("cycle-" + cycle).getBytes(StandardCharsets.UTF_8);
                engine.put(RocksDBColumnFamily.GLOBAL_SESSION, key, value);
                Assertions.assertArrayEquals(value, engine.get(RocksDBColumnFamily.GLOBAL_SESSION, key));
            }
        }

        // verify final open still works and data from last cycle is readable
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(config)) {
            byte[] key = RocksDBKeyCodec.encodeXid("xid-cycle-2");
            Assertions.assertNotNull(engine.get(RocksDBColumnFamily.GLOBAL_SESSION, key));
        }
    }

    private RocksDBStoreEngine open(String name, boolean syncWrite) {
        return RocksDBStoreEngine.open(config(name, syncWrite));
    }

    private RocksDBStoreEngine open(String name, boolean syncWrite, boolean enableRangeDelete) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), syncWrite, enableRangeDelete));
    }

    private RocksDBStoreConfig config(String name, boolean syncWrite) {
        return new RocksDBStoreConfig(tempDir.resolve(name).toString(), syncWrite);
    }

    private long getLongProperty(RocksDBStoreEngine engine, RocksDBColumnFamily columnFamily, String property)
            throws ReflectiveOperationException, RocksDBException {
        Field dbField = RocksDBStoreEngine.class.getDeclaredField("db");
        dbField.setAccessible(true);
        RocksDB db = (RocksDB) dbField.get(engine);
        return db.getLongProperty(engine.handle(columnFamily), property);
    }

    private RocksDBStoreConfig tunedConfig(String name, boolean syncWrite) {
        return tunedConfig(name, syncWrite, "no");
    }

    private RocksDBStoreConfig tunedConfig(String name, boolean syncWrite, String compressionType) {
        return new RocksDBStoreConfig(
                tempDir.resolve(name).toString(),
                syncWrite,
                1024L * 1024L,
                1024L * 1024L,
                2,
                1,
                2,
                64,
                1024L * 1024L,
                4,
                8,
                12,
                true,
                true,
                compressionType);
    }
}
