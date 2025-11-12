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

import org.apache.seata.common.exception.FrameworkErrorCode;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.GlobalSessionParam;
import org.apache.seata.server.console.entity.vo.GlobalSessionVO;
import org.apache.seata.server.storage.redis.JedisPooledFactory;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * Test for GlobalSessionRedisServiceImpl
 * Integration test that requires real Redis instance
 * Set -DredisCaseEnabled=true to run these tests
 */
@EnabledIfSystemProperty(named = "redisCaseEnabled", matches = "true")
@TestPropertySource(properties = {"sessionMode=redis", "lockMode=file"})
class GlobalSessionRedisServiceImplTest extends BaseSpringBootTest {

    @Autowired
    private GlobalSessionRedisServiceImpl globalSessionRedisService;

    private RedisTransactionStoreManager redisTransactionStoreManager;
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        redisTransactionStoreManager = RedisTransactionStoreManagerFactory.getInstance();
        jedis = JedisPooledFactory.getJedisInstance();
        cleanupRedisSessionData();
    }

    @AfterEach
    void tearDown() {
        cleanupRedisSessionData();
        if (jedis != null) {
            jedis.close();
        }
    }

    private void cleanupRedisSessionData() {
        Set<String> keys = jedis.keys("SEATA_GLOBAL_*");
        if (keys != null && !keys.isEmpty()) {
            jedis.del(keys.toArray(new String[0]));
        }

        keys = jedis.keys("SEATA_STATUS_*");
        if (keys != null && !keys.isEmpty()) {
            for (String key : keys) {
                jedis.del(key);
            }
        }

        jedis.del("SEATA_BEGIN_TRANSACTIONS");
    }

    private void createTestGlobalTransaction(String xid, long transactionId, GlobalStatus status) throws Exception {
        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(xid);
        globalTransactionDO.setTransactionId(transactionId);
        globalTransactionDO.setStatus(status.getCode());
        globalTransactionDO.setApplicationId("test-app");
        globalTransactionDO.setTransactionServiceGroup("default_tx_group");
        globalTransactionDO.setTransactionName("test-transaction-" + transactionId);
        globalTransactionDO.setTimeout(60000);
        globalTransactionDO.setBeginTime(System.currentTimeMillis());

        java.lang.reflect.Method method = redisTransactionStoreManager
                .getClass()
                .getDeclaredMethod("insertGlobalTransactionDO", GlobalTransactionDO.class);
        method.setAccessible(true);
        method.invoke(redisTransactionStoreManager, globalTransactionDO);
    }

    @Test
    void queryAllSessionsTest() throws Exception {
        createTestGlobalTransaction("127.0.0.1:8091:1001", 1001L, GlobalStatus.Begin);
        createTestGlobalTransaction("127.0.0.1:8091:1002", 1002L, GlobalStatus.Begin);

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setWithBranch(false);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(2, result.getData().size());
        Assertions.assertEquals(2, result.getTotal());
        Assertions.assertEquals(1, result.getPageNum());
        Assertions.assertEquals(10, result.getPageSize());
    }

    @Test
    void queryByXidTest() throws Exception {
        createTestGlobalTransaction("127.0.0.1:8091:1001", 1001L, GlobalStatus.Begin);

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("127.0.0.1:8091:1001");
        param.setWithBranch(false);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertEquals(1, result.getTotal());
        Assertions.assertEquals("127.0.0.1:8091:1001", result.getData().get(0).getXid());
    }

    @Test
    void queryByStatusTest() throws Exception {
        createTestGlobalTransaction("127.0.0.1:8091:1001", 1001L, GlobalStatus.Begin);

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setStatus(GlobalStatus.Begin.getCode());
        param.setWithBranch(false);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertEquals(1, result.getTotal());
    }

    @Test
    void queryByXidAndStatusTest() throws Exception {
        createTestGlobalTransaction("127.0.0.1:8091:1001", 1001L, GlobalStatus.Begin);

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("127.0.0.1:8091:1001");
        param.setStatus(GlobalStatus.Begin.getCode());
        param.setWithBranch(false);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getData().size());
        Assertions.assertEquals(1, result.getTotal());
    }

    @Test
    void queryEmptyResultTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("127.0.0.1:8091:9999");
        param.setWithBranch(false);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }

    @Test
    void queryTimeRangeNotSupportedTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setTimeStart(System.currentTimeMillis() - 86400000L);
        param.setTimeEnd(System.currentTimeMillis());

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals(FrameworkErrorCode.ParameterRequired.getErrCode(), result.getCode());
        Assertions.assertTrue(result.getMessage().contains("not supported according to time range query"));
    }

    @Test
    void queryWithBranchTest() throws Exception {
        createTestGlobalTransaction("127.0.0.1:8091:1001", 1001L, GlobalStatus.Begin);

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setXid("127.0.0.1:8091:1001");
        param.setWithBranch(true);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(1, result.getData().size());
    }

    @Test
    void queryInvalidPageNumTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(0);
        param.setPageSize(10);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalSessionRedisService.query(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void queryInvalidPageSizeTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(0);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> globalSessionRedisService.query(param));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void queryAllSessionsEmptyResultTest() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(10);
        param.setWithBranch(false);

        PageResult<GlobalSessionVO> result = globalSessionRedisService.query(param);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertEquals(0, result.getData().size());
        Assertions.assertEquals(0, result.getTotal());
    }
}
