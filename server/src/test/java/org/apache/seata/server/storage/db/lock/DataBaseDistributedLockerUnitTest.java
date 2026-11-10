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
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.*;
import org.apache.seata.core.store.DistributedLockDO;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;

import javax.sql.DataSource;
import java.sql.*;

import static org.apache.seata.core.constants.ConfigurationKeys.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataBaseDistributedLockerUnitTest extends BaseSpringBootTest {
    private DataSource dataSource;
    private Connection connection;
    private PreparedStatement statement;
    private ResultSet result;
    private DataBaseDistributedLocker locker;
    private Configuration config;
    private MockedStatic<ConfigurationFactory> configuration;
    private MockedStatic<EnhancedServiceLoader> loader;

    @BeforeEach
    void open() throws Exception {
        config = mock(Configuration.class);
        when(config.getConfig(DISTRIBUTED_LOCK_DB_TABLE)).thenReturn("distributed_lock");
        when(config.getConfig(STORE_DB_TYPE)).thenReturn("mysql");
        when(config.getConfig(STORE_DB_DATASOURCE_TYPE)).thenReturn("test");
        configuration = mockStatic(ConfigurationFactory.class);
        configuration.when(ConfigurationFactory::getInstance).thenReturn(config);
        dataSource = mock(DataSource.class);
        connection = mock(Connection.class);
        statement = mock(PreparedStatement.class);
        result = mock(ResultSet.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getAutoCommit()).thenReturn(true);
        when(connection.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(statement.executeUpdate()).thenReturn(1);
        DataSourceProvider provider = mock(DataSourceProvider.class);
        when(provider.provide()).thenReturn(dataSource);
        // Intercept provider loading before any real loader invocation can initialize
        // a static datasource configuration with this test's mocked configuration.
        loader = mockStatic(EnhancedServiceLoader.class, invocation -> {
            Object[] args = invocation.getArguments();
            if ("load".equals(invocation.getMethod().getName())
                    && args.length > 0
                    && args[0] == DataSourceProvider.class) {
                return provider;
            }
            return invocation.callRealMethod();
        });
        locker = new DataBaseDistributedLocker();
    }

    @AfterEach
    void close() {
        loader.close();
        configuration.close();
    }

    private DistributedLockDO lock() {
        DistributedLockDO d = new DistributedLockDO();
        d.setLockKey("lease");
        d.setLockValue("owner");
        d.setExpireTime(1000L);
        return d;
    }

    @Test
    void acquisitionInsertsLeaseAndRestoresConnection() throws Exception {
        DistributedLockDO d = lock();
        long start = System.currentTimeMillis();
        assertTrue(locker.acquireLock(d));
        assertTrue(d.getExpireTime() >= start + 1000);
        verify(statement, times(2)).setString(1, "lease");
        verify(statement).setString(2, "owner");
        verify(connection).commit();
        verify(connection).setAutoCommit(false);
        verify(connection).setAutoCommit(true);
        verify(connection).close();
    }

    @Test
    void activeLeaseBlocksButExpiredLeaseCanBeReplaced() throws Exception {
        when(result.next()).thenReturn(true);
        when(result.getLong(anyString())).thenReturn(Long.MAX_VALUE);
        when(result.getString(anyString())).thenReturn("other");
        assertFalse(locker.acquireLock(lock()));
        verify(statement, never()).executeUpdate();
        when(result.getLong(anyString())).thenReturn(0L);
        assertTrue(locker.acquireLock(lock()));
        verify(statement).executeUpdate();
        verify(connection, times(2)).commit();
    }

    @Test
    void releasePreservesOtherOwnerAndExpiresOwnedLease() throws Exception {
        when(result.next()).thenReturn(true);
        when(result.getLong(anyString())).thenReturn(Long.MAX_VALUE);
        when(result.getString(anyString())).thenReturn("other");
        assertTrue(locker.releaseLock(lock()));
        verify(statement, never()).executeUpdate();
        when(result.getString(anyString())).thenReturn("owner");
        DistributedLockDO d = lock();
        assertTrue(locker.releaseLock(d));
        assertEquals(0L, d.getExpireTime());
        assertEquals(" ", d.getLockValue());
        verify(statement).setLong(2, 0L);
        verify(connection, times(2)).close();
    }

    @Test
    void missingLeaseCannotBeReleased() throws Exception {
        assertThrows(ShouldNeverHappenException.class, () -> locker.releaseLock(lock()));
        verify(connection).close();
    }

    @Test
    void sqlFailuresRollbackAndCloseIncludingLockWaitTimeout() throws Exception {
        when(statement.executeQuery()).thenThrow(new SQLException("try restarting transaction", "", 1205));
        assertFalse(locker.acquireLock(lock()));
        assertFalse(locker.releaseLock(lock()));
        verify(connection, times(2)).rollback();
        verify(connection, times(2)).close();
        doThrow(new SQLException("broken")).when(statement).executeQuery();
        doThrow(new SQLException("rollback failed")).when(connection).rollback();
        assertFalse(locker.acquireLock(lock()));
        assertFalse(locker.releaseLock(lock()));
        when(dataSource.getConnection()).thenThrow(new SQLException("offline"));
        assertFalse(locker.acquireLock(lock()));
        assertFalse(locker.releaseLock(lock()));
    }

    @Test
    void zeroAffectedRowsAndZeroDurationArePreserved() throws Exception {
        DistributedLockDO d = lock();
        d.setExpireTime(0L);
        when(statement.executeUpdate()).thenReturn(0);
        assertFalse(locker.insertDistribute(connection, d));
        assertFalse(locker.updateDistributedLock(connection, d));
        assertEquals(0L, d.getExpireTime());
    }

    @Test
    void missingTableTemporarilyDemotesLocking() throws Exception {
        when(config.getConfig(DISTRIBUTED_LOCK_DB_TABLE)).thenReturn("");
        DataBaseDistributedLocker demoted = new DataBaseDistributedLocker();
        assertTrue(demoted.acquireLock(lock()));
        assertTrue(demoted.releaseLock(lock()));
        verifyNoInteractions(connection);
        org.mockito.ArgumentCaptor<ConfigurationChangeListener> listener =
                org.mockito.ArgumentCaptor.forClass(ConfigurationChangeListener.class);
        verify(config).addConfigListener(eq(DISTRIBUTED_LOCK_DB_TABLE), listener.capture());
        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setNewValue("distributed_lock");
        listener.getValue().onChangeEvent(event);
        assertTrue(demoted.acquireLock(lock()));
        verify(connection).commit();
    }
}
