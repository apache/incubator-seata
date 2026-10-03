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
package org.apache.seata.server.storage.rocksdb.index;

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
import org.apache.seata.server.storage.rocksdb.RocksDBValueCodec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class RocksDBIndexManagerTest {

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

    @Test
    void testRebuildsIndexesFromGlobalSessions() {
        try (RocksDBStoreEngine engine = open("rebuild")) {
            GlobalSession begin = globalSession("tx-begin", GlobalStatus.Begin);
            GlobalSession committing = globalSession("tx-committing", GlobalStatus.Committing);
            putGlobal(engine, begin);
            putGlobal(engine, committing);

            RocksDBIndexManager indexManager = new RocksDBIndexManager(engine);
            indexManager.ensureReady();

            Assertions.assertEquals(begin.getXid(), indexManager.findXidByTransactionId(begin.getTransactionId()));
            Assertions.assertEquals(
                    committing.getXid(), indexManager.findXidByTransactionId(committing.getTransactionId()));
            Assertions.assertEquals(
                    1, indexManager.scanXidsByStatus(GlobalStatus.Begin).size());
            Assertions.assertEquals(
                    begin.getXid(),
                    indexManager.scanXidsByStatus(GlobalStatus.Begin).get(0));
            List<String> streamedBeginXids = new ArrayList<>();
            indexManager.scanXidsByStatus(GlobalStatus.Begin, streamedBeginXids::add);
            Assertions.assertEquals(1, streamedBeginXids.size());
            Assertions.assertEquals(begin.getXid(), streamedBeginXids.get(0));
            Assertions.assertEquals(
                    RocksDBIndexManager.INDEX_BUILD_STATUS_COMPLETED,
                    getMetadata(engine, RocksDBIndexManager.INDEX_BUILD_STATUS_KEY));
        }
    }

    @Test
    void testInProgressIndexBuildIsClearedAndRebuilt() {
        try (RocksDBStoreEngine engine = open("in-progress")) {
            GlobalSession active = globalSession("tx-active", GlobalStatus.Begin);
            putGlobal(engine, active);
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    bytes(RocksDBIndexManager.INDEX_BUILD_STATUS_KEY),
                    bytes(RocksDBIndexManager.INDEX_BUILD_STATUS_IN_PROGRESS));
            engine.put(
                    RocksDBColumnFamily.GLOBAL_STATUS_INDEX,
                    RocksDBKeyCodec.encodeGlobalStatusIndex(GlobalStatus.Committed, 1L, "stale"),
                    bytes("stale"));

            RocksDBIndexManager indexManager = new RocksDBIndexManager(engine);
            indexManager.ensureReady();

            List<String> committedXids = indexManager.scanXidsByStatus(GlobalStatus.Committed);
            Assertions.assertTrue(committedXids.isEmpty());
            Assertions.assertEquals(
                    active.getXid(),
                    indexManager.scanXidsByStatus(GlobalStatus.Begin).get(0));
        }
    }

    @Test
    void testScanXidsByStatusBeforeBeginTimeDeadlineStopsAtBoundary() {
        try (RocksDBStoreEngine engine = open("bounded-status")) {
            GlobalSession old = globalSession("tx-old", GlobalStatus.Begin);
            old.setBeginTime(100L);
            GlobalSession deadline = globalSession("tx-deadline", GlobalStatus.Begin);
            deadline.setBeginTime(200L);
            GlobalSession future = globalSession("tx-future", GlobalStatus.Begin);
            future.setBeginTime(300L);
            GlobalSession otherStatus = globalSession("tx-other-status", GlobalStatus.Committing);
            otherStatus.setBeginTime(50L);
            putGlobal(engine, old);
            putGlobal(engine, deadline);
            putGlobal(engine, future);
            putGlobal(engine, otherStatus);

            RocksDBIndexManager indexManager = new RocksDBIndexManager(engine);
            indexManager.ensureReady();

            RocksDBIndexManager.StatusScanResult result = indexManager.scanXidsByStatus(GlobalStatus.Begin, 200L, 10);

            Assertions.assertEquals(List.of(old.getXid(), deadline.getXid()), result.getXids());
            Assertions.assertEquals(3, result.getRowsScanned());
            Assertions.assertEquals(2, result.getRowsReturned());
            Assertions.assertFalse(result.isLimitReached());
        }
    }

    @Test
    void testScanXidsByStatusUsesLowerBoundAndCursor() {
        try (RocksDBStoreEngine engine = open("paged-status")) {
            GlobalSession beforeLowerBound = globalSession("tx-before-lower", GlobalStatus.Begin);
            beforeLowerBound.setBeginTime(100L);
            GlobalSession first = globalSession("tx-first-page", GlobalStatus.Begin);
            first.setBeginTime(200L);
            GlobalSession second = globalSession("tx-second-page", GlobalStatus.Begin);
            second.setBeginTime(300L);
            GlobalSession third = globalSession("tx-third-page", GlobalStatus.Begin);
            third.setBeginTime(400L);
            GlobalSession afterUpperBound = globalSession("tx-after-upper", GlobalStatus.Begin);
            afterUpperBound.setBeginTime(500L);
            putGlobal(engine, beforeLowerBound);
            putGlobal(engine, first);
            putGlobal(engine, second);
            putGlobal(engine, third);
            putGlobal(engine, afterUpperBound);

            RocksDBIndexManager indexManager = new RocksDBIndexManager(engine);
            indexManager.ensureReady();

            RocksDBIndexManager.StatusScanResult firstPage =
                    indexManager.scanXidsByStatus(GlobalStatus.Begin, 200L, 400L, null, 2);

            Assertions.assertEquals(List.of(first.getXid(), second.getXid()), firstPage.getXids());
            Assertions.assertEquals(2, firstPage.getRowsScanned());
            Assertions.assertEquals(2, firstPage.getRowsReturned());
            Assertions.assertTrue(firstPage.isLimitReached());
            Assertions.assertNotNull(firstPage.getNextCursor());

            RocksDBIndexManager.StatusScanResult secondPage =
                    indexManager.scanXidsByStatus(GlobalStatus.Begin, 200L, 400L, firstPage.getNextCursor(), 2);

            Assertions.assertEquals(List.of(third.getXid()), secondPage.getXids());
            Assertions.assertEquals(2, secondPage.getRowsScanned());
            Assertions.assertEquals(1, secondPage.getRowsReturned());
            Assertions.assertFalse(secondPage.isLimitReached());
            Assertions.assertNull(secondPage.getNextCursor());
        }
    }

    @Test
    void testFutureIndexVersionFailsFast() {
        try (RocksDBStoreEngine engine = open("future")) {
            engine.put(
                    RocksDBColumnFamily.METADATA,
                    bytes(RocksDBIndexManager.INDEX_VERSION_KEY),
                    bytes(Integer.toString(RocksDBIndexManager.INDEX_VERSION + 1)));

            StoreException exception =
                    Assertions.assertThrows(StoreException.class, () -> new RocksDBIndexManager(engine).ensureReady());
            Assertions.assertTrue(exception.getMessage().contains("unsupported RocksDB index version"));
        }
    }

    private RocksDBStoreEngine open(String name) {
        return RocksDBStoreEngine.open(
                new RocksDBStoreConfig(tempDir.resolve(name).toString(), true));
    }

    private GlobalSession globalSession(String name, GlobalStatus status) {
        GlobalSession globalSession = new GlobalSession("app", "group", name, 60000);
        globalSession.setStatus(status);
        return globalSession;
    }

    private void putGlobal(RocksDBStoreEngine engine, GlobalSession globalSession) {
        engine.put(
                RocksDBColumnFamily.GLOBAL_SESSION,
                RocksDBKeyCodec.encodeXid(globalSession.getXid()),
                RocksDBValueCodec.encode(RocksDBValueCodec.ValueType.GLOBAL_SESSION, globalSession.encode()));
    }

    private String getMetadata(RocksDBStoreEngine engine, String key) {
        byte[] value = engine.get(RocksDBColumnFamily.METADATA, bytes(key));
        return value == null ? null : new String(value, StandardCharsets.UTF_8);
    }

    private byte[] bytes(String value) {
        return value.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private void restoreEnvironment() throws Exception {
        Field field = ObjectHolder.class.getDeclaredField("OBJECT_MAP");
        field.setAccessible(true);
        Map<String, Object> objectMap = (Map<String, Object>) field.get(ObjectHolder.INSTANCE);
        if (originalEnvironment == null) {
            objectMap.remove(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        } else {
            objectMap.put(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, originalEnvironment);
        }
    }
}
