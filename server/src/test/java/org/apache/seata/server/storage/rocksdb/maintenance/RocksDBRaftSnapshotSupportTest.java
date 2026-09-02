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
package org.apache.seata.server.storage.rocksdb.maintenance;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.rocksdb.RocksDBColumnFamily;
import org.apache.seata.server.storage.rocksdb.RocksDBKeyCodec;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreConfig;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngine;
import org.apache.seata.server.storage.rocksdb.store.RocksDBTransactionStoreManager;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;
import org.rocksdb.Options;
import org.rocksdb.RocksDB;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class RocksDBRaftSnapshotSupportTest {

    @TempDir
    Path tempDir;

    private Object originalEnvironment;

    @BeforeEach
    void beforeEach() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
    }

    @AfterEach
    void afterEach() throws Exception {
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    // ---- saveSnapshot tests ----

    @Test
    void testSaveSnapshotCreatesCheckpointAndMetadata() {
        try (RocksDBStoreEngine engine = open("snap-save")) {
            writeGlobal(engine, "tx-snap-1", GlobalStatus.Begin);

            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-out");
            support.saveSnapshot(snapshotDir, true);

            Assertions.assertTrue(Files.exists(snapshotDir.resolve("seata-checkpoint-metadata.txt")));
            Assertions.assertTrue(Files.exists(snapshotDir));
            // Snapshot directory should contain RocksDB files (at least CURRENT)
            Assertions.assertTrue(Files.exists(snapshotDir.resolve("CURRENT")));
        }
    }

    @Test
    void testSaveSnapshotRejectsNullPath() {
        try (RocksDBStoreEngine engine = open("snap-null")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Assertions.assertThrows(StoreException.class, () -> support.saveSnapshot(null, false));
        }
    }

    // ---- getSnapshotMetadata tests ----

    @Test
    void testGetSnapshotMetadataReadsAllFields() {
        try (RocksDBStoreEngine engine = open("snap-meta")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-meta-out");
            support.saveSnapshot(snapshotDir, false);

            Map<String, String> metadata = support.getSnapshotMetadata(snapshotDir);
            Assertions.assertTrue(metadata.containsKey("sourceDbPath"));
            Assertions.assertEquals(Integer.toString(RocksDBStoreEngine.FORMAT_VERSION), metadata.get("formatVersion"));
            Assertions.assertTrue(metadata.get("columnFamilies").contains("global_session"));
            Assertions.assertTrue(metadata.get("columnFamilies").contains("branch_session"));
            Assertions.assertTrue(metadata.containsKey("rocksdbVersion"));
            Assertions.assertTrue(metadata.containsKey("createdAt"));
            Assertions.assertTrue(metadata.containsKey("syncWrite"));
        }
    }

    @Test
    void testGetSnapshotMetadataFailsForMissingFile() {
        try (RocksDBStoreEngine engine = open("snap-nometa")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path emptyDir = tempDir.resolve("empty-dir");
            try {
                Files.createDirectories(emptyDir);
            } catch (IOException e) {
                Assertions.fail(e);
            }
            Assertions.assertThrows(StoreException.class, () -> support.getSnapshotMetadata(emptyDir));
        }
    }

    // ---- validateSnapshotCompatibility tests ----

    @Test
    void testValidateSnapshotCompatibilityPassesForValidSnapshot() {
        try (RocksDBStoreEngine engine = open("snap-valid")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-valid-out");
            support.saveSnapshot(snapshotDir, false);

            // Should not throw
            support.validateSnapshotCompatibility(snapshotDir);
        }
    }

    @Test
    void testValidateSnapshotCompatibilityFailsOnVersionMismatch() throws IOException {
        try (RocksDBStoreEngine engine = open("snap-version")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-version-out");
            support.saveSnapshot(snapshotDir, false);

            // Tamper with the metadata to have a wrong format version
            Path metadataFile = snapshotDir.resolve("seata-checkpoint-metadata.txt");
            String content = new String(Files.readAllBytes(metadataFile), StandardCharsets.UTF_8);
            content = content.replace("formatVersion=" + RocksDBStoreEngine.FORMAT_VERSION, "formatVersion=999");
            Files.write(metadataFile, content.getBytes(StandardCharsets.UTF_8));

            Assertions.assertThrows(StoreException.class, () -> support.validateSnapshotCompatibility(snapshotDir));
        }
    }

    @Test
    void testValidateSnapshotCompatibilityFailsOnMissingMetadata() {
        try (RocksDBStoreEngine engine = open("snap-missing")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path emptyDir = tempDir.resolve("missing-meta-dir");
            try {
                Files.createDirectories(emptyDir);
            } catch (IOException e) {
                Assertions.fail(e);
            }
            Assertions.assertThrows(StoreException.class, () -> support.validateSnapshotCompatibility(emptyDir));
        }
    }

    // ---- replaceLocalDbFromSnapshot tests ----

    @Test
    void testReplaceLocalDbFromSnapshotCopiesFiles() {
        String snapshotXid;
        try (RocksDBStoreEngine engine = open("snap-replace-src")) {
            GlobalSession global = writeGlobal(engine, "tx-replace", GlobalStatus.Begin);
            snapshotXid = global.getXid();

            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-replace-out");
            support.saveSnapshot(snapshotDir, true);
        }

        // Create target DB with some initial data
        String oldXid;
        Path targetDir = tempDir.resolve("target-db");
        try (RocksDBStoreEngine targetEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(targetDir.toString(), true))) {
            GlobalSession oldGlobal = writeGlobal(targetEngine, "tx-old", GlobalStatus.Begin);
            oldXid = oldGlobal.getXid();
        }

        // Replace target from snapshot (open engine on snapshot dir to avoid temp dir LOCK leak)
        Path snapshotDir = tempDir.resolve("snapshot-replace-out");
        try (RocksDBStoreEngine snapshotEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(snapshotDir.toString(), true))) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(snapshotEngine);
            support.replaceLocalDbFromSnapshot(snapshotDir, targetDir);
        }

        // Reopen target and verify it has snapshot data, not old data
        try (RocksDBStoreEngine reopenedEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(targetDir.toString(), true))) {
            byte[] newValue =
                    reopenedEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(snapshotXid));
            Assertions.assertNotNull(newValue, "snapshot data should be present after replace");

            byte[] oldValue = reopenedEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(oldXid));
            Assertions.assertNull(oldValue, "old data should be gone after replace");
        }
    }

    @Test
    void testReplaceLocalDbFailsForMissingSnapshotDir() {
        Path missingDir = tempDir.resolve("nonexistent-snapshot");
        Path targetDir = tempDir.resolve("target-for-missing");
        try {
            Files.createDirectories(targetDir);
        } catch (IOException e) {
            Assertions.fail(e);
        }

        try (RocksDBStoreEngine engine = open("snap-replace-fail")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Assertions.assertThrows(
                    StoreException.class, () -> support.replaceLocalDbFromSnapshot(missingDir, targetDir));
        }
    }

    @Test
    void testReplaceLocalDbFailsForMissingTargetDir() {
        try (RocksDBStoreEngine engine = open("snap-replace-no-target")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-for-no-target");
            support.saveSnapshot(snapshotDir, false);

            Path missingTarget = tempDir.resolve("nonexistent-target");
            Assertions.assertThrows(
                    StoreException.class, () -> support.replaceLocalDbFromSnapshot(snapshotDir, missingTarget));
        }
    }

    // ---- openFromSnapshot tests ----

    @Test
    void testOpenFromSnapshotReadsData() {
        try (RocksDBStoreEngine engine = open("snap-open-src")) {
            GlobalSession global = writeGlobal(engine, "tx-open-snap", GlobalStatus.Committing);
            String xid = global.getXid();

            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            Path snapshotDir = tempDir.resolve("snapshot-open-out");
            support.saveSnapshot(snapshotDir, true);

            try (RocksDBStoreEngine snapshotEngine = support.openFromSnapshot(snapshotDir, true)) {
                byte[] value = snapshotEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid));
                Assertions.assertNotNull(value, "data should be readable from snapshot");
            }
        }
    }

    // ---- loadSnapshot (full flow) tests ----

    @Test
    void testLoadSnapshotFullFlow() {
        // Step 1: Create source DB with data and take snapshot
        String xid1;
        String xid2;
        Path sourceDir = tempDir.resolve("load-source");
        try (RocksDBStoreEngine sourceEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(sourceDir.toString(), true))) {
            GlobalSession global1 = writeGlobal(sourceEngine, "tx-load-1", GlobalStatus.Begin);
            GlobalSession global2 = writeGlobal(sourceEngine, "tx-load-2", GlobalStatus.Committing);
            xid1 = global1.getXid();
            xid2 = global2.getXid();

            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(sourceEngine);
            Path snapshotDir = tempDir.resolve("snapshot-load-out");
            support.saveSnapshot(snapshotDir, true);
        }

        // Step 2: Create target DB with different data
        String oldXid;
        Path targetDir = tempDir.resolve("load-target");
        try (RocksDBStoreEngine targetEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(targetDir.toString(), true))) {
            GlobalSession oldGlobal = writeGlobal(targetEngine, "tx-target-old", GlobalStatus.Begin);
            oldXid = oldGlobal.getXid();
        }

        // Step 3: Load snapshot into target (validate + replace, open engine on snapshot dir)
        Path snapshotDir = tempDir.resolve("snapshot-load-out");
        try (RocksDBStoreEngine snapshotEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(snapshotDir.toString(), true))) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(snapshotEngine);
            support.loadSnapshot(snapshotDir, targetDir);
        }

        // Step 4: Reopen target and verify
        try (RocksDBStoreEngine reopenedEngine =
                RocksDBStoreEngine.open(new RocksDBStoreConfig(targetDir.toString(), true))) {
            byte[] value1 = reopenedEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid1));
            Assertions.assertNotNull(value1, "tx-load-1 should be present after snapshot load");

            byte[] value2 = reopenedEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(xid2));
            Assertions.assertNotNull(value2, "tx-load-2 should be present after snapshot load");

            byte[] oldValue = reopenedEngine.get(RocksDBColumnFamily.GLOBAL_SESSION, RocksDBKeyCodec.encodeXid(oldXid));
            Assertions.assertNull(oldValue, "old target data should be gone after snapshot load");
        }
    }

    @Test
    void testLoadSnapshotRejectsSameSourceAndTargetWithoutChangingTarget() {
        Path targetDir = tempDir.resolve("load-overlap-same");
        byte[] key = "original-key".getBytes(StandardCharsets.UTF_8);
        byte[] originalValue = "original-value".getBytes(StandardCharsets.UTF_8);

        try (RocksDBStoreEngine engine = open("load-overlap-same-source")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            support.saveSnapshot(targetDir, true);
            putMetadata(targetDir, key, originalValue);

            Assertions.assertThrows(StoreException.class, () -> support.loadSnapshot(targetDir, targetDir));
        }

        assertMetadataValue(targetDir, key, originalValue);
    }

    @Test
    void testLoadSnapshotRejectsSourceContainingTargetWithoutChangingTarget() {
        Path snapshotDir = tempDir.resolve("load-source-parent");
        Path targetDir = snapshotDir.resolve("target");
        byte[] key = "original-key".getBytes(StandardCharsets.UTF_8);
        byte[] originalValue = "original-value".getBytes(StandardCharsets.UTF_8);
        putMetadata(targetDir, key, originalValue);

        try (RocksDBStoreEngine engine = open("load-source-parent-source")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);

            StoreException exception =
                    Assertions.assertThrows(StoreException.class, () -> support.loadSnapshot(snapshotDir, targetDir));
            Assertions.assertTrue(exception.getMessage().contains("overlap"));
        }

        assertMetadataValue(targetDir, key, originalValue);
    }

    @Test
    void testLoadSnapshotRejectsTargetContainingSourceWithoutChangingTarget() {
        Path targetDir = tempDir.resolve("load-target-parent");
        Path snapshotDir = targetDir.resolve("snapshot");
        byte[] key = "original-key".getBytes(StandardCharsets.UTF_8);
        byte[] originalValue = "original-value".getBytes(StandardCharsets.UTF_8);
        putMetadata(targetDir, key, originalValue);

        try (RocksDBStoreEngine engine = open("load-target-parent-source")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            support.saveSnapshot(snapshotDir, true);

            Assertions.assertThrows(StoreException.class, () -> support.loadSnapshot(snapshotDir, targetDir));
        }

        assertMetadataValue(targetDir, key, originalValue);
    }

    @Test
    void testLoadSnapshotValidatesStagingBeforeChangingTarget() throws IOException {
        Path snapshotDir = tempDir.resolve("load-invalid-snapshot");
        Path targetDir = tempDir.resolve("load-invalid-target");
        byte[] key = "original-key".getBytes(StandardCharsets.UTF_8);
        byte[] originalValue = "original-value".getBytes(StandardCharsets.UTF_8);
        putMetadata(targetDir, key, originalValue);

        try (RocksDBStoreEngine engine = open("load-invalid-source")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            support.saveSnapshot(snapshotDir, true);
            Files.delete(snapshotDir.resolve("CURRENT"));

            Assertions.assertThrows(StoreException.class, () -> support.loadSnapshot(snapshotDir, targetDir));
        }

        assertMetadataValue(targetDir, key, originalValue);
    }

    @Test
    @Timeout(value = 10, unit = TimeUnit.SECONDS)
    void testLoadSnapshotRejectsRealPathAliasWithoutChangingTarget() throws IOException {
        Path targetDir = tempDir.resolve("load-alias-target");
        Path snapshotAlias = tempDir.resolve("load-alias-snapshot");
        byte[] key = "original-key".getBytes(StandardCharsets.UTF_8);
        byte[] originalValue = "original-value".getBytes(StandardCharsets.UTF_8);

        try (RocksDBStoreEngine engine = open("load-alias-source")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            support.saveSnapshot(targetDir, true);
            putMetadata(targetDir, key, originalValue);
            try {
                Files.createSymbolicLink(snapshotAlias, targetDir);
            } catch (IOException | UnsupportedOperationException | SecurityException e) {
                Assumptions.assumeTrue(false, "symbolic links are not available: " + e.getMessage());
            }

            Assertions.assertThrows(StoreException.class, () -> support.loadSnapshot(snapshotAlias, targetDir));
        }

        assertMetadataValue(targetDir, key, originalValue);
    }

    @Test
    void testLoadSnapshotRejectsMissingColumnFamilyWithoutChangingTarget() throws IOException {
        Path validSnapshotDir = tempDir.resolve("load-valid-cf-snapshot");
        Path incompleteSnapshotDir = tempDir.resolve("load-missing-cf-snapshot");
        Path targetDir = tempDir.resolve("load-missing-cf-target");
        byte[] key = "original-key".getBytes(StandardCharsets.UTF_8);
        byte[] originalValue = "original-value".getBytes(StandardCharsets.UTF_8);
        putMetadata(targetDir, key, originalValue);

        try (RocksDBStoreEngine engine = open("load-missing-cf-source")) {
            RocksDBRaftSnapshotSupport support = new RocksDBRaftSnapshotSupport(engine);
            support.saveSnapshot(validSnapshotDir, false);
            try (Options options = new Options().setCreateIfMissing(true);
                    RocksDB ignored = RocksDB.open(options, incompleteSnapshotDir.toString())) {
                Assertions.assertEquals(
                        1,
                        RocksDB.listColumnFamilies(options, incompleteSnapshotDir.toString())
                                .size());
            } catch (org.rocksdb.RocksDBException e) {
                Assertions.fail(e);
            }
            Files.copy(
                    validSnapshotDir.resolve("seata-checkpoint-metadata.txt"),
                    incompleteSnapshotDir.resolve("seata-checkpoint-metadata.txt"),
                    StandardCopyOption.REPLACE_EXISTING);

            Assertions.assertThrows(StoreException.class, () -> support.loadSnapshot(incompleteSnapshotDir, targetDir));
        }

        assertMetadataValue(targetDir, key, originalValue);
    }

    // ---- Engine read-only access API tests ----

    @Test
    void testEngineGetDbPath() {
        try (RocksDBStoreEngine engine = open("api-dbpath")) {
            Assertions.assertNotNull(engine.getDbPath());
            Assertions.assertTrue(engine.getDbPath().contains("api-dbpath"));
        }
    }

    @Test
    void testEngineGetColumnFamilyNames() {
        try (RocksDBStoreEngine engine = open("api-cfnames")) {
            java.util.List<String> names = engine.getColumnFamilyNames();
            Assertions.assertNotNull(names);
            Assertions.assertFalse(names.isEmpty());
            Assertions.assertTrue(names.contains("default"));
            Assertions.assertTrue(names.contains("metadata"));
            Assertions.assertTrue(names.contains("global_session"));
            Assertions.assertTrue(names.contains("branch_session"));
            Assertions.assertTrue(names.contains("lock"));
            Assertions.assertTrue(names.contains("lock_branch_index"));
            Assertions.assertTrue(names.contains("global_status_index"));
            Assertions.assertTrue(names.contains("transaction_id_index"));
            Assertions.assertEquals(RocksDBColumnFamily.values().length, names.size());
            // Should be unmodifiable
            Assertions.assertThrows(UnsupportedOperationException.class, () -> names.add("extra"));
        }
    }

    @Test
    void testEngineIsClosed() {
        RocksDBStoreEngine engine = open("api-closed");
        Assertions.assertFalse(engine.isClosed());
        engine.close();
        Assertions.assertTrue(engine.isClosed());
    }

    // ---- Helper methods ----

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true));
    }

    private GlobalSession writeGlobal(RocksDBStoreEngine engine, String name, GlobalStatus status) {
        GlobalSession global = new GlobalSession("app", "group", name, 60000);
        global.setStatus(status);
        RocksDBTransactionStoreManager storeManager = new RocksDBTransactionStoreManager(engine);
        storeManager.writeSession(LogOperation.GLOBAL_ADD, global);
        return global;
    }

    private void putMetadata(Path dbDir, byte[] key, byte[] value) {
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(new RocksDBStoreConfig(dbDir.toString(), true))) {
            engine.put(RocksDBColumnFamily.METADATA, key, value);
        }
    }

    private void assertMetadataValue(Path dbDir, byte[] key, byte[] expectedValue) {
        try (RocksDBStoreEngine engine = RocksDBStoreEngine.open(new RocksDBStoreConfig(dbDir.toString(), true))) {
            Assertions.assertArrayEquals(expectedValue, engine.get(RocksDBColumnFamily.METADATA, key));
        }
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment() throws Exception {
        java.lang.reflect.Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        java.util.Map<String, Object> objectMap = (java.util.Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }
}
