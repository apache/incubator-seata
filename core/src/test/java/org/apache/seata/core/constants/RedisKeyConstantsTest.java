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
package org.apache.seata.core.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link RedisKeyConstants}.
 */
public class RedisKeyConstantsTest {

    @Test
    void testGlobalTransactionKeyConstants() {
        assertEquals("xid", RedisKeyConstants.REDIS_KEY_GLOBAL_XID);
        assertEquals("transactionId", RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_ID);
        assertEquals("status", RedisKeyConstants.REDIS_KEY_GLOBAL_STATUS);
        assertEquals("applicationId", RedisKeyConstants.REDIS_KEY_GLOBAL_APPLICATION_ID);
        assertEquals("transactionServiceGroup", RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_SERVICE_GROUP);
        assertEquals("transactionName", RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_NAME);
        assertEquals("timeout", RedisKeyConstants.REDIS_KEY_GLOBAL_TIMEOUT);
        assertEquals("beginTime", RedisKeyConstants.REDIS_KEY_GLOBAL_BEGIN_TIME);
        assertEquals("applicationData", RedisKeyConstants.REDIS_KEY_GLOBAL_APPLICATION_DATA);
        assertEquals("gmtCreate", RedisKeyConstants.REDIS_KEY_GLOBAL_GMT_CREATE);
        assertEquals("gmtModified", RedisKeyConstants.REDIS_KEY_GLOBAL_GMT_MODIFIED);
    }

    @Test
    void testBranchTransactionKeyConstants() {
        assertEquals("branchId", RedisKeyConstants.REDIS_KEY_BRANCH_BRANCH_ID);
        assertEquals("xid", RedisKeyConstants.REDIS_KEY_BRANCH_XID);
        assertEquals("transactionId", RedisKeyConstants.REDIS_KEY_BRANCH_TRANSACTION_ID);
        assertEquals("resourceGroupId", RedisKeyConstants.REDIS_KEY_BRANCH_RESOURCE_GROUP_ID);
        assertEquals("resourceId", RedisKeyConstants.REDIS_KEY_BRANCH_RESOURCE_ID);
        assertEquals("branchType", RedisKeyConstants.REDIS_KEY_BRANCH_BRANCH_TYPE);
        assertEquals("status", RedisKeyConstants.REDIS_KEY_BRANCH_STATUS);
        assertEquals("beginTime", RedisKeyConstants.REDIS_KEY_BRANCH_BEGIN_TIME);
        assertEquals("applicationData", RedisKeyConstants.REDIS_KEY_BRANCH_APPLICATION_DATA);
        assertEquals("clientId", RedisKeyConstants.REDIS_KEY_BRANCH_CLIENT_ID);
        assertEquals("gmtCreate", RedisKeyConstants.REDIS_KEY_BRANCH_GMT_CREATE);
        assertEquals("gmtModified", RedisKeyConstants.REDIS_KEY_BRANCH_GMT_MODIFIED);
    }

    @Test
    void testLockPrefixConstants() {
        assertEquals("SEATA_GLOBAL_LOCK", RedisKeyConstants.DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX);
        assertEquals("SEATA_ROW_LOCK_", RedisKeyConstants.DEFAULT_REDIS_SEATA_ROW_LOCK_PREFIX);
    }

    @Test
    void testSplitConstant() {
        assertEquals("^^^", RedisKeyConstants.SPLIT);
    }

    @Test
    void testDefaultLogQueryLimit() {
        assertEquals(100, RedisKeyConstants.DEFAULT_LOG_QUERY_LIMIT);
    }

    @Test
    void testConstantsAreNotNull() {
        assertNotNull(RedisKeyConstants.REDIS_KEY_GLOBAL_XID);
        assertNotNull(RedisKeyConstants.REDIS_KEY_GLOBAL_TRANSACTION_ID);
        assertNotNull(RedisKeyConstants.REDIS_KEY_BRANCH_BRANCH_ID);
        assertNotNull(RedisKeyConstants.DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX);
        assertNotNull(RedisKeyConstants.SPLIT);
    }

    @Test
    void testConstantsAreNotEmpty() {
        assertFalse(RedisKeyConstants.REDIS_KEY_GLOBAL_XID.isEmpty());
        assertFalse(RedisKeyConstants.REDIS_KEY_BRANCH_XID.isEmpty());
        assertFalse(RedisKeyConstants.DEFAULT_REDIS_SEATA_GLOBAL_LOCK_PREFIX.isEmpty());
        assertFalse(RedisKeyConstants.SPLIT.isEmpty());
    }
}
