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
package org.apache.seata.server.console.impl.redis;

import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.vo.BranchSessionVO;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * Test for BranchSessionRedisServiceImpl
 * Integration test that requires real Redis instance
 * Set -DredisCaseEnabled=true to run these tests
 */
@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
@TestPropertySource(properties = {"sessionMode=redis", "lockMode=file"})
class BranchSessionRedisServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private BranchSessionRedisServiceImpl branchSessionRedisService;

    private RedisTransactionStoreManager redisTransactionStoreManager;

    @BeforeEach
    void setUp() {
        redisTransactionStoreManager = RedisTransactionStoreManagerFactory.getInstance();
    }

    @Test
    void queryByXidSuccessTest() throws Exception {
        String xid = "test-console-branch-xid-001";

        // Insert test data
        BranchTransactionDO branchDO1 = new BranchTransactionDO();
        branchDO1.setXid(xid);
        branchDO1.setBranchId(123456L);
        branchDO1.setResourceGroupId("test-app");
        branchDO1.setTransactionId(1001L);
        branchDO1.setResourceId("jdbc:mysql://localhost:3306/test");
        branchDO1.setBranchType("AT");
        branchDO1.setStatus(1);
        branchDO1.setClientId("192.168.1.1:8091");
        branchDO1.setApplicationData("{}");

        BranchTransactionDO branchDO2 = new BranchTransactionDO();
        branchDO2.setXid(xid);
        branchDO2.setBranchId(123457L);
        branchDO2.setResourceGroupId("test-app");
        branchDO2.setTransactionId(1001L);
        branchDO2.setResourceId("jdbc:mysql://localhost:3306/test");
        branchDO2.setBranchType("AT");
        branchDO2.setStatus(1);
        branchDO2.setClientId("192.168.1.1:8091");
        branchDO2.setApplicationData("{}");

        // Write to Redis via reflection
        java.lang.reflect.Method insertMethod = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("insertBranchTransactionDO", BranchTransactionDO.class);
        insertMethod.setAccessible(true);
        insertMethod.invoke(redisTransactionStoreManager, branchDO1);
        insertMethod.invoke(redisTransactionStoreManager, branchDO2);

        try {
            PageResult<BranchSessionVO> result = branchSessionRedisService.queryByXid(xid);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals(2, result.getData().size());
            Assertions.assertEquals(2, result.getTotal());
            Assertions.assertEquals(xid, result.getData().get(0).getXid());
        } finally {
            // Cleanup
            java.lang.reflect.Method deleteMethod = redisTransactionStoreManager
                    .getClass()
                    .getDeclaredMethod("deleteBranchTransactionDO", BranchTransactionDO.class);
            deleteMethod.setAccessible(true);
            deleteMethod.invoke(redisTransactionStoreManager, branchDO1);
            deleteMethod.invoke(redisTransactionStoreManager, branchDO2);
        }
    }

    @Test
    void queryByXidEmptyResultTest() {
        String xid = "test-non-existent-xid-002";

        PageResult<BranchSessionVO> result = branchSessionRedisService.queryByXid(xid);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryByXidBlankXidTest() {
        PageResult<BranchSessionVO> result = branchSessionRedisService.queryByXid("");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryByXidNullXidTest() {
        PageResult<BranchSessionVO> result = branchSessionRedisService.queryByXid(null);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryByXidWhiteSpaceXidTest() {
        PageResult<BranchSessionVO> result = branchSessionRedisService.queryByXid("   ");

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryByXidSingleResultTest() throws Exception {
        String xid = "test-console-branch-xid-single";

        BranchTransactionDO branchDO = new BranchTransactionDO();
        branchDO.setXid(xid);
        branchDO.setBranchId(999L);
        branchDO.setResourceGroupId("single-app");
        branchDO.setTransactionId(9999L);
        branchDO.setResourceId("jdbc:mysql://localhost:3306/test");
        branchDO.setBranchType("AT");
        branchDO.setStatus(1);

        java.lang.reflect.Method insertMethod = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("insertBranchTransactionDO", BranchTransactionDO.class);
        insertMethod.setAccessible(true);
        insertMethod.invoke(redisTransactionStoreManager, branchDO);

        try {
            PageResult<BranchSessionVO> result = branchSessionRedisService.queryByXid(xid);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.isSuccess());
            Assertions.assertEquals(1, result.getData().size());
            Assertions.assertEquals(1, result.getTotal());
            Assertions.assertEquals(999L, Long.parseLong(result.getData().get(0).getBranchId()));
            Assertions.assertEquals("single-app", result.getData().get(0).getResourceGroupId());
        } finally {
            // Cleanup
            java.lang.reflect.Method deleteMethod = redisTransactionStoreManager
                    .getClass()
                    .getDeclaredMethod("deleteBranchTransactionDO", BranchTransactionDO.class);
            deleteMethod.setAccessible(true);
            deleteMethod.invoke(redisTransactionStoreManager, branchDO);
        }
    }
}
