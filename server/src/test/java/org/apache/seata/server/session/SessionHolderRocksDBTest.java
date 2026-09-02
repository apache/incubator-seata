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
package org.apache.seata.server.session;

import org.apache.seata.common.Constants;
import org.apache.seata.common.XID;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.storage.file.TransactionWriteStore;
import org.apache.seata.server.storage.rocksdb.RocksDBStoreEngineFactory;
import org.apache.seata.server.storage.rocksdb.session.RocksDBSessionManager;
import org.apache.seata.server.store.SessionStorable;
import org.apache.seata.server.store.TransactionStoreManager.LogOperation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.env.MockEnvironment;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Map;

class SessionHolderRocksDBTest {

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
        System.clearProperty(ConfigurationKeys.STORE_FILE_DIR);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ENGINE);
        System.clearProperty(ConfigurationKeys.STORE_FILE_ROCKSDB_DIR);
        SessionHolder.destroy();
        LockerManagerFactory.destroy();
        RocksDBStoreEngineFactory.destroy();
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    @Test
    void testRocksDBFileEngineInitializesRocksDBSessionManager() {
        configureRocksDBFileMode();

        SessionHolder.init(SessionMode.FILE);

        Assertions.assertTrue(SessionHolder.getRootSessionManager() instanceof RocksDBSessionManager);
    }

    @Test
    void testRocksDBFileEngineReloadsBranchLocks() throws Exception {
        configureRocksDBFileMode();
        SessionHolder.init(SessionMode.FILE);

        BranchSession branchSession = branchSession(1001L, 1L, "t_order:1");
        GlobalSession globalSession = globalSession(branchSession);
        globalSession.add(branchSession);

        SessionHolder.reload(Collections.singletonList(globalSession), SessionMode.FILE);

        BranchSession conflict = branchSession(1002L, 2L, "t_order:1");
        Assertions.assertFalse(LockerManagerFactory.getLockManager().acquireLock(conflict));
    }

    @Test
    void testRocksDBFileEngineMigratesFileSessionLog() throws Exception {
        configureRocksDBFileMode();
        GlobalSession globalSession = globalSession(2001L);
        appendFileLog(globalSession, LogOperation.GLOBAL_ADD);

        SessionHolder.init(SessionMode.FILE);

        GlobalSession actual = SessionHolder.getRootSessionManager().findGlobalSession(globalSession.getXid(), true);
        Assertions.assertNotNull(actual);
        Assertions.assertEquals(globalSession.getXid(), actual.getXid());
    }

    private void configureRocksDBFileMode() {
        System.setProperty(
                ConfigurationKeys.STORE_FILE_DIR, tempDir.resolve("file").toString());
        System.setProperty(ConfigurationKeys.STORE_FILE_ENGINE, "rocksdb");
        System.setProperty(
                ConfigurationKeys.STORE_FILE_ROCKSDB_DIR,
                tempDir.resolve("rocksdb").toString());
    }

    private GlobalSession globalSession(BranchSession branchSession) {
        GlobalSession globalSession = globalSession(branchSession.getTransactionId());
        globalSession.setXid(branchSession.getXid());
        return globalSession;
    }

    private GlobalSession globalSession(long transactionId) {
        GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
        globalSession.setXid("127.0.0.1:8091:" + transactionId);
        globalSession.setTransactionId(transactionId);
        globalSession.setStatus(GlobalStatus.Begin);
        return globalSession;
    }

    private BranchSession branchSession(long transactionId, long branchId, String lockKey) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid("127.0.0.1:8091:" + transactionId);
        branchSession.setTransactionId(transactionId);
        branchSession.setBranchId(branchId);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/db");
        branchSession.setLockKey(lockKey);
        return branchSession;
    }

    private void appendFileLog(SessionStorable session, LogOperation logOperation) throws IOException {
        byte[] data = new TransactionWriteStore(session, logOperation).encode();
        ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + data.length);
        buffer.putInt(data.length);
        buffer.put(data);
        Path fileLog = tempDir.resolve("file")
                .resolve(String.valueOf(XID.getPort()))
                .resolve(SessionHolder.ROOT_SESSION_MANAGER_NAME);
        Files.createDirectories(fileLog.getParent());
        Files.write(fileLog, buffer.array(), StandardOpenOption.CREATE, StandardOpenOption.APPEND);
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
