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
package org.apache.seata.server.storage.file.lock;

import org.apache.seata.common.Constants;
import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.store.LockMode;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.storage.file.spi.FileLockStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.Set;

class FileLockManagerStrategyTest {

    private Object originalEnvironment;

    @BeforeEach
    void beforeEach() {
        originalEnvironment = ObjectHolder.INSTANCE.getObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT);
        ObjectHolder.INSTANCE.setObject(Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        ConfigurationCache.clear();
    }

    @AfterEach
    void afterEach() throws Exception {
        LockerManagerFactory.destroy();
        new FileLocker(null).cleanAllLocks();
        ConfigurationCache.clear();
        restoreEnvironment();
    }

    @Test
    void testDefaultStoreSelectsFileLockerAndReleasesOwnerHolder() throws Exception {
        FileLockManager lockManager = install(new DefaultFileLockStore());
        BranchSession owner = branchSession(1001L, 1L, "t_order:1");
        BranchSession contender = branchSession(1002L, 2L, "t_order:1");

        Assertions.assertInstanceOf(FileLocker.class, lockManager.getLocker(owner));
        Assertions.assertTrue(lockManager.acquireLock(owner));
        Assertions.assertFalse(lockManager.acquireLock(contender));

        owner.setLockKey("");
        Assertions.assertTrue(lockManager.releaseLock(owner));
        Assertions.assertTrue(owner.getLockHolder().isEmpty());
        Assertions.assertTrue(lockManager.acquireLock(contender));
    }

    @Test
    void testDefaultStorePreservesGlobalReleaseAndLockerSemantics() throws Exception {
        FileLockManager lockManager = install(new DefaultFileLockStore());
        GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
        BranchSession first = branchSession(globalSession.getTransactionId(), 1L, "t_order:1");
        BranchSession second = branchSession(globalSession.getTransactionId(), 2L, "t_order:2");
        first.setXid(globalSession.getXid());
        second.setXid(globalSession.getXid());
        globalSession.add(first);
        globalSession.add(second);

        Assertions.assertTrue(lockManager.acquireLock(first));
        Assertions.assertTrue(lockManager.acquireLock(second));
        Assertions.assertFalse(lockManager.isLockable(xid(2001L), first.getResourceId(), "t_order:1,2"));

        lockManager.updateLockStatus(globalSession.getXid(), LockStatus.Rollbacking);
        Assertions.assertEquals(LockStatus.Locked, first.getLockStatus());

        first.setLockKey("");
        second.setLockKey("");
        Assertions.assertTrue(lockManager.releaseGlobalSessionLock(globalSession));
        Assertions.assertTrue(first.getLockHolder().isEmpty());
        Assertions.assertTrue(second.getLockHolder().isEmpty());
        Assertions.assertTrue(lockManager.isLockable(xid(2001L), first.getResourceId(), "t_order:1,2"));

        BranchSession owner = branchSession(3001L, 3L, "t_order:3");
        Assertions.assertTrue(lockManager.acquireLock(owner));
        lockManager.cleanAllLocks();
        Assertions.assertTrue(lockManager.isLockable(xid(3002L), owner.getResourceId(), "t_order:3"));
    }

    @Test
    void testDefaultStoreReturnsFalseWhenOwnerReleaseFails() throws Exception {
        FileLockManager lockManager = install(new DefaultFileLockStore());
        BranchSession failingOwner = failingOwner();

        Assertions.assertFalse(lockManager.releaseLock(failingOwner));
    }

    @Test
    void testDefaultGlobalReleaseContinuesAfterFailureAndReturnsLastSuccess() throws Exception {
        FileLockManager lockManager = install(new DefaultFileLockStore());
        GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
        BranchSession successfulOwner = branchSession(globalSession.getTransactionId(), 2L, "t_order:2");
        globalSession.add(failingOwner());
        globalSession.add(successfulOwner);
        Assertions.assertTrue(lockManager.acquireLock(successfulOwner));

        Assertions.assertTrue(lockManager.releaseGlobalSessionLock(globalSession));

        Assertions.assertTrue(successfulOwner.getLockHolder().isEmpty());
        Assertions.assertTrue(
                lockManager.isLockable(xid(2002L), successfulOwner.getResourceId(), successfulOwner.getLockKey()));
    }

    @Test
    void testDefaultGlobalReleaseReturnsFinalFailureAfterEarlierSuccess() throws Exception {
        FileLockManager lockManager = install(new DefaultFileLockStore());
        GlobalSession globalSession = new GlobalSession("app", "group", "tx", 60000);
        BranchSession successfulOwner = branchSession(globalSession.getTransactionId(), 1L, "t_order:1");
        globalSession.add(successfulOwner);
        globalSession.add(failingOwner());
        Assertions.assertTrue(lockManager.acquireLock(successfulOwner));

        Assertions.assertFalse(lockManager.releaseGlobalSessionLock(globalSession));

        Assertions.assertTrue(successfulOwner.getLockHolder().isEmpty());
        Assertions.assertTrue(
                lockManager.isLockable(xid(2001L), successfulOwner.getResourceId(), successfulOwner.getLockKey()));
    }

    private BranchSession branchSession(long transactionId, long branchId, String lockKey) {
        BranchSession branchSession = new BranchSession(BranchType.AT);
        branchSession.setXid(xid(transactionId));
        branchSession.setTransactionId(transactionId);
        branchSession.setBranchId(branchId);
        branchSession.setStatus(BranchStatus.Registered);
        branchSession.setResourceId("jdbc:mysql://127.0.0.1/db");
        branchSession.setLockKey(lockKey);
        return branchSession;
    }

    private FileLockManager install(FileLockStore lockStore) {
        LockerManagerFactory.init(LockMode.FILE, new Class<?>[] {FileLockStore.class}, new Object[] {lockStore});
        return (FileLockManager) LockerManagerFactory.getLockManager();
    }

    private static String xid(long transactionId) {
        return "127.0.0.1:8091:" + transactionId;
    }

    private static BranchSession failingOwner() {
        return new BranchSession(BranchType.AT) {
            @Override
            public Map<FileLocker.BucketLockMap, Set<String>> getLockHolder() {
                throw new IllegalStateException("release failed");
            }
        };
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
