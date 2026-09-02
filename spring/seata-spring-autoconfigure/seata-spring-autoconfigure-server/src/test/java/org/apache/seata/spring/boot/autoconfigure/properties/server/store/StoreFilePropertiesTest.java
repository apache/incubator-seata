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
package org.apache.seata.spring.boot.autoconfigure.properties.server.store;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class StoreFilePropertiesTest {

    @Test
    public void testStoreFileProperties() {
        StoreFileProperties storeFileProperties = new StoreFileProperties();
        storeFileProperties.setDir("dir");
        storeFileProperties.setEngine("rocksdb");
        storeFileProperties.setFlushDiskMode("disk");
        storeFileProperties.setFileWriteBufferCacheSize(1);
        storeFileProperties.setMaxBranchSessionSize(1);
        storeFileProperties.setMaxGlobalSessionSize(1);
        storeFileProperties.setSessionReloadReadSize(1);
        StoreFileProperties.RocksDB rocksDB = new StoreFileProperties.RocksDB();
        rocksDB.setDir("rocksdb-dir");

        Assertions.assertEquals("dir", storeFileProperties.getDir());
        Assertions.assertEquals("rocksdb", storeFileProperties.getEngine());
        Assertions.assertEquals("disk", storeFileProperties.getFlushDiskMode());
        Assertions.assertEquals(1, storeFileProperties.getFileWriteBufferCacheSize());
        Assertions.assertEquals(1, storeFileProperties.getMaxGlobalSessionSize());
        Assertions.assertEquals(1, storeFileProperties.getMaxBranchSessionSize());
        Assertions.assertEquals(1, storeFileProperties.getSessionReloadReadSize());
        Assertions.assertEquals("rocksdb-dir", rocksDB.getDir());
    }

    @Test
    public void testRocksDBDefaultsMatchRuntimeConfiguration() {
        StoreFileProperties.RocksDB rocksDB = new StoreFileProperties.RocksDB();

        Assertions.assertTrue(rocksDB.getEnableRangeDelete());
    }

    @Test
    public void testRocksDBPhase4PropertiesAreExposed() throws Exception {
        StoreFileProperties.RocksDB rocksDB = new StoreFileProperties.RocksDB();
        Map<String, Object> expectedDefaults = new LinkedHashMap<>();
        expectedDefaults.put("dbWriteBufferSize", "0");
        expectedDefaults.put("maxTotalWalSize", "0");
        expectedDefaults.put("globalWriteBufferSize", "0");
        expectedDefaults.put("branchWriteBufferSize", "0");
        expectedDefaults.put("lockWriteBufferSize", "0");
        expectedDefaults.put("indexWriteBufferSize", "0");
        expectedDefaults.put("metadataWriteBufferSize", "0");
        expectedDefaults.put("fullScanMaxLimit", 10000);
        expectedDefaults.put("fullScanDeadlineMillis", 5000L);
        expectedDefaults.put("multiStatusScanPageSize", 256);
        expectedDefaults.put("walSyncShutdownTimeoutMillis", 30000);
        expectedDefaults.put("orphanLockCleanEnabled", true);
        expectedDefaults.put("orphanLockCleanIntervalMillis", 60000L);
        expectedDefaults.put("orphanLockCleanBatchLimit", 1000);
        expectedDefaults.put("orphanLockCleanMaxBatches", 2);
        expectedDefaults.put("orphanLockCleanRoundSleepMillis", 100L);

        Map<String, PropertyDescriptor> properties = Arrays.stream(
                        Introspector.getBeanInfo(StoreFileProperties.RocksDB.class)
                                .getPropertyDescriptors())
                .collect(Collectors.toMap(PropertyDescriptor::getName, Function.identity()));

        for (Map.Entry<String, Object> expected : expectedDefaults.entrySet()) {
            PropertyDescriptor property = properties.get(expected.getKey());
            Assertions.assertNotNull(property, expected.getKey() + " should be exposed as a Spring property");
            Assertions.assertNotNull(property.getReadMethod(), expected.getKey() + " should have a getter");
            String setterName = "set" + Character.toUpperCase(expected.getKey().charAt(0))
                    + expected.getKey().substring(1);
            Assertions.assertDoesNotThrow(
                    () -> StoreFileProperties.RocksDB.class.getMethod(
                            setterName, property.getReadMethod().getReturnType()),
                    expected.getKey() + " should have a fluent setter");
            Assertions.assertEquals(
                    expected.getValue(), property.getReadMethod().invoke(rocksDB));
        }
    }
}
