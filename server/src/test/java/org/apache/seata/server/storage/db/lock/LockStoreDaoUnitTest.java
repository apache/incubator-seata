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

import org.apache.seata.common.exception.*;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.apache.seata.core.lock.RowLock;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.*;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.*;

import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LockStoreDaoUnitTest extends BaseSpringBootTest {
    private DataSource source;
    private Connection connection;
    private PreparedStatement statement;
    private LockStoreDataBaseDAO dao;
    private LockDO lock;

    @BeforeEach
    void open() throws Exception {
        ConfigurationFactory.getInstance().putConfig(ConfigurationKeys.STORE_DB_TYPE, "mysql");
        ConfigurationFactory.getInstance().putConfig(ConfigurationKeys.LOCK_DB_TABLE, "lock_table");
        source = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        when(source.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeUpdate()).thenReturn(1);
        when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
        dao = new LockStoreDataBaseDAO(source);
        lock = new LockDO();
        lock.setXid("host:8091:42");
        lock.setTransactionId(42L);
        lock.setBranchId(7L);
        lock.setResourceId("db");
        lock.setTableName("orders");
        lock.setPk("1");
        lock.setRowKey("db^^^orders^^^1");
    }

    @Test
    void unlockOperationsBindTheirOwnScopeAndCloseResources() throws Exception {
        assertTrue(dao.unLock(lock));
        assertTrue(dao.unLock(lock.getXid()));
        assertTrue(dao.unLock(7L));
        dao.updateLockStatus(lock.getXid(), LockStatus.Rollbacking);
        verify(statement, times(2)).setString(1, lock.getXid());
        verify(statement).setLong(1, 7L);
        verify(statement).setString(2, lock.getRowKey());
        verify(statement).setInt(1, LockStatus.Rollbacking.getCode());
        verify(connection, times(4)).close();
        verify(statement, times(4)).close();
    }

    @Test
    void singleInsertDistinguishesConflictFromDatabaseFailure() throws Exception {
        assertTrue(dao.doAcquireLock(connection, lock));
        verify(statement).setString(7, lock.getRowKey());
        verify(statement).setInt(8, LockStatus.Locked.getCode());
        when(statement.executeUpdate()).thenReturn(0);
        assertFalse(dao.doAcquireLock(connection, lock));
        doThrow(new SQLIntegrityConstraintViolationException("duplicate"))
                .when(statement)
                .executeUpdate();
        assertFalse(dao.doAcquireLock(connection, lock));
        doThrow(new SQLException("offline")).when(statement).executeUpdate();
        assertThrows(StoreException.class, () -> dao.doAcquireLock(connection, lock));
    }

    @Test
    void batchExceptionsDistinguishConstraintViolations() throws Exception {
        BatchUpdateException withCause = new BatchUpdateException();
        withCause.initCause(new SQLIntegrityConstraintViolationException("duplicate"));
        when(statement.executeBatch()).thenThrow(withCause);
        assertFalse(dao.doAcquireLocks(connection, Collections.singletonList(lock)));
        BatchUpdateException withNext = new BatchUpdateException();
        withNext.setNextException(new SQLIntegrityConstraintViolationException("duplicate"));
        doThrow(withNext).when(statement).executeBatch();
        assertFalse(dao.doAcquireLocks(connection, Collections.singletonList(lock)));
        BatchUpdateException other = new BatchUpdateException();
        other.setNextException(new SQLException("offline"));
        doThrow(other).when(statement).executeBatch();
        assertThrows(SQLException.class, () -> dao.doAcquireLocks(connection, Collections.singletonList(lock)));
        doThrow(new BatchUpdateException()).when(statement).executeBatch();
        assertThrows(BatchUpdateException.class, () -> dao.doAcquireLocks(connection, Collections.singletonList(lock)));
    }

    @Test
    void queryAndUnlockFailuresPropagateWithResourcesClosed() throws Exception {
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("offline"));
        assertThrows(StoreException.class, () -> dao.unLock(lock));
        assertThrows(StoreException.class, () -> dao.unLock(lock.getXid()));
        assertThrows(StoreException.class, () -> dao.unLock(7L));
        assertThrows(DataAccessException.class, () -> dao.updateLockStatus(lock.getXid(), LockStatus.Locked));
        assertThrows(DataAccessException.class, () -> dao.isLockable(Collections.singletonList(lock)));
        verify(connection, times(5)).close();
    }

    @Test
    void databaseLockerDelegatesAndDistinguishesStoreFailures() {
        LockStore store = mock(LockStore.class);
        DataBaseLocker locker = new DataBaseLocker();
        locker.setLockStore(store);
        RowLock row = new RowLock();
        row.setXid(lock.getXid());
        row.setTransactionId(42L);
        row.setBranchId(7L);
        row.setResourceId("db");
        row.setTableName("orders");
        row.setPk("1");
        row.setRowKey(lock.getRowKey());
        List<RowLock> rows = Collections.singletonList(row);
        when(store.acquireLock(anyList(), eq(true), eq(false))).thenReturn(true);
        when(store.unLock(anyList())).thenReturn(true);
        when(store.unLock(7L)).thenReturn(true);
        when(store.unLock("xid")).thenReturn(true);
        when(store.isLockable(anyList())).thenReturn(true);
        assertTrue(locker.acquireLock(rows));
        assertTrue(locker.releaseLock(rows));
        assertTrue(locker.releaseLock("xid", 7L));
        assertTrue(locker.releaseLock("xid"));
        assertTrue(locker.isLockable(rows));
        locker.updateLockStatus("xid", LockStatus.Rollbacking);
        verify(store).updateLockStatus("xid", LockStatus.Rollbacking);
        RuntimeException failure = new IllegalStateException("offline");
        doThrow(failure).when(store).acquireLock(anyList(), anyBoolean(), anyBoolean());
        doThrow(failure).when(store).unLock(anyList());
        doThrow(failure).when(store).unLock(anyLong());
        doThrow(failure).when(store).unLock(anyString());
        doThrow(failure).when(store).isLockable(anyList());
        assertFalse(locker.acquireLock(rows));
        assertFalse(locker.releaseLock(rows));
        assertFalse(locker.releaseLock("xid", 7L));
        assertFalse(locker.releaseLock("xid"));
        assertFalse(locker.isLockable(rows));
        StoreException se = new StoreException("store");
        doThrow(se).when(store).acquireLock(anyList(), anyBoolean(), anyBoolean());
        doThrow(se).when(store).unLock(anyList());
        doThrow(se).when(store).unLock(anyLong());
        doThrow(se).when(store).unLock(anyString());
        assertSame(se, assertThrows(StoreException.class, () -> locker.acquireLock(rows)));
        assertThrows(StoreException.class, () -> locker.releaseLock(rows));
        assertThrows(StoreException.class, () -> locker.releaseLock("xid", 7L));
        assertThrows(StoreException.class, () -> locker.releaseLock("xid"));
        assertTrue(locker.acquireLock(Collections.emptyList()));
        assertTrue(locker.releaseLock(Collections.emptyList()));
        assertTrue(locker.isLockable(Collections.emptyList()));
    }
}
