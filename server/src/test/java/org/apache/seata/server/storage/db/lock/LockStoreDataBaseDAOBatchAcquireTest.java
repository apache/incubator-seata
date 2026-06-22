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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.LockStatus;
import org.apache.seata.core.store.LockDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link LockStoreDataBaseDAO#doAcquireLocks} batch-result handling.
 *
 * <p>Batch lock acquisition must not rely on {@code executeBatch().length == lockDOs.size()}.
 * The JDBC spec does not guarantee that the returned array has one element per batched
 * statement, and some drivers (e.g. Dameng/DM) aggregate the per-statement results into an
 * array of a different length. The previous length comparison then evaluated to {@code false}
 * even though every row was inserted, producing a bogus "global lock batch acquire failed"
 * for any branch that acquires two or more locks at once.
 *
 * <p>The JDBC layer is mocked, so no database is required and the test runs in CI by default
 * (unlike {@link LockStoreDataBaseDAOTest}, which is gated behind a live DB).
 */
public class LockStoreDataBaseDAOBatchAcquireTest extends BaseSpringBootTest {

    private static LockStoreDataBaseDAO lockStoreDataBaseDAO;

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        // The DAO's static CONFIG needs the Spring-Boot test context (from BaseSpringBootTest)
        // to initialize; feed it a db type so the constructor passes, then mock the data source
        // since this test stubs the JDBC layer rather than hitting a real database.
        ConfigurationFactory.getInstance().putConfig(ConfigurationKeys.STORE_DB_TYPE, "mysql");
        ConfigurationFactory.getInstance().putConfig(ConfigurationKeys.LOCK_DB_TABLE, "lock_table");
        lockStoreDataBaseDAO = new LockStoreDataBaseDAO(mock(DataSource.class));
    }

    /**
     * Driver aggregates the batch result into an array shorter than the number of batched
     * statements, with no {@link Statement#EXECUTE_FAILED}. Acquisition must still succeed.
     * Before the fix this returned {@code false}.
     */
    @Test
    public void doAcquireLocksSucceedsWhenDriverAggregatesBatchResult() throws SQLException {
        // 3 statements batched, driver returns a single aggregated update count
        Connection conn = mockConnReturning(new int[] {3});
        assertTrue(
                lockStoreDataBaseDAO.doAcquireLocks(conn, locks(3)),
                "batch acquire must succeed when the driver aggregates executeBatch() results");
    }

    /**
     * A spec-conforming driver returns one update count per statement. Acquisition succeeds.
     */
    @Test
    public void doAcquireLocksSucceedsWhenDriverReturnsOneCountPerStatement() throws SQLException {
        Connection conn = mockConnReturning(new int[] {1, 1, 1});
        assertTrue(
                lockStoreDataBaseDAO.doAcquireLocks(conn, locks(3)),
                "batch acquire must succeed for a spec-conforming driver");
    }

    /**
     * Any per-statement {@link Statement#EXECUTE_FAILED} in the returned array is a real
     * failure and acquisition must report it.
     */
    @Test
    public void doAcquireLocksFailsWhenAnyStatementReportsExecuteFailed() throws SQLException {
        Connection conn = mockConnReturning(new int[] {1, Statement.EXECUTE_FAILED, 1});
        assertFalse(
                lockStoreDataBaseDAO.doAcquireLocks(conn, locks(3)),
                "batch acquire must fail when any statement reports EXECUTE_FAILED");
    }

    private Connection mockConnReturning(int[] batchResult) throws SQLException {
        PreparedStatement ps = mock(PreparedStatement.class);
        when(ps.executeBatch()).thenReturn(batchResult);
        Connection conn = mock(Connection.class);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        return conn;
    }

    private List<LockDO> locks(int n) {
        List<LockDO> lockDOs = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            LockDO lockDO = new LockDO();
            lockDO.setXid("test-xid");
            lockDO.setTransactionId(1L);
            lockDO.setBranchId(1L);
            lockDO.setResourceId("test-resource");
            lockDO.setTableName("test_table");
            lockDO.setPk(String.valueOf(i));
            lockDO.setRowKey("test-resource^^^test_table^^^" + i);
            lockDO.setStatus(LockStatus.Locked.getCode());
            lockDOs.add(lockDO);
        }
        return lockDOs;
    }
}
