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
package org.apache.seata.core.store.db.sql.distributed.lock;

import org.apache.seata.core.constants.ServerTableColumnsName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the dialect specific {@link DistributedLockSql} implementations
 * that support {@code FOR UPDATE NOWAIT} (issue #7993).
 */
class DistributedLockSqlNoWaitTest {

    private static final String TABLE = "distributed_lock";

    private static final String SELECT_BASE_SQL = "SELECT " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + ","
            + ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE + "," + ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE
            + " FROM " + TABLE + " WHERE " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + " = ?";

    @Test
    void testMysqlDialectAppendsNoWait() {
        MysqlDistributedLockSql sql = new MysqlDistributedLockSql();
        assertEquals(SELECT_BASE_SQL + " FOR UPDATE NOWAIT", sql.getSelectDistributeForUpdateNoWaitSql(TABLE));
        // legacy blocking variant must be preserved for backward compatibility
        assertEquals(SELECT_BASE_SQL + " FOR UPDATE", sql.getSelectDistributeForUpdateSql(TABLE));
    }

    @Test
    void testPostgresqlDialectAppendsNoWait() {
        PostgresqlDistributedLockSql sql = new PostgresqlDistributedLockSql();
        assertEquals(SELECT_BASE_SQL + " FOR UPDATE NOWAIT", sql.getSelectDistributeForUpdateNoWaitSql(TABLE));
        assertEquals(SELECT_BASE_SQL + " FOR UPDATE", sql.getSelectDistributeForUpdateSql(TABLE));
    }

    @Test
    void testOracleDialectAppendsNoWait() {
        OracleDistributedLockSql sql = new OracleDistributedLockSql();
        assertEquals(SELECT_BASE_SQL + " FOR UPDATE NOWAIT", sql.getSelectDistributeForUpdateNoWaitSql(TABLE));
        assertEquals(SELECT_BASE_SQL + " FOR UPDATE", sql.getSelectDistributeForUpdateSql(TABLE));
    }

    @Test
    void testBaseDialectFallsBackToBlockingForNoWait() {
        // BaseDistributedLockSql does not override the NOWAIT method and
        // must fall back to the regular FOR UPDATE statement so dialects
        // without NOWAIT support keep their previous semantics.
        BaseDistributedLockSql sql = new BaseDistributedLockSql();
        String noWait = sql.getSelectDistributeForUpdateNoWaitSql(TABLE);
        assertEquals(sql.getSelectDistributeForUpdateSql(TABLE), noWait);
        assertFalse(noWait.toUpperCase().contains("NOWAIT"));
    }

    @Test
    void testSqlServerDialectFallsBackToBlockingForNoWait() {
        // SQL Server uses table hints (WITH (ROWLOCK, UPDLOCK, HOLDLOCK))
        // and does not support NOWAIT in this codepath. The default
        // implementation should return the regular hinted SELECT.
        BaseDistributedLockSqlServer sql = new BaseDistributedLockSqlServer();
        String noWait = sql.getSelectDistributeForUpdateNoWaitSql(TABLE);
        assertEquals(sql.getSelectDistributeForUpdateSql(TABLE), noWait);
        assertTrue(noWait.contains("WITH (ROWLOCK, UPDLOCK, HOLDLOCK)"));
    }
}
