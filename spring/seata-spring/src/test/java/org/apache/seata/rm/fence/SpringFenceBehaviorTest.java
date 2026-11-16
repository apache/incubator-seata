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
package org.apache.seata.rm.fence;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.SkipCallbackWrapperException;
import org.apache.seata.integration.tx.api.fence.constant.CommonFenceConstant;
import org.apache.seata.integration.tx.api.remoting.TwoPhaseResult;
import org.apache.seata.rm.tcc.api.BusinessActionContext;
import org.apache.seata.rm.tcc.api.BusinessActionContextUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SpringFenceBehaviorTest {
    private final SpringFenceHandler handler = new SpringFenceHandler();
    private DataSource previousDataSource;
    private TransactionTemplate previousTemplate;
    private BusinessActionContext previousContext;
    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet rows;
    private PlatformTransactionManager manager;
    private SimpleTransactionStatus status;
    private final Action action = new Action();

    @BeforeEach
    void setUp() throws Exception {
        previousDataSource = SpringFenceHandler.getDataSource();
        Field field = SpringFenceHandler.class.getDeclaredField("transactionTemplate");
        field.setAccessible(true);
        previousTemplate = (TransactionTemplate) field.get(null);
        previousContext = BusinessActionContextUtil.getContext();
        BusinessActionContextUtil.clear();
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        rows = mock(ResultSet.class);
        manager = mock(PlatformTransactionManager.class);
        status = new SimpleTransactionStatus();
        when(manager.getTransaction(any())).thenReturn(status);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(rows);
        when(statement.executeUpdate()).thenReturn(1);
        SpringFenceHandler.setDataSource(dataSource);
        SpringFenceHandler.setTransactionTemplate(new TransactionTemplate(manager));
    }

    @AfterEach
    void restoreState() {
        SpringFenceHandler.setDataSource(previousDataSource);
        SpringFenceHandler.setTransactionTemplate(previousTemplate);
        BusinessActionContextUtil.clear();
        if (previousContext != null) {
            BusinessActionContextUtil.setContext(previousContext);
        }
    }

    @Test
    void prepareInsertsTriedBeforeInvokingBusiness() throws Exception {
        Object expected = new Object();
        assertSame(expected, handler.prepareFence("xid", 7L, "action", () -> {
            verify(statement).setInt(4, CommonFenceConstant.STATUS_TRIED);
            verify(statement).executeUpdate();
            return expected;
        }));
        verify(manager).commit(status);
        assertFalse(status.isRollbackOnly());
    }

    @Test
    void unsuccessfulInsertDoesNotInvokeBusiness() throws Exception {
        when(statement.executeUpdate()).thenReturn(0);
        AtomicInteger calls = new AtomicInteger();
        assertThrows(
                SkipCallbackWrapperException.class,
                () -> handler.prepareFence("xid", 7L, "action", () -> calls.incrementAndGet()));
        assertEquals(0, calls.get());
        assertTrue(status.isRollbackOnly());
        verify(manager).rollback(status);
    }

    @Test
    void prepareRollsBackBusinessException() {
        IllegalStateException failure = new IllegalStateException("business failure");
        SkipCallbackWrapperException wrapped = assertThrows(
                SkipCallbackWrapperException.class,
                () -> handler.prepareFence("xid", 7L, "action", () -> {
                    throw failure;
                }));
        assertSame(failure, wrapped.getCause());
        verify(manager).rollback(status);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void commitAndRollbackUpdateOnlyTriedRecords(boolean commit) throws Exception {
        existing(CommonFenceConstant.STATUS_TRIED);
        assertTrue(invoke(commit, "success"));
        assertEquals(1, action.calls);
        verify(statement)
                .setInt(1, commit ? CommonFenceConstant.STATUS_COMMITTED : CommonFenceConstant.STATUS_ROLLBACKED);
        verify(statement).setInt(5, CommonFenceConstant.STATUS_TRIED);
        verify(manager).commit(status);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void alreadyCompletedPhaseIsIdempotent(boolean commit) throws Exception {
        existing(commit ? CommonFenceConstant.STATUS_COMMITTED : CommonFenceConstant.STATUS_ROLLBACKED);
        assertTrue(invoke(commit, "success"));
        assertEquals(0, action.calls);
        verify(statement, never()).executeUpdate();
    }

    @Test
    void suspendedRollbackIsIdempotent() throws Exception {
        existing(CommonFenceConstant.STATUS_SUSPENDED);
        assertTrue(invoke(false, "success"));
        assertEquals(0, action.calls);
        verify(statement, never()).executeUpdate();
    }

    @ParameterizedTest
    @ValueSource(ints = {CommonFenceConstant.STATUS_ROLLBACKED, CommonFenceConstant.STATUS_SUSPENDED})
    void commitRejectsRollbackAndSuspension(int state) throws Exception {
        existing(state);
        assertFalse(invoke(true, "success"));
        assertEquals(0, action.calls);
    }

    @Test
    void rollbackRejectsCommittedRecord() throws Exception {
        existing(CommonFenceConstant.STATUS_COMMITTED);
        assertFalse(invoke(false, "success"));
        assertEquals(0, action.calls);
    }

    @Test
    void commitRequiresExistingFence() {
        assertThrows(SkipCallbackWrapperException.class, () -> invoke(true, "success"));
        assertEquals(0, action.calls);
        verify(manager).rollback(status);
    }

    @Test
    void emptyRollbackCreatesSuspensionWithoutCallingBusiness() throws Exception {
        assertTrue(invoke(false, "success"));
        assertEquals(0, action.calls);
        verify(statement).setInt(4, CommonFenceConstant.STATUS_SUSPENDED);
    }

    @Test
    void emptyRollbackInsertFailureRollsBack() throws Exception {
        when(statement.executeUpdate()).thenReturn(0);
        assertThrows(SkipCallbackWrapperException.class, () -> invoke(false, "success"));
        verify(manager).rollback(status);
        assertEquals(0, action.calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void lostCompareAndSetDoesNotInvokeBusiness(boolean commit) throws Exception {
        existing(CommonFenceConstant.STATUS_TRIED);
        when(statement.executeUpdate()).thenReturn(0);
        assertFalse(invoke(commit, "success"));
        assertEquals(0, action.calls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"failure", "twoPhaseFailure"})
    void failedBusinessResultMarksTransactionRollbackOnly(String method) throws Exception {
        existing(CommonFenceConstant.STATUS_TRIED);
        assertFalse(invoke(true, method));
        assertTrue(status.isRollbackOnly());
        assertEquals(1, action.calls);
    }

    @ParameterizedTest
    @ValueSource(strings = {"noResult", "twoPhaseSuccess"})
    void successfulBusinessResultsCommit(String method) throws Exception {
        existing(CommonFenceConstant.STATUS_TRIED);
        assertTrue(invoke(false, method));
        assertFalse(status.isRollbackOnly());
        assertEquals(1, action.calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void businessExceptionIsUnwrappedAndRolledBack(boolean commit) throws Exception {
        existing(CommonFenceConstant.STATUS_TRIED);
        SkipCallbackWrapperException failure =
                assertThrows(SkipCallbackWrapperException.class, () -> invoke(commit, "throwsFailure"));
        assertSame(action.failure, failure.getCause());
        assertTrue(status.isRollbackOnly());
        verify(manager).rollback(status);
    }

    @Test
    void deadlockDuringRollbackPreservesDatabaseCause() throws Exception {
        SQLException failure =
                new SQLException("deadlock", Constants.DEAD_LOCK_SQL_STATE, Constants.DEAD_LOCK_ERROR_CODE);
        when(statement.executeQuery()).thenThrow(failure);
        SkipCallbackWrapperException wrapped =
                assertThrows(SkipCallbackWrapperException.class, () -> invoke(false, "success"));
        assertSame(failure, wrapped.getCause().getCause());
        verify(manager).rollback(status);
    }

    @Test
    void transactionalMethodUsesItsIsolation() throws Exception {
        existing(CommonFenceConstant.STATUS_TRIED);
        assertTrue(invoke(true, "isolated"));
        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(manager).getTransaction(definition.capture());
        assertEquals(
                TransactionDefinition.ISOLATION_SERIALIZABLE,
                definition.getValue().getIsolationLevel());
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void prepareUsesIsolationFromActionContextWhenPresent(boolean withIsolation) {
        BusinessActionContext context = new BusinessActionContext();
        context.setActionContext(new HashMap<>());
        if (withIsolation) {
            context.getActionContext().put(Constants.TX_ISOLATION, TransactionDefinition.ISOLATION_READ_COMMITTED);
        }
        BusinessActionContextUtil.setContext(context);
        assertEquals("prepared", handler.prepareFence("xid", 7L, "action", () -> "prepared"));
        ArgumentCaptor<TransactionDefinition> definition = ArgumentCaptor.forClass(TransactionDefinition.class);
        verify(manager).getTransaction(definition.capture());
        assertEquals(
                withIsolation
                        ? TransactionDefinition.ISOLATION_READ_COMMITTED
                        : TransactionDefinition.ISOLATION_DEFAULT,
                definition.getValue().getIsolationLevel());
    }

    @Test
    void deleteFenceCommitsDeletion() throws Exception {
        assertTrue(SpringFenceHandler.deleteFence("xid", 7L));
        verify(statement).setString(1, "xid");
        verify(statement).setLong(2, 7L);
        verify(manager).commit(status);
    }

    @Test
    void deleteFailureReturnsFalseAndMarksRollback() throws Exception {
        when(statement.executeUpdate()).thenThrow(new SQLException("delete failed"));
        assertFalse(SpringFenceHandler.deleteFence("xid", 7L));
        assertTrue(status.isRollbackOnly());
    }

    @Test
    void cleanupDeletesExpiredBatchAndReleasesConnection() throws Exception {
        DatabaseMetaData metadata = mock(DatabaseMetaData.class);
        when(connection.getMetaData()).thenReturn(metadata);
        when(metadata.getURL()).thenReturn("jdbc:mysql://unused");
        when(rows.next()).thenReturn(true, false);
        when(rows.getString("xid")).thenReturn("expired");
        when(statement.executeUpdate()).thenReturn(3);
        assertEquals(3, handler.deleteFenceByDate(new Date(1234)));
        verify(statement).setInt(2, 1000);
        verify(connection).close();
    }

    @Test
    void cleanupConnectionFailureReturnsZero() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("unavailable"));
        assertEquals(0, handler.deleteFenceByDate(new Date()));
        verify(connection, never()).close();
    }

    private void existing(int state) throws Exception {
        when(rows.next()).thenReturn(true);
        when(rows.getInt("status")).thenReturn(state);
    }

    private boolean invoke(boolean commit, String method) throws Exception {
        return commit
                ? handler.commitFence(Action.class.getMethod(method), action, "xid", 7L, new Object[0])
                : handler.rollbackFence(Action.class.getMethod(method), action, "xid", 7L, new Object[0], "action");
    }

    public static class Action {
        int calls;
        final IllegalStateException failure = new IllegalStateException("business failure");

        public boolean success() {
            calls++;
            return true;
        }

        public boolean failure() {
            calls++;
            return false;
        }

        public TwoPhaseResult twoPhaseFailure() {
            calls++;
            return new TwoPhaseResult(false, "retry");
        }

        public TwoPhaseResult twoPhaseSuccess() {
            calls++;
            return new TwoPhaseResult(true, "done");
        }

        public void noResult() {
            calls++;
        }

        public boolean throwsFailure() {
            throw failure;
        }

        @Transactional(isolation = Isolation.SERIALIZABLE)
        public boolean isolated() {
            calls++;
            return true;
        }
    }
}
