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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * the base distributed lock sql test.
 * @author cx
 */
public class BaseDistributedLockSqlTest {

    private static final String GLOBAL_TABLE = "global_table";

    private static final String BRANCH_TABLE = "branch_table";

    private static final String EXPECT_INSERT_GLOBAL_SQL = "INSERT INTO " + GLOBAL_TABLE + "(lock_key,lock_value,expire) VALUES (?, ?, ?)";

    private static final String EXPECT_INSERT_BRANCH_SQL = "INSERT INTO " + BRANCH_TABLE + "(lock_key,lock_value,expire) VALUES (?, ?, ?)";

    private static final String EXPECT_UPDATE_GLOBAL_SQL = "UPDATE " + GLOBAL_TABLE + " SET lock_value=?, expire=? WHERE lock_key=?";

    private static final String EXPECT_UPDATE_BRANCH_SQL = "UPDATE " + BRANCH_TABLE + " SET lock_value=?, expire=? WHERE lock_key=?";

    private static final String EXPECT_SELECT_FOR_UPDATE_GLOBAL_SQL = "SELECT lock_key,lock_value,expire FROM " + GLOBAL_TABLE + " WHERE lock_key = ? FOR UPDATE";

    private static final String EXPECT_SELECT_FOR_UPDATE_BRANCH_SQL = "SELECT lock_key,lock_value,expire FROM " + BRANCH_TABLE + " WHERE lock_key = ? FOR UPDATE";

    private static final DistributedLockSql DISTRIBUTEDLOCKSQL = DistributedLockSqlFactory.getDistributedLogStoreSql(" ");

    @Test
    public void testGetInsertSql() {
        String sql;
        sql = DISTRIBUTEDLOCKSQL.getInsertSql(GLOBAL_TABLE);
        Assertions.assertEquals(EXPECT_INSERT_GLOBAL_SQL, sql);
        sql = DISTRIBUTEDLOCKSQL.getInsertSql(BRANCH_TABLE);
        Assertions.assertEquals(EXPECT_INSERT_BRANCH_SQL, sql);
    }

    @Test
    public void testGetUpdateSql() {
        String sql;
        sql = DISTRIBUTEDLOCKSQL.getUpdateSql(GLOBAL_TABLE);
        Assertions.assertEquals(EXPECT_UPDATE_GLOBAL_SQL, sql);
        sql = DISTRIBUTEDLOCKSQL.getUpdateSql(BRANCH_TABLE);
        Assertions.assertEquals(EXPECT_UPDATE_BRANCH_SQL, sql);
    }

    @Test
    public void testGetSelectDistributeForUpdateSql() {
        String sql;
        sql = DISTRIBUTEDLOCKSQL.getSelectDistributeForUpdateSql(GLOBAL_TABLE);
        Assertions.assertEquals(EXPECT_SELECT_FOR_UPDATE_GLOBAL_SQL, sql);
        sql = DISTRIBUTEDLOCKSQL.getSelectDistributeForUpdateSql(BRANCH_TABLE);
        Assertions.assertEquals(EXPECT_SELECT_FOR_UPDATE_BRANCH_SQL, sql);
    }

}
