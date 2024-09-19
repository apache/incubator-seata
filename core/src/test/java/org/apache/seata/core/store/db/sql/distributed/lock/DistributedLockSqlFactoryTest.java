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

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DistributedLockSqlFactoryTest {

    @Test
    void testGetDistributedLogStoreSqlForMysql() {
        DistributedLockSql sql = DistributedLockSqlFactory.getDistributedLogStoreSql("mysql");
        assertNotNull(sql);
        assertTrue(sql instanceof BaseDistributedLockSql);
    }

    @Test
    void testGetDistributedLogStoreSqlForSqlServer() {
        DistributedLockSql sql = DistributedLockSqlFactory.getDistributedLogStoreSql("sqlserver");
        assertNotNull(sql);
        assertTrue(sql instanceof BaseDistributedLockSqlServer);
    }

    @Test
    void testGetDistributedLogStoreSqlForUnsupportedDb() {
        DistributedLockSql sql = DistributedLockSqlFactory.getDistributedLogStoreSql("unsupported");
        assertNotNull(sql);
        assertTrue(sql instanceof BaseDistributedLockSql);
    }

    @Test
    void testCacheImplementation() {
        DistributedLockSql sql1 = DistributedLockSqlFactory.getDistributedLogStoreSql("mysql");
        DistributedLockSql sql2 = DistributedLockSqlFactory.getDistributedLogStoreSql("mysql");
        assertSame(sql1, sql2);
    }
}