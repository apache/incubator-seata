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
package org.apache.seata.server.store;

import org.apache.seata.common.store.LockMode;
import org.apache.seata.common.store.SessionMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * StoreConfig Test - Direct testing without mocks
 */
@DisplayName("StoreConfig Test")
class StoreConfigTest {

    @BeforeEach
    void setUp() {
        // Reset to default state
        StoreConfig.setStartupParameter(null, null, null);
    }

    @Test
    @DisplayName("test setStartupParameter with storeMode")
    void testSetStartupParameterWithStoreMode() {
        StoreConfig.setStartupParameter("file", null, null);
        // Parameter is set, getSessionMode should use it
        assertNotNull(StoreConfig.getSessionMode());
    }

    @Test
    @DisplayName("test setStartupParameter with sessionMode")
    void testSetStartupParameterWithSessionMode() {
        StoreConfig.setStartupParameter(null, "file", null);
        SessionMode sessionMode = StoreConfig.getSessionMode();
        assertEquals(SessionMode.FILE, sessionMode);
    }

    @Test
    @DisplayName("test setStartupParameter with lockMode")
    void testSetStartupParameterWithLockMode() {
        StoreConfig.setStartupParameter(null, null, "file");
        LockMode lockMode = StoreConfig.getLockMode();
        assertEquals(LockMode.FILE, lockMode);
    }

    @Test
    @DisplayName("test setStartupParameter with all modes")
    void testSetStartupParameterWithAllModes() {
        StoreConfig.setStartupParameter("db", "db", "db");

        SessionMode sessionMode = StoreConfig.getSessionMode();
        LockMode lockMode = StoreConfig.getLockMode();

        assertEquals(SessionMode.DB, sessionMode);
        assertEquals(LockMode.DB, lockMode);
    }

    @Test
    @DisplayName("test getMaxBranchSessionSize default")
    void testGetMaxBranchSessionSizeDefault() {
        int size = StoreConfig.getMaxBranchSessionSize();
        assertTrue(size > 0);
    }

    @Test
    @DisplayName("test getMaxGlobalSessionSize default")
    void testGetMaxGlobalSessionSizeDefault() {
        int size = StoreConfig.getMaxGlobalSessionSize();
        assertTrue(size > 0);
    }

    @Test
    @DisplayName("test getFileWriteBufferCacheSize default")
    void testGetFileWriteBufferCacheSizeDefault() {
        int size = StoreConfig.getFileWriteBufferCacheSize();
        assertTrue(size > 0);
    }

    @Test
    @DisplayName("test getFlushDiskMode default")
    void testGetFlushDiskModeDefault() {
        assertNotNull(StoreConfig.getFlushDiskMode());
    }

    @Test
    @DisplayName("test getSessionMode with blank input")
    void testGetSessionModeWithBlankInput() {
        StoreConfig.setStartupParameter(null, "", null);
        assertNotNull(StoreConfig.getSessionMode());
    }

    @Test
    @DisplayName("test getLockMode with blank input")
    void testGetLockModeWithBlankInput() {
        StoreConfig.setStartupParameter(null, null, "");
        assertNotNull(StoreConfig.getLockMode());
    }

    @Test
    @DisplayName("test setStartupParameter with null values")
    void testSetStartupParameterWithNullValues() {
        StoreConfig.setStartupParameter(null, null, null);

        assertNotNull(StoreConfig.getSessionMode());
        assertNotNull(StoreConfig.getLockMode());
    }

    @Test
    @DisplayName("test different store modes")
    void testDifferentStoreModes() {
        // Test DB mode
        StoreConfig.setStartupParameter("db", "db", "db");
        assertEquals(SessionMode.DB, StoreConfig.getSessionMode());
        assertEquals(LockMode.DB, StoreConfig.getLockMode());

        // Reset and test FILE mode
        StoreConfig.setStartupParameter("file", "file", "file");
        assertEquals(SessionMode.FILE, StoreConfig.getSessionMode());
        assertEquals(LockMode.FILE, StoreConfig.getLockMode());
    }

    @Test
    @DisplayName("test redis store mode")
    void testRedisStoreMode() {
        StoreConfig.setStartupParameter("redis", "redis", "redis");
        assertEquals(SessionMode.REDIS, StoreConfig.getSessionMode());
        assertEquals(LockMode.REDIS, StoreConfig.getLockMode());
    }

    @Test
    @DisplayName("test raft store mode")
    void testRaftStoreMode() {
        StoreConfig.setStartupParameter("raft", "raft", "raft");
        assertEquals(SessionMode.RAFT, StoreConfig.getSessionMode());
        assertEquals(LockMode.RAFT, StoreConfig.getLockMode());
    }

    @Test
    @DisplayName("test mixed store modes")
    void testMixedStoreModes() {
        StoreConfig.setStartupParameter("file", "db", "redis");
        assertEquals(SessionMode.DB, StoreConfig.getSessionMode());
        assertEquals(LockMode.REDIS, StoreConfig.getLockMode());
    }

    @Test
    @DisplayName("test size configurations are positive")
    void testSizeConfigurationsArePositive() {
        int branchSize = StoreConfig.getMaxBranchSessionSize();
        int globalSize = StoreConfig.getMaxGlobalSessionSize();
        int bufferSize = StoreConfig.getFileWriteBufferCacheSize();

        assertTrue(branchSize > 0, "Branch session size should be positive");
        assertTrue(globalSize > 0, "Global session size should be positive");
        assertTrue(bufferSize > 0, "Buffer cache size should be positive");
    }
}
