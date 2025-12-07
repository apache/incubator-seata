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
package org.apache.seata.rm.datasource;

import org.apache.seata.common.LockStrategyMode;
import org.apache.seata.core.context.GlobalLockConfigHolder;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalLockConfig;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.rm.datasource.ConnectionProxy.LockRetryPolicy;
import org.apache.seata.rm.datasource.exec.LockConflictException;
import org.apache.seata.rm.datasource.exec.LockWaitTimeoutException;
import org.apache.seata.rm.datasource.mock.MockConnection;
import org.apache.seata.rm.datasource.mock.MockDriver;
import org.apache.seata.rm.datasource.sql.struct.Row;
import org.apache.seata.rm.datasource.sql.struct.TableRecords;
import org.apache.seata.rm.datasource.undo.SQLUndoLog;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.SQLException;
import java.sql.Savepoint;

/**
 * ConnectionProxy test
 *
 */
@EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11
}) // `ReflectionUtil.modifyStaticFinalField` does not supported java17 and above versions
public class ConnectionProxyTest {
    private DataSourceProxy dataSourceProxy;

    private static final String TEST_RESOURCE_ID = "testResourceId";

    private static final String TEST_XID = "testXid";

    private static final String lockKey = "order:123";

    private static final String DB_TYPE = "mysql";

    private Field branchRollbackFlagField;
    private boolean originalBranchRollbackFlag;

    @BeforeEach
    public void initBeforeEach() throws Exception {
        branchRollbackFlagField =
                LockRetryPolicy.class.getDeclaredField("LOCK_RETRY_POLICY_BRANCH_ROLLBACK_ON_CONFLICT");
        Field modifiersField = Field.class.getDeclaredField("modifiers");
        modifiersField.setAccessible(true);
        modifiersField.setInt(branchRollbackFlagField, branchRollbackFlagField.getModifiers() & ~Modifier.FINAL);
        branchRollbackFlagField.setAccessible(true);
        originalBranchRollbackFlag = (boolean) branchRollbackFlagField.get(null);

        dataSourceProxy = Mockito.mock(DataSourceProxy.class);
        Mockito.when(dataSourceProxy.getResourceId()).thenReturn(TEST_RESOURCE_ID);
        Mockito.when(dataSourceProxy.getDbType()).thenReturn(DB_TYPE);
        DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);

        Mockito.when(rm.branchRegister(
                        BranchType.AT,
                        dataSourceProxy.getResourceId(),
                        null,
                        TEST_XID,
                        "{\"autoCommit\":false}",
                        lockKey))
                .thenThrow(new TransactionException(TransactionExceptionCode.LockKeyConflict));
        DefaultResourceManager defaultResourceManager = DefaultResourceManager.get();
        Assertions.assertNotNull(defaultResourceManager);
        DefaultResourceManager.mockResourceManager(BranchType.AT, rm);
    }

    @org.junit.jupiter.api.AfterEach
    public void cleanupAfterEach() throws Exception {
        branchRollbackFlagField.set(null, originalBranchRollbackFlag);
    }

    @Test
    public void testLockRetryPolicyRollbackOnConflict() throws Exception {
        branchRollbackFlagField.set(null, true);
        GlobalLockConfig preGlobalLockConfig = new GlobalLockConfig();
        preGlobalLockConfig.setLockRetryTimes(0);
        preGlobalLockConfig.setLockRetryInterval(10);
        preGlobalLockConfig.setLockStrategyMode(LockStrategyMode.PESSIMISTIC);
        GlobalLockConfig globalLockConfig = GlobalLockConfigHolder.setAndReturnPrevious(preGlobalLockConfig);
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.getContext().appendUndoItem(sqlUndoLog);
            connectionProxy.appendUndoLog(new SQLUndoLog());
            connectionProxy.appendLockKey(lockKey);
            Assertions.assertThrows(LockWaitTimeoutException.class, connectionProxy::commit);
        }
    }

    @Test
    public void testLockRetryPolicyNotRollbackOnConflict() throws Exception {
        branchRollbackFlagField.set(null, false);
        GlobalLockConfig preGlobalLockConfig = new GlobalLockConfig();
        preGlobalLockConfig.setLockRetryTimes(30);
        preGlobalLockConfig.setLockRetryInterval(10);
        preGlobalLockConfig.setLockStrategyMode(LockStrategyMode.PESSIMISTIC);
        GlobalLockConfig globalLockConfig = GlobalLockConfigHolder.setAndReturnPrevious(preGlobalLockConfig);
        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, null);
        connectionProxy.bind(TEST_XID);
        connectionProxy.appendUndoLog(new SQLUndoLog());
        connectionProxy.appendLockKey(lockKey);
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        TableRecords beforeImage = new TableRecords();
        beforeImage.add(new Row());
        sqlUndoLog.setBeforeImage(beforeImage);
        connectionProxy.getContext().appendUndoItem(sqlUndoLog);
        Assertions.assertThrows(LockWaitTimeoutException.class, connectionProxy::commit);
    }

    @Test
    public void testGetContext() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            Assertions.assertNotNull(connectionProxy.getContext());
        }
    }

    @Test
    public void testBindXid() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            connectionProxy.bind(TEST_XID);
            Assertions.assertEquals(TEST_XID, connectionProxy.getContext().getXid());
        }
    }

    @Test
    public void testSetGlobalLockRequire() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            connectionProxy.setGlobalLockRequire(true);
            Assertions.assertTrue(connectionProxy.isGlobalLockRequire());
            connectionProxy.setGlobalLockRequire(false);
            Assertions.assertFalse(connectionProxy.isGlobalLockRequire());
        }
    }

    @Test
    public void testAppendUndoLog() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            SQLUndoLog undoLog = new SQLUndoLog();
            connectionProxy.appendUndoLog(undoLog);
            Assertions.assertEquals(
                    1, connectionProxy.getContext().getUndoItems().size());
        }
    }

    @Test
    public void testAppendLockKey() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            connectionProxy.appendLockKey("test:1");
            connectionProxy.appendLockKey("test:2");
            Assertions.assertNotNull(connectionProxy.getContext());
        }
    }

    @Test
    public void testGetTargetConnection() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            Assertions.assertEquals(mockConnection, connectionProxy.getTargetConnection());
        }
    }

    @Test
    public void testGetDataSourceProxy() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            Assertions.assertEquals(dataSourceProxy, connectionProxy.getDataSourceProxy());
        }
    }

    @Test
    public void testCheckLockWithBlankLockKeys() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            connectionProxy.bind(TEST_XID);
            connectionProxy.checkLock("");
            connectionProxy.checkLock(null);
        }
    }

    @Test
    public void testLockQueryWithBlankLockKeys() throws Exception {
        try (ConnectionProxy connectionProxy =
                new ConnectionProxy(dataSourceProxy, new MockConnection(new MockDriver(), "", null))) {
            connectionProxy.bind(TEST_XID);
            boolean result = connectionProxy.lockQuery("");
            Assertions.assertFalse(result);
        }
    }

    @Test
    public void commitInGlobalTransactionTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString()))
                    .thenReturn(123456L);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            Mockito.verify(rm)
                    .branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.eq(TEST_XID),
                            Mockito.anyString(),
                            Mockito.eq(lockKey));
        }
    }

    @Test
    public void commitWithGlobalLockRequireTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT), Mockito.anyString(), Mockito.isNull(), Mockito.anyString()))
                    .thenReturn(true);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.setGlobalLockRequire(true);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            Mockito.verify(rm)
                    .lockQuery(Mockito.eq(BranchType.AT), Mockito.anyString(), Mockito.isNull(), Mockito.eq(lockKey));
        }
    }

    @Test
    public void commitWithoutTransactionTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            connectionProxy.setAutoCommit(false);

            connectionProxy.commit();

            Assertions.assertNull(connectionProxy.getContext().getXid());
        }
    }

    @Test
    public void commitWithSQLExceptionTest() throws Exception {
        MockConnection mockConnection = Mockito.mock(MockConnection.class);
        Mockito.when(mockConnection.getAutoCommit()).thenReturn(false);
        Mockito.doThrow(new SQLException("Commit failed")).when(mockConnection).commit();
        Mockito.doNothing().when(mockConnection).rollback();

        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection);
        connectionProxy.setAutoCommit(false);

        Assertions.assertThrows(SQLException.class, connectionProxy::commit);

        Mockito.verify(mockConnection).rollback();
    }

    @Test
    public void rollbackTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            connectionProxy.setAutoCommit(false);

            connectionProxy.rollback();

            Assertions.assertNull(connectionProxy.getContext().getBranchId());
        }
    }

    @Test
    public void rollbackWithBranchRegisteredTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString()))
                    .thenReturn(123456L);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            connectionProxy.bind(TEST_XID);
            connectionProxy.getContext().setBranchId(123456L);
            connectionProxy.rollback();

            Mockito.verify(rm)
                    .branchReport(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(123456L),
                            Mockito.eq(BranchStatus.PhaseOne_Failed),
                            Mockito.isNull());
        }
    }

    @Test
    public void registerSuccessTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.isNull(),
                            Mockito.eq(TEST_XID),
                            Mockito.anyString(),
                            Mockito.eq(lockKey)))
                    .thenReturn(789L);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            Mockito.verify(rm)
                    .branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.isNull(),
                            Mockito.eq(TEST_XID),
                            Mockito.anyString(),
                            Mockito.eq(lockKey));
        }
    }

    @Test
    public void registerWithNoUndoLogTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            Mockito.verify(rm, Mockito.never())
                    .branchRegister(
                            Mockito.any(),
                            Mockito.anyString(),
                            Mockito.any(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString());
        }
    }

    @Test
    public void registerWithNoLockKeyTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);

            connectionProxy.commit();

            Mockito.verify(rm, Mockito.never())
                    .branchRegister(
                            Mockito.any(),
                            Mockito.anyString(),
                            Mockito.any(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString());
        }
    }

    @Test
    public void reportSuccessTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString()))
                    .thenReturn(999L);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            if (ConnectionProxy.IS_REPORT_SUCCESS_ENABLE) {
                Mockito.verify(rm)
                        .branchReport(
                                Mockito.eq(BranchType.AT),
                                Mockito.eq(TEST_XID),
                                Mockito.eq(999L),
                                Mockito.eq(BranchStatus.PhaseOne_Done),
                                Mockito.isNull());
            }
        }
    }

    @Test
    public void reportFailureTest() throws Exception {
        MockConnection mockConnection = Mockito.mock(MockConnection.class);
        Mockito.when(mockConnection.getAutoCommit()).thenReturn(false);
        Mockito.doThrow(new SQLException("Commit failed")).when(mockConnection).commit();

        ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection);

        DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
        Mockito.when(rm.branchRegister(
                        Mockito.eq(BranchType.AT),
                        Mockito.anyString(),
                        Mockito.isNull(),
                        Mockito.anyString(),
                        Mockito.anyString(),
                        Mockito.anyString()))
                .thenReturn(888L);
        DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

        connectionProxy.setAutoCommit(false);
        connectionProxy.bind(TEST_XID);
        SQLUndoLog sqlUndoLog = new SQLUndoLog();
        TableRecords beforeImage = new TableRecords();
        beforeImage.add(new Row());
        sqlUndoLog.setBeforeImage(beforeImage);
        connectionProxy.appendUndoLog(sqlUndoLog);
        connectionProxy.appendLockKey(lockKey);

        Assertions.assertThrows(SQLException.class, connectionProxy::commit);

        Mockito.verify(rm, Mockito.times(2))
                .branchReport(
                        Mockito.eq(BranchType.AT),
                        Mockito.eq(TEST_XID),
                        Mockito.eq(888L),
                        Mockito.eq(BranchStatus.PhaseOne_Failed),
                        Mockito.isNull());
    }

    @Test
    public void reportWithRetryTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString()))
                    .thenReturn(777L);
            Mockito.doThrow(new TransactionException("Report failed"))
                    .doNothing()
                    .when(rm)
                    .branchReport(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(777L),
                            Mockito.eq(BranchStatus.PhaseOne_Done),
                            Mockito.isNull());
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.commit();

            if (ConnectionProxy.IS_REPORT_SUCCESS_ENABLE) {
                Mockito.verify(rm, Mockito.atLeast(1))
                        .branchReport(
                                Mockito.eq(BranchType.AT),
                                Mockito.eq(TEST_XID),
                                Mockito.eq(777L),
                                Mockito.eq(BranchStatus.PhaseOne_Done),
                                Mockito.isNull());
            }
        }
    }

    @Test
    public void setAutoCommitToTrueInGlobalTransactionTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.anyString(),
                            Mockito.anyString(),
                            Mockito.anyString()))
                    .thenReturn(555L);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.bind(TEST_XID);
            SQLUndoLog sqlUndoLog = new SQLUndoLog();
            TableRecords beforeImage = new TableRecords();
            beforeImage.add(new Row());
            sqlUndoLog.setBeforeImage(beforeImage);
            connectionProxy.appendUndoLog(sqlUndoLog);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.setAutoCommit(true);

            Mockito.verify(rm)
                    .branchRegister(
                            Mockito.eq(BranchType.AT),
                            Mockito.anyString(),
                            Mockito.isNull(),
                            Mockito.eq(TEST_XID),
                            Mockito.anyString(),
                            Mockito.eq(lockKey));
            Assertions.assertTrue(mockConnection.getAutoCommit());
        }
    }

    @Test
    public void setAutoCommitToTrueWithGlobalLockTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT), Mockito.anyString(), Mockito.isNull(), Mockito.anyString()))
                    .thenReturn(true);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.setAutoCommit(false);
            connectionProxy.setGlobalLockRequire(true);
            connectionProxy.appendLockKey(lockKey);

            connectionProxy.setAutoCommit(true);

            Mockito.verify(rm)
                    .lockQuery(Mockito.eq(BranchType.AT), Mockito.anyString(), Mockito.isNull(), Mockito.eq(lockKey));
            Assertions.assertTrue(mockConnection.getAutoCommit());
        }
    }

    @Test
    public void checkLockWithRealLockKeysTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey)))
                    .thenReturn(true);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.bind(TEST_XID);

            connectionProxy.checkLock(lockKey);

            Mockito.verify(rm)
                    .lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey));
        }
    }

    @Test
    public void checkLockWithConflictTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey)))
                    .thenReturn(false);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.bind(TEST_XID);

            Assertions.assertThrows(LockConflictException.class, () -> connectionProxy.checkLock(lockKey));
        }
    }

    @Test
    public void lockQueryReturnsTrueTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey)))
                    .thenReturn(true);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.bind(TEST_XID);

            boolean result = connectionProxy.lockQuery(lockKey);

            Assertions.assertTrue(result);
        }
    }

    @Test
    public void lockQueryReturnsFalseTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey)))
                    .thenReturn(false);
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.bind(TEST_XID);

            boolean result = connectionProxy.lockQuery(lockKey);

            Assertions.assertFalse(result);
        }
    }

    @Test
    public void setSavepointTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            Savepoint savepoint = connectionProxy.setSavepoint();

            Assertions.assertNotNull(savepoint);
        }
    }

    @Test
    public void setSavepointWithNameTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            String savepointName = "sp1";
            Savepoint savepoint = connectionProxy.setSavepoint(savepointName);

            Assertions.assertNotNull(savepoint);
        }
    }

    @Test
    public void rollbackToSavepointTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            Savepoint savepoint = connectionProxy.setSavepoint();
            Assertions.assertNotNull(savepoint);

            connectionProxy.rollback(savepoint);

            Assertions.assertNotNull(connectionProxy.getContext());
        }
    }

    @Test
    public void releaseSavepointTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            Savepoint savepoint = connectionProxy.setSavepoint();
            Assertions.assertNotNull(savepoint);

            connectionProxy.releaseSavepoint(savepoint);

            Assertions.assertNotNull(connectionProxy.getContext());
        }
    }

    @Test
    public void recognizeLockKeyConflictExceptionTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey)))
                    .thenThrow(new TransactionException(TransactionExceptionCode.LockKeyConflict, "lock conflict"));
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.bind(TEST_XID);

            LockConflictException exception =
                    Assertions.assertThrows(LockConflictException.class, () -> connectionProxy.checkLock(lockKey));
            Assertions.assertEquals(TransactionExceptionCode.LockKeyConflict, exception.getCode());
        }
    }

    @Test
    public void recognizeLockKeyConflictFailFastExceptionTest() throws Exception {
        MockConnection mockConnection = new MockConnection(new MockDriver(), "", null);
        try (ConnectionProxy connectionProxy = new ConnectionProxy(dataSourceProxy, mockConnection)) {
            DefaultResourceManager rm = Mockito.mock(DefaultResourceManager.class);
            Mockito.when(rm.lockQuery(
                            Mockito.eq(BranchType.AT),
                            Mockito.eq(TEST_RESOURCE_ID),
                            Mockito.eq(TEST_XID),
                            Mockito.eq(lockKey)))
                    .thenThrow(new TransactionException(
                            TransactionExceptionCode.LockKeyConflictFailFast, "lock conflict fail fast"));
            DefaultResourceManager.mockResourceManager(BranchType.AT, rm);

            connectionProxy.bind(TEST_XID);

            LockConflictException exception =
                    Assertions.assertThrows(LockConflictException.class, () -> connectionProxy.checkLock(lockKey));
            Assertions.assertEquals(TransactionExceptionCode.LockKeyConflictFailFast, exception.getCode());
        }
    }
}
