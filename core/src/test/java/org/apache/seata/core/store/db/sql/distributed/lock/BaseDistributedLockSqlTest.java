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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BaseDistributedLockSqlTest {

    private BaseDistributedLockSql baseDistributedLockSql;
    private final String testTable = "test_lock_table";

    @BeforeEach
    void setUp() {
        baseDistributedLockSql = new BaseDistributedLockSql();
    }

    @Test
    void testGetSelectDistributeForUpdateSql() {
        String expected = "SELECT " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + "," +
                ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE + "," + ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE +
                " FROM " + testTable + " WHERE " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + " = ? FOR UPDATE";
        String actual = baseDistributedLockSql.getSelectDistributeForUpdateSql(testTable);
        assertEquals(expected, actual);
    }

    @Test
    void testGetInsertSql() {
        String expected = "INSERT INTO " + testTable + "(" +
                ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + "," +
                ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE + "," + ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE +
                ") VALUES (?, ?, ?)";
        String actual = baseDistributedLockSql.getInsertSql(testTable);
        assertEquals(expected, actual);
    }

    @Test
    void testGetUpdateSql() {
        String expected = "UPDATE " + testTable + " SET " +
                ServerTableColumnsName.DISTRIBUTED_LOCK_VALUE + "=?, " + ServerTableColumnsName.DISTRIBUTED_LOCK_EXPIRE + "=?" +
                " WHERE " + ServerTableColumnsName.DISTRIBUTED_LOCK_KEY + "=?";
        String actual = baseDistributedLockSql.getUpdateSql(testTable);
        assertEquals(expected, actual);
    }
}