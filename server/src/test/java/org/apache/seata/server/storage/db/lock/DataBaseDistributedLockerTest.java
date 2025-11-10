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
package org.apache.seata.server.storage.db.lock;

import org.apache.seata.common.exception.ShouldNeverHappenException;
import org.apache.seata.core.constants.ServerTableColumnsName;
import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.core.store.DistributedLocker;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@EnabledIfSystemProperty(named = "dbCaseEnabled", matches = "true")
public class DataBaseDistributedLockerTest extends BaseSpringBootTest {

    private DataBaseDistributedLocker dataBaseDistributedLocker;

    @BeforeEach
    public void setUp() {
        dataBaseDistributedLocker = new DataBaseDistributedLocker();
    }

    @AfterEach
    public void tearDown() {
        // Clean up test locks
        try {
            DistributedLockDO cleanupLock = new DistributedLockDO();
            cleanupLock.setLockKey("test-key");
            cleanupLock.setLockValue("");
            cleanupLock.setExpireTime(0L);
            dataBaseDistributedLocker.releaseLock(cleanupLock);

            cleanupLock.setLockKey("test-key-2");
            dataBaseDistributedLocker.releaseLock(cleanupLock);

            cleanupLock.setLockKey("test-key-expired");
            dataBaseDistributedLocker.releaseLock(cleanupLock);

            cleanupLock.setLockKey("test-key-conflict");
            dataBaseDistributedLocker.releaseLock(cleanupLock);
        } catch (Exception e) {
            // Ignore cleanup errors
        }
    }

    @Test
    public void testInstanceCreation() {
        Assertions.assertNotNull(dataBaseDistributedLocker);
    }

    @Test
    public void testImplementsDistributedLocker() {
        Assertions.assertTrue(dataBaseDistributedLocker instanceof DistributedLocker);
    }

    @Test
    public void testAcquireAndReleaseLock() {
        DistributedLockDO lockDO = new DistributedLockDO();
        lockDO.setLockKey("test-key");
        lockDO.setLockValue("test-value");
        lockDO.setExpireTime(60000L);

        boolean acquired = dataBaseDistributedLocker.acquireLock(lockDO);
        Assertions.assertTrue(acquired, "Lock acquisition should succeed");

        boolean released = dataBaseDistributedLocker.releaseLock(lockDO);
        Assertions.assertTrue(released, "Lock release should succeed");
    }

    @Test
    public void testAcquireLockConflict() {
        DistributedLockDO lockDO1 = new DistributedLockDO();
        lockDO1.setLockKey("test-key-conflict");
        lockDO1.setLockValue("holder-1");
        lockDO1.setExpireTime(60000L);

        // First acquisition should succeed
        boolean firstAcquired = dataBaseDistributedLocker.acquireLock(lockDO1);
        Assertions.assertTrue(firstAcquired, "First lock acquisition should succeed");

        // Second acquisition with different value should fail
        DistributedLockDO lockDO2 = new DistributedLockDO();
        lockDO2.setLockKey("test-key-conflict");
        lockDO2.setLockValue("holder-2");
        lockDO2.setExpireTime(60000L);

        boolean secondAcquired = dataBaseDistributedLocker.acquireLock(lockDO2);
        Assertions.assertFalse(secondAcquired, "Conflicting lock acquisition should fail");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO1);
    }

    @Test
    public void testAcquireLockAfterExpired() throws InterruptedException {
        DistributedLockDO lockDO1 = new DistributedLockDO();
        lockDO1.setLockKey("test-key-expired");
        lockDO1.setLockValue("holder-1");
        lockDO1.setExpireTime(100L); // 100ms expiration

        // First acquisition should succeed
        boolean firstAcquired = dataBaseDistributedLocker.acquireLock(lockDO1);
        Assertions.assertTrue(firstAcquired, "First lock acquisition should succeed");

        // Wait for lock to expire
        Thread.sleep(150);

        // Second acquisition should succeed after expiration
        DistributedLockDO lockDO2 = new DistributedLockDO();
        lockDO2.setLockKey("test-key-expired");
        lockDO2.setLockValue("holder-2");
        lockDO2.setExpireTime(60000L);

        boolean secondAcquired = dataBaseDistributedLocker.acquireLock(lockDO2);
        Assertions.assertTrue(secondAcquired, "Lock acquisition should succeed after expiration");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO2);
    }

    @Test
    public void testReleaseLockNotOwned() {
        DistributedLockDO lockDO1 = new DistributedLockDO();
        lockDO1.setLockKey("test-key-2");
        lockDO1.setLockValue("holder-1");
        lockDO1.setExpireTime(60000L);

        // Acquire lock with holder-1
        boolean acquired = dataBaseDistributedLocker.acquireLock(lockDO1);
        Assertions.assertTrue(acquired, "Lock acquisition should succeed");

        // Try to release with different holder
        DistributedLockDO lockDO2 = new DistributedLockDO();
        lockDO2.setLockKey("test-key-2");
        lockDO2.setLockValue("holder-2");
        lockDO2.setExpireTime(60000L);

        boolean released = dataBaseDistributedLocker.releaseLock(lockDO2);
        // Release should succeed but not actually release (as per implementation)
        Assertions.assertTrue(released, "Release should return true even if not owned");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO1);
    }

    @Test
    public void testAcquireLockWithZeroExpireTime() {
        DistributedLockDO lockDO = new DistributedLockDO();
        lockDO.setLockKey("test-key");
        lockDO.setLockValue("test-value");
        lockDO.setExpireTime(0L);

        boolean acquired = dataBaseDistributedLocker.acquireLock(lockDO);
        Assertions.assertTrue(acquired, "Lock acquisition with zero expire time should succeed");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO);
    }

    @Test
    public void testReacquireSameLock() {
        DistributedLockDO lockDO1 = new DistributedLockDO();
        lockDO1.setLockKey("test-key");
        lockDO1.setLockValue("holder-1");
        lockDO1.setExpireTime(60000L);

        // First acquisition
        boolean firstAcquired = dataBaseDistributedLocker.acquireLock(lockDO1);
        Assertions.assertTrue(firstAcquired, "First acquisition should succeed");

        // Try to reacquire same lock with same holder - should fail due to conflict
        DistributedLockDO lockDO2 = new DistributedLockDO();
        lockDO2.setLockKey("test-key");
        lockDO2.setLockValue("holder-1");
        lockDO2.setExpireTime(60000L);

        boolean secondAcquired = dataBaseDistributedLocker.acquireLock(lockDO2);
        Assertions.assertFalse(secondAcquired, "Reacquisition should fail while lock is held");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO1);
    }

    @Test
    public void testAcquireLockAfterRelease() {
        DistributedLockDO lockDO1 = new DistributedLockDO();
        lockDO1.setLockKey("test-key");
        lockDO1.setLockValue("holder-1");
        lockDO1.setExpireTime(60000L);

        // Acquire and release
        boolean acquired1 = dataBaseDistributedLocker.acquireLock(lockDO1);
        Assertions.assertTrue(acquired1, "First acquisition should succeed");

        boolean released = dataBaseDistributedLocker.releaseLock(lockDO1);
        Assertions.assertTrue(released, "Release should succeed");

        // Acquire again with different holder
        DistributedLockDO lockDO2 = new DistributedLockDO();
        lockDO2.setLockKey("test-key");
        lockDO2.setLockValue("holder-2");
        lockDO2.setExpireTime(60000L);

        boolean acquired2 = dataBaseDistributedLocker.acquireLock(lockDO2);
        Assertions.assertTrue(acquired2, "Acquisition after release should succeed");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO2);
    }

    @Test
    public void testMultipleLockKeys() {
        DistributedLockDO lockDO1 = new DistributedLockDO();
        lockDO1.setLockKey("test-key");
        lockDO1.setLockValue("holder-1");
        lockDO1.setExpireTime(60000L);

        DistributedLockDO lockDO2 = new DistributedLockDO();
        lockDO2.setLockKey("test-key-2");
        lockDO2.setLockValue("holder-2");
        lockDO2.setExpireTime(60000L);

        // Both locks should succeed (different keys)
        boolean acquired1 = dataBaseDistributedLocker.acquireLock(lockDO1);
        Assertions.assertTrue(acquired1, "First lock should succeed");

        boolean acquired2 = dataBaseDistributedLocker.acquireLock(lockDO2);
        Assertions.assertTrue(acquired2, "Second lock with different key should succeed");

        // Clean up
        dataBaseDistributedLocker.releaseLock(lockDO1);
        dataBaseDistributedLocker.releaseLock(lockDO2);
    }

    // ===========================
    // Mock-based Unit Tests
    // These tests use mocks to test edge cases, exception handling, and branch coverage
    // ===========================

    @Nested
    class MockBasedUnitTests {

        @Mock
        private DataSource mockDataSource;

        @Mock
        private Connection mockConnection;

        @Mock
        private PreparedStatement mockPreparedStatement;

        @Mock
        private ResultSet mockResultSet;

        private DataBaseDistributedLocker locker;

        @BeforeEach
        void setUpMocks() throws Exception {
            // Create a real instance and inject mock datasource via reflection
            locker = new DataBaseDistributedLocker();
            setDistributedLockDataSource(locker, mockDataSource);
            // Disable demotion mode to ensure normal operation
            setDemotionMode(locker, false);
        }

        // ===========================
        // SQLException Handling Tests
        // ===========================

        @Test
        void testAcquireLock_WithIgnoredSQLException_MySQLCode1205() throws Exception {
            SQLException sqlException = new SQLException("Lock wait timeout exceeded", "HY000", 1205);
            when(mockDataSource.getConnection()).thenThrow(sqlException);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertFalse(result, "acquireLock should return false on ignored SQLException");
        }

        @Test
        void testAcquireLock_WithIgnoredSQLException_MySQLMessage() throws Exception {
            SQLException sqlException =
                    new SQLException("Lock wait timeout exceeded; try restarting transaction", "HY000", 9999);
            when(mockDataSource.getConnection()).thenThrow(sqlException);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertFalse(result, "acquireLock should return false on ignored SQLException");
        }

        @Test
        void testAcquireLock_WithNonIgnoredSQLException() throws Exception {
            SQLException sqlException = new SQLException("Connection refused", "08001", 8888);
            when(mockDataSource.getConnection()).thenThrow(sqlException);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertFalse(result, "acquireLock should return false on non-ignored SQLException");
        }

        @Test
        void testReleaseLock_WithIgnoredSQLException() throws Exception {
            SQLException sqlException = new SQLException("Lock wait timeout exceeded", "HY000", 1205);
            when(mockDataSource.getConnection()).thenThrow(sqlException);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertFalse(result, "releaseLock should return false on ignored SQLException");
        }

        @Test
        void testReleaseLock_WithNonIgnoredSQLException() throws Exception {
            SQLException sqlException = new SQLException("Connection refused", "08001", 8888);
            when(mockDataSource.getConnection()).thenThrow(sqlException);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertFalse(result, "releaseLock should return false on non-ignored SQLException");
        }

        // ===========================
        // Rollback Failure Tests
        // ===========================

        @Test
        void testAcquireLock_RollbackFailure_HandlesGracefully() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);

            SQLException executionException = new SQLException("Execution failed", "HY000", 8888);
            SQLException rollbackException = new SQLException("Rollback failed", "HY000", 9999);

            when(mockConnection.prepareStatement(anyString())).thenThrow(executionException);
            doThrow(rollbackException).when(mockConnection).rollback();

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertFalse(result, "acquireLock should return false when both execution and rollback fail");
            verify(mockConnection).rollback();
        }

        @Test
        void testReleaseLock_RollbackFailure_HandlesGracefully() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);

            SQLException executionException = new SQLException("Execution failed", "HY000", 8888);
            SQLException rollbackException = new SQLException("Rollback failed", "HY000", 9999);

            when(mockConnection.prepareStatement(anyString())).thenThrow(executionException);
            doThrow(rollbackException).when(mockConnection).rollback();

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertFalse(result, "releaseLock should return false when both execution and rollback fail");
            verify(mockConnection).rollback();
        }

        // ===========================
        // Connection Closure Exception Tests
        // ===========================

        @Test
        void testAcquireLock_ConnectionCloseException_IsSilentlyIgnored() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            SQLException closeException = new SQLException("Close failed");
            doThrow(closeException).when(mockConnection).close();

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertTrue(result, "Lock acquisition should succeed even if close() fails");
        }

        @Test
        void testAcquireLock_SetAutoCommitException_IsSilentlyIgnored() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            SQLException setAutoCommitException = new SQLException("setAutoCommit failed");
            doNothing().when(mockConnection).setAutoCommit(false);
            doThrow(setAutoCommitException).when(mockConnection).setAutoCommit(true);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertTrue(result, "Lock acquisition should succeed even if setAutoCommit(true) fails");
        }

        @Test
        void testReleaseLock_ConnectionCloseException_IsSilentlyIgnored() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getLong(ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE))
                    .thenReturn(System.currentTimeMillis() + 60000);
            when(mockResultSet.getString(ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE))
                    .thenReturn("test-value");
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            SQLException closeException = new SQLException("Close failed");
            doThrow(closeException).when(mockConnection).close();

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertTrue(result, "Lock release should succeed even if close() fails");
        }

        // ===========================
        // ShouldNeverHappenException Test
        // ===========================

        @Test
        void testReleaseLock_WithNonExistentLock_ThrowsShouldNeverHappenException() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("non-existent-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            ShouldNeverHappenException exception = assertThrows(
                    ShouldNeverHappenException.class,
                    () -> locker.releaseLock(lockDO),
                    "releaseLock should throw ShouldNeverHappenException when lock doesn't exist");

            assertTrue(
                    exception.getMessage().contains("would not be null when release distribute lock"),
                    "Exception message should mention the error condition");
        }

        // ===========================
        // ExpireTime Branch Coverage Tests
        // ===========================

        @Test
        void testInsertDistribute_WithPositiveExpireTime_AddsCurrentTime() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ArgumentCaptor<Long> expireTimeCaptor = ArgumentCaptor.forClass(Long.class);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            long beforeTime = System.currentTimeMillis();
            boolean result = locker.acquireLock(lockDO);
            long afterTime = System.currentTimeMillis();

            assertTrue(result);
            verify(mockPreparedStatement, atLeastOnce()).setLong(eq(3), expireTimeCaptor.capture());

            Long capturedExpireTime = expireTimeCaptor.getValue();
            assertTrue(
                    capturedExpireTime >= beforeTime + 5000L && capturedExpireTime <= afterTime + 5000L,
                    "ExpireTime should be adjusted by adding current timestamp");
        }

        @Test
        void testInsertDistribute_WithZeroExpireTime_DoesNotAddCurrentTime() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ArgumentCaptor<Long> expireTimeCaptor = ArgumentCaptor.forClass(Long.class);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(0L);

            boolean result = locker.acquireLock(lockDO);

            assertTrue(result);
            verify(mockPreparedStatement, atLeastOnce()).setLong(eq(3), expireTimeCaptor.capture());
            assertEquals(0L, expireTimeCaptor.getValue(), "ExpireTime should remain 0 when initially 0");
        }

        @Test
        void testUpdateDistributedLock_WithPositiveExpireTime_AddsCurrentTime() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getLong(ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE))
                    .thenReturn(System.currentTimeMillis() - 1000);
            when(mockResultSet.getString(ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE))
                    .thenReturn("old-value");
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ArgumentCaptor<Long> expireTimeCaptor = ArgumentCaptor.forClass(Long.class);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("new-value");
            lockDO.setExpireTime(5000L);

            long beforeTime = System.currentTimeMillis();
            boolean result = locker.acquireLock(lockDO);
            long afterTime = System.currentTimeMillis();

            assertTrue(result);
            verify(mockPreparedStatement, atLeastOnce()).setLong(eq(2), expireTimeCaptor.capture());

            Long capturedExpireTime = expireTimeCaptor.getValue();
            assertTrue(
                    capturedExpireTime >= beforeTime + 5000L && capturedExpireTime <= afterTime + 5000L,
                    "ExpireTime should be adjusted by adding current timestamp in update");
        }

        @Test
        void testUpdateDistributedLock_WithZeroExpireTime_DoesNotAddCurrentTime() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getLong(ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE))
                    .thenReturn(System.currentTimeMillis() + 60000);
            when(mockResultSet.getString(ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE))
                    .thenReturn("test-value");
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            ArgumentCaptor<Long> expireTimeCaptor = ArgumentCaptor.forClass(Long.class);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(0L);

            boolean result = locker.releaseLock(lockDO);

            assertTrue(result);
            verify(mockPreparedStatement, atLeastOnce()).setLong(eq(2), expireTimeCaptor.capture());
            assertEquals(0L, expireTimeCaptor.getValue(), "ExpireTime should remain 0 when initially 0 in update");
        }

        // ===========================
        // Debug Logging Branch Tests
        // ===========================

        @Test
        void testReleaseLock_WithDifferentOwner_ReturnsTrue() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(true);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getLong(ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE))
                    .thenReturn(System.currentTimeMillis() + 60000);
            when(mockResultSet.getString(ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE))
                    .thenReturn("other-holder");

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertTrue(result, "releaseLock should return true when lock is held by different owner");
            verify(mockConnection).commit();
            verify(mockPreparedStatement, never()).executeUpdate();
        }

        // ===========================
        // ignoreSQLException() Method Tests
        // ===========================

        @Test
        void testIgnoreSQLException_WithCode1205_ReturnsTrue() throws Exception {
            SQLException exception = new SQLException("Lock wait timeout", "HY000", 1205);

            Method method = DataBaseDistributedLocker.class.getDeclaredMethod("ignoreSQLException", SQLException.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(locker, exception);

            assertTrue(result, "ignoreSQLException should return true for error code 1205");
        }

        @Test
        void testIgnoreSQLException_WithMatchingMessage_ReturnsTrue() throws Exception {
            SQLException exception =
                    new SQLException("Lock wait timeout exceeded; try restarting transaction", "HY000", 9999);

            Method method = DataBaseDistributedLocker.class.getDeclaredMethod("ignoreSQLException", SQLException.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(locker, exception);

            assertTrue(result, "ignoreSQLException should return true for matching message");
        }

        @Test
        void testIgnoreSQLException_WithNullMessage_ReturnsFalse() throws Exception {
            SQLException exception = new SQLException(null, "HY000", 9999);

            Method method = DataBaseDistributedLocker.class.getDeclaredMethod("ignoreSQLException", SQLException.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(locker, exception);

            assertFalse(result, "ignoreSQLException should return false for null message and non-ignored code");
        }

        @Test
        void testIgnoreSQLException_WithNonMatchingCodeAndMessage_ReturnsFalse() throws Exception {
            SQLException exception = new SQLException("Unknown error", "HY000", 9999);

            Method method = DataBaseDistributedLocker.class.getDeclaredMethod("ignoreSQLException", SQLException.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(locker, exception);

            assertFalse(result, "ignoreSQLException should return false for non-matching code and message");
        }

        @Test
        void testIgnoreSQLException_WithPartialMessageMatch_ReturnsTrue() throws Exception {
            SQLException exception = new SQLException("MySQL error: try restarting transaction now", "HY000", 9999);

            Method method = DataBaseDistributedLocker.class.getDeclaredMethod("ignoreSQLException", SQLException.class);
            method.setAccessible(true);
            boolean result = (boolean) method.invoke(locker, exception);

            assertTrue(result, "ignoreSQLException should return true for partial message match");
        }

        // ===========================
        // Static Initializer & Edge Cases Tests
        // ===========================

        @Test
        void testStaticInitializer_PopulatesIgnoreSets() throws Exception {
            Field ignoreMysqlCodeField = DataBaseDistributedLocker.class.getDeclaredField("IGNORE_MYSQL_CODE");
            ignoreMysqlCodeField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<Integer> ignoreMysqlCode = (Set<Integer>) ignoreMysqlCodeField.get(null);

            Field ignoreMysqlMessageField = DataBaseDistributedLocker.class.getDeclaredField("IGNORE_MYSQL_MESSAGE");
            ignoreMysqlMessageField.setAccessible(true);
            @SuppressWarnings("unchecked")
            Set<String> ignoreMysqlMessage = (Set<String>) ignoreMysqlMessageField.get(null);

            assertTrue(ignoreMysqlCode.contains(1205), "IGNORE_MYSQL_CODE should contain 1205");
            assertTrue(
                    ignoreMysqlMessage.contains("try restarting transaction"),
                    "IGNORE_MYSQL_MESSAGE should contain 'try restarting transaction'");
        }

        @Test
        void testAcquireLock_ConnectionGetAutoCommit_False() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(false);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(false);
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertTrue(result);
            verify(mockConnection, never()).setAutoCommit(true);
        }

        @Test
        void testReleaseLock_ConnectionGetAutoCommit_False() throws Exception {
            when(mockDataSource.getConnection()).thenReturn(mockConnection);
            when(mockConnection.getAutoCommit()).thenReturn(false);
            when(mockConnection.prepareStatement(anyString())).thenReturn(mockPreparedStatement);
            when(mockPreparedStatement.executeQuery()).thenReturn(mockResultSet);
            when(mockResultSet.next()).thenReturn(true);
            when(mockResultSet.getLong(ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE))
                    .thenReturn(System.currentTimeMillis() + 60000);
            when(mockResultSet.getString(ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE))
                    .thenReturn("test-value");
            when(mockPreparedStatement.executeUpdate()).thenReturn(1);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertTrue(result);
            verify(mockConnection, never()).setAutoCommit(true);
        }

        @Test
        void testAcquireLock_NullConnection_HandlesGracefully() throws Exception {
            SQLException exception = new SQLException("Connection failed", "08001", 8888);
            when(mockDataSource.getConnection()).thenThrow(exception);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.acquireLock(lockDO);

            assertFalse(result, "acquireLock should return false when connection fails");
            verify(mockConnection, never()).setAutoCommit(anyBoolean());
            verify(mockConnection, never()).close();
        }

        @Test
        void testReleaseLock_NullConnection_HandlesGracefully() throws Exception {
            SQLException exception = new SQLException("Connection failed", "08001", 8888);
            when(mockDataSource.getConnection()).thenThrow(exception);

            DistributedLockDO lockDO = new DistributedLockDO();
            lockDO.setLockKey("test-key");
            lockDO.setLockValue("test-value");
            lockDO.setExpireTime(5000L);

            boolean result = locker.releaseLock(lockDO);

            assertFalse(result, "releaseLock should return false when connection fails");
            verify(mockConnection, never()).setAutoCommit(anyBoolean());
            verify(mockConnection, never()).close();
        }

        // ===========================
        // Helper Methods for Reflection
        // ===========================

        private void setDemotionMode(DataBaseDistributedLocker locker, boolean value) throws Exception {
            Field demotionField = DataBaseDistributedLocker.class.getDeclaredField("demotion");
            demotionField.setAccessible(true);
            demotionField.set(locker, value);
        }

        private void setDistributedLockDataSource(DataBaseDistributedLocker locker, DataSource dataSource)
                throws Exception {
            Field dataSourceField = DataBaseDistributedLocker.class.getDeclaredField("distributedLockDataSource");
            dataSourceField.setAccessible(true);
            dataSourceField.set(locker, dataSource);
        }
    }
}
