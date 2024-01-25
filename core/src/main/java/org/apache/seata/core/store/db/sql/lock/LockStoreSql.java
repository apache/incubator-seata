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
package org.apache.seata.core.store.db.sql.lock;

/**
 * the database lock store sql interface
 *
 * @since 1.2.0
 */
public interface LockStoreSql {

    /**
     * Get all lock sql string.
     *
     * @param lockTable the lock table
     * @param whereCondition where condition
     * @return the string
     */
    String getAllLockSql(String lockTable, String whereCondition);

    /**
     * Get insert lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getInsertLockSQL(String lockTable);

    /**
     * Get delete lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getDeleteLockSql(String lockTable);

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable      the lock table
     * @param rowSize the size of rowkey
     * @return the string
     */
    String getBatchDeleteLockSql(String lockTable, int rowSize);

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getBatchDeleteLockSqlByBranchId(String lockTable);

    /**
     * Get batch delete lock sql string.
     *
     * @param lockTable      the lock table
     * @return the string
     */
    String getBatchDeleteLockSqlByXid(String lockTable);

    /**
     * Get query lock sql string.
     *
     * @param lockTable the lock table
     * @return the string
     */
    String getQueryLockSql(String lockTable);

    /**
     * Get check lock sql string.
     *
     * @param lockTable      the lock table
     * @param rowSize the size of rowkey
     * @return the string
     */
    String getCheckLockableSql(String lockTable, int rowSize);

    /**
     * get batch update status lock by global sql
     *
     * @param lockTable      the lock table
     * @return the string
     */
    String getBatchUpdateStatusLockByGlobalSql(String lockTable) ;

}
