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
package org.apache.seata.server.storage.redis.store;

import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import redis.clients.jedis.Jedis;

import java.util.List;
import java.util.Map;
import java.util.Set;

@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
public class RedisTransactionStoreManagerTest extends BaseSpringBootTest {

    private RedisTransactionStoreManager redisTransactionStoreManager;

    @BeforeEach
    public void setUp() {
        redisTransactionStoreManager = new RedisTransactionStoreManager();
    }

    // Helper methods to invoke protected methods via reflection
    private boolean invokeInsertBranchTransactionDO(BranchTransactionDO branchTransactionDO) throws Exception {
        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("insertBranchTransactionDO", BranchTransactionDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(redisTransactionStoreManager, branchTransactionDO);
    }

    private boolean invokeDeleteBranchTransactionDO(BranchTransactionDO branchTransactionDO) throws Exception {
        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("deleteBranchTransactionDO", BranchTransactionDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(redisTransactionStoreManager, branchTransactionDO);
    }

    private boolean invokeUpdateBranchTransactionDO(BranchTransactionDO branchTransactionDO) throws Exception {
        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("updateBranchTransactionDO", BranchTransactionDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(redisTransactionStoreManager, branchTransactionDO);
    }

    private boolean invokeInsertGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) throws Exception {
        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("insertGlobalTransactionDO", GlobalTransactionDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(redisTransactionStoreManager, globalTransactionDO);
    }

    private boolean invokeDeleteGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) throws Exception {
        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("deleteGlobalTransactionDO", GlobalTransactionDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(redisTransactionStoreManager, globalTransactionDO);
    }

    private boolean invokeUpdateGlobalTransactionDO(GlobalTransactionDO globalTransactionDO) throws Exception {
        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("updateGlobalTransactionDO", GlobalTransactionDO.class);
        method.setAccessible(true);
        return (boolean) method.invoke(redisTransactionStoreManager, globalTransactionDO);
    }

    @Test
    public void testInsertBranchTransactionDO() throws Exception {
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid("testXid:123");
        branchTransactionDO.setBranchId(1001L);
        branchTransactionDO.setTransactionId(123L);
        branchTransactionDO.setResourceId("jdbc:mysql://localhost:3306/test");
        branchTransactionDO.setResourceGroupId("testGroup");
        branchTransactionDO.setBranchType("AT");
        branchTransactionDO.setStatus(1);
        branchTransactionDO.setClientId("testClient");
        branchTransactionDO.setApplicationData("testData");

        boolean result = invokeInsertBranchTransactionDO(branchTransactionDO);

        Assertions.assertTrue(result);

        // Verify data in Redis
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String branchKey = "SEATA_BRANCH_" + branchTransactionDO.getBranchId();
            Map<String, String> branchData = jedis.hgetAll(branchKey);

            Assertions.assertNotNull(branchData);
            Assertions.assertEquals("testXid:123", branchData.get("xid"));
            Assertions.assertEquals("1001", branchData.get("branchId"));
            Assertions.assertEquals("jdbc:mysql://localhost:3306/test", branchData.get("resourceId"));
            Assertions.assertNotNull(branchData.get("gmtCreate"));
            Assertions.assertNotNull(branchData.get("gmtModified"));

            // Verify branch list
            String branchListKey = "SEATA_BRANCHES_testXid:123";
            List<String> branchList = jedis.lrange(branchListKey, 0, -1);
            Assertions.assertTrue(branchList.contains(branchKey));

            // Cleanup
            jedis.del(branchKey);
            jedis.del(branchListKey);
        }
    }

    @Test
    public void testDeleteBranchTransactionDO() throws Exception {
        // Setup: insert a branch transaction first
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid("testXid:456");
        branchTransactionDO.setBranchId(2001L);
        branchTransactionDO.setTransactionId(456L);
        branchTransactionDO.setResourceId("jdbc:mysql://localhost:3306/test");
        branchTransactionDO.setStatus(1);

        invokeInsertBranchTransactionDO(branchTransactionDO);

        // Test delete
        boolean result = invokeDeleteBranchTransactionDO(branchTransactionDO);
        Assertions.assertTrue(result);

        // Verify deletion
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String branchKey = "SEATA_BRANCH_" + branchTransactionDO.getBranchId();
            Map<String, String> branchData = jedis.hgetAll(branchKey);
            Assertions.assertTrue(branchData.isEmpty());

            String branchListKey = "SEATA_BRANCHES_testXid:456";
            List<String> branchList = jedis.lrange(branchListKey, 0, -1);
            Assertions.assertFalse(branchList.contains(branchKey));

            // Cleanup
            jedis.del(branchListKey);
        }
    }

    @Test
    public void testDeleteBranchTransactionDO_NotExist() throws Exception {
        // Test delete non-existent branch - should return true
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid("nonExistentXid:999");
        branchTransactionDO.setBranchId(9999L);

        boolean result = invokeDeleteBranchTransactionDO(branchTransactionDO);
        Assertions.assertTrue(result);
    }

    @Test
    public void testUpdateBranchTransactionDO() throws Exception {
        // Setup: insert a branch transaction first
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid("testXid:789");
        branchTransactionDO.setBranchId(3001L);
        branchTransactionDO.setTransactionId(789L);
        branchTransactionDO.setResourceId("jdbc:mysql://localhost:3306/test");
        branchTransactionDO.setStatus(1);
        branchTransactionDO.setApplicationData("originalData");

        invokeInsertBranchTransactionDO(branchTransactionDO);

        // Update status and application data
        branchTransactionDO.setStatus(2);
        branchTransactionDO.setApplicationData("updatedData");

        boolean result = invokeUpdateBranchTransactionDO(branchTransactionDO);
        Assertions.assertTrue(result);

        // Verify update
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String branchKey = "SEATA_BRANCH_" + branchTransactionDO.getBranchId();
            Map<String, String> branchData = jedis.hgetAll(branchKey);

            Assertions.assertEquals("2", branchData.get("status"));
            Assertions.assertEquals("updatedData", branchData.get("applicationData"));
            Assertions.assertNotNull(branchData.get("gmtModified"));

            // Cleanup
            jedis.del(branchKey);
            jedis.del("SEATA_BRANCHES_testXid:789");
        }
    }

    @Test
    public void testUpdateBranchTransactionDO_NotExist() throws Exception {
        // Test update non-existent branch - should throw StoreException
        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid("nonExistentXid:999");
        branchTransactionDO.setBranchId(9999L);
        branchTransactionDO.setStatus(2);

        Assertions.assertThrows(Exception.class, () -> {
            invokeUpdateBranchTransactionDO(branchTransactionDO);
        });
    }

    @Test
    public void testInsertGlobalTransactionDO() throws Exception {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("testGlobalXid:111");
        globalTransactionDO.setTransactionId(111L);
        globalTransactionDO.setStatus(GlobalStatus.Begin.getCode());
        globalTransactionDO.setApplicationId("testApp");
        globalTransactionDO.setTransactionServiceGroup("testGroup");
        globalTransactionDO.setTransactionName("testTx");
        globalTransactionDO.setTimeout(60000);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());
        globalTransactionDO.setApplicationData("testGlobalData");

        boolean result = invokeInsertGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(result);

        // Verify data in Redis
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String globalKey = "SEATA_GLOBAL_" + globalTransactionDO.getTransactionId();
            Map<String, String> globalData = jedis.hgetAll(globalKey);

            Assertions.assertNotNull(globalData);
            Assertions.assertEquals("testGlobalXid:111", globalData.get("xid"));
            Assertions.assertEquals(String.valueOf(GlobalStatus.Begin.getCode()), globalData.get("status"));
            Assertions.assertEquals("testApp", globalData.get("applicationId"));
            Assertions.assertNotNull(globalData.get("gmtCreate"));
            Assertions.assertNotNull(globalData.get("gmtModified"));

            // Verify status list
            String statusKey = "SEATA_STATUS_" + GlobalStatus.Begin.getCode();
            List<String> statusList = jedis.lrange(statusKey, 0, -1);
            Assertions.assertTrue(statusList.contains("testGlobalXid:111"));

            // Verify timeout sorted set
            Set<String> timeoutSet = jedis.zrangeByScore("SEATA_BEGIN_TRANSACTIONS", 0, Double.MAX_VALUE);
            Assertions.assertTrue(timeoutSet.contains(globalKey));

            // Cleanup
            jedis.del(globalKey);
            jedis.lrem(statusKey, 0, "testGlobalXid:111");
            jedis.zrem("SEATA_BEGIN_TRANSACTIONS", globalKey);
        }
    }

    @Test
    public void testDeleteGlobalTransactionDO() throws Exception {
        // Setup: insert a global transaction first
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("testGlobalXid:222");
        globalTransactionDO.setTransactionId(222L);
        globalTransactionDO.setStatus(GlobalStatus.Begin.getCode());
        globalTransactionDO.setApplicationId("testApp");
        globalTransactionDO.setTransactionServiceGroup("testGroup");
        globalTransactionDO.setTimeout(60000);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());

        invokeInsertGlobalTransactionDO(globalTransactionDO);

        // Test delete
        boolean result = invokeDeleteGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(result);

        // Verify deletion
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String globalKey = "SEATA_GLOBAL_" + globalTransactionDO.getTransactionId();
            Map<String, String> globalData = jedis.hgetAll(globalKey);
            Assertions.assertTrue(globalData.isEmpty());

            String statusKey = "SEATA_STATUS_" + GlobalStatus.Begin.getCode();
            List<String> statusList = jedis.lrange(statusKey, 0, -1);
            Assertions.assertFalse(statusList.contains("testGlobalXid:222"));

            Set<String> timeoutSet = jedis.zrangeByScore("SEATA_BEGIN_TRANSACTIONS", 0, Double.MAX_VALUE);
            Assertions.assertFalse(timeoutSet.contains(globalKey));
        }
    }

    @Test
    public void testDeleteGlobalTransactionDO_NotExist() throws Exception {
        // Test delete non-existent global transaction - should return true and log warning
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("nonExistentGlobalXid:999");
        globalTransactionDO.setTransactionId(999L);
        globalTransactionDO.setStatus(GlobalStatus.Begin.getCode());

        boolean result = invokeDeleteGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(result);
    }

    @Test
    public void testUpdateGlobalTransactionDO() throws Exception {
        // Setup: insert a global transaction first
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("testGlobalXid:333");
        globalTransactionDO.setTransactionId(333L);
        globalTransactionDO.setStatus(GlobalStatus.Begin.getCode());
        globalTransactionDO.setApplicationId("testApp");
        globalTransactionDO.setTransactionServiceGroup("testGroup");
        globalTransactionDO.setTimeout(60000);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());

        invokeInsertGlobalTransactionDO(globalTransactionDO);

        // Update status to Committing
        globalTransactionDO.setStatus(GlobalStatus.Committing.getCode());

        boolean result = invokeUpdateGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(result);

        // Verify update
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String globalKey = "SEATA_GLOBAL_" + globalTransactionDO.getTransactionId();
            Map<String, String> globalData = jedis.hgetAll(globalKey);

            Assertions.assertEquals(String.valueOf(GlobalStatus.Committing.getCode()), globalData.get("status"));
            Assertions.assertNotNull(globalData.get("gmtModified"));

            // Verify status list updated
            String oldStatusKey = "SEATA_STATUS_" + GlobalStatus.Begin.getCode();
            List<String> oldStatusList = jedis.lrange(oldStatusKey, 0, -1);
            Assertions.assertFalse(oldStatusList.contains("testGlobalXid:333"));

            String newStatusKey = "SEATA_STATUS_" + GlobalStatus.Committing.getCode();
            List<String> newStatusList = jedis.lrange(newStatusKey, 0, -1);
            Assertions.assertTrue(newStatusList.contains("testGlobalXid:333"));

            // Cleanup
            jedis.del(globalKey);
            jedis.lrem(newStatusKey, 0, "testGlobalXid:333");
        }
    }

    @Test
    public void testUpdateGlobalTransactionDO_SameStatus() throws Exception {
        // Setup: insert a global transaction
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("testGlobalXid:444");
        globalTransactionDO.setTransactionId(444L);
        globalTransactionDO.setStatus(GlobalStatus.Begin.getCode());
        globalTransactionDO.setApplicationId("testApp");
        globalTransactionDO.setTransactionServiceGroup("testGroup");
        globalTransactionDO.setTimeout(60000);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());

        invokeInsertGlobalTransactionDO(globalTransactionDO);

        // Update with same status - should return true immediately
        boolean result = invokeUpdateGlobalTransactionDO(globalTransactionDO);
        Assertions.assertTrue(result);

        // Cleanup
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            String globalKey = "SEATA_GLOBAL_" + globalTransactionDO.getTransactionId();
            jedis.del(globalKey);
            String statusKey = "SEATA_STATUS_" + GlobalStatus.Begin.getCode();
            jedis.lrem(statusKey, 0, "testGlobalXid:444");
            jedis.zrem("SEATA_BEGIN_TRANSACTIONS", globalKey);
        }
    }

    @Test
    public void testUpdateGlobalTransactionDO_NotExist() throws Exception {
        // Test update non-existent global transaction - should throw StoreException
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid("nonExistentGlobalXid:999");
        globalTransactionDO.setTransactionId(999L);
        globalTransactionDO.setStatus(GlobalStatus.Committing.getCode());

        Assertions.assertThrows(Exception.class, () -> {
            invokeUpdateGlobalTransactionDO(globalTransactionDO);
        });
    }
}
