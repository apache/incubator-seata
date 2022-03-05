/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.session.redis;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.github.fppt.jedismock.RedisServer;
import io.seata.common.exception.RedisException;
import io.seata.common.util.BeanUtils;
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.server.console.vo.GlobalLockVO;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.session.GlobalSession;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.Pipeline;

/**
 * @author doubleDimple
 */
@SpringBootTest
public class RedisTransactionStoreManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTransactionStoreManagerTest.class);

    private static RedisServer server = null;
    private static RedisTransactionStoreManager redisTransactionStoreManager = null;

    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        server = RedisServer.newRedisServer(6789);
        server.start();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6789, 60000));
        redisTransactionStoreManager = RedisTransactionStoreManager.getInstance();
    }

    @Test
    public void testInsertGlobalSessionData(){
        GlobalStatus[] values = GlobalStatus.values();
        for (int i = 0;i<15;i++) {
            GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
            globalTransactionDO.setXid("123123123123"+i);
            globalTransactionDO.setStatus(values[i].getCode());
            globalTransactionDO.setApplicationId("test_"+i);
            globalTransactionDO.setTransactionServiceGroup("redis-group");
            globalTransactionDO.setTransactionName("test-redis");
            globalTransactionDO.setTimeout(30);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            globalTransactionDO.setTransactionId(i);
            globalTransactionDO.setApplicationData("123");
            globalTransactionDO.setGmtCreate(new Date());
            globalTransactionDO.setGmtModified(new Date());
            globalTransactionDO.setTransactionId(12312342l);
            globalTransactionDO.setBeginTime(System.currentTimeMillis());
            String globalKey = "SEATA_GLOBAL_"+"test_"+i;
            try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
                Date now = new Date();
                globalTransactionDO.setGmtCreate(now);
                globalTransactionDO.setGmtModified(now);
                Pipeline pipelined = jedis.pipelined();
                pipelined.hmset(globalKey, BeanUtils.objectToMap(globalTransactionDO));
                pipelined.rpush("SEATA_STATUS_"+(globalTransactionDO.getStatus()), globalTransactionDO.getXid());
                pipelined.sync();
            } catch (Exception ex) {
                throw new RedisException(ex);
            }
        }

    }

    @Test
    public void testInsertGlobalLockData() {
        String GLOBAL_LOCK_KEY = "SEATA_GLOBAL_LOCK_192.168.158.80:8091:37621364385185792";
        String ROW_LOCK_KEY = "SEATA_ROW_LOCK_jdbc:mysql://116.62.62.26/seata-order^^^order^^^2188";
        Map<String, String> globallockMap = new HashMap<>();
        globallockMap.put("137621367686103001","SEATA_ROW_LOCK_jdbc:mysql://116.62.62.26/seata-order^^^order^^^2188;SEATA_ROW_LOCK_jdbc:mysql://116.62.62.26/seata-storage^^^storage^^^1");
        globallockMap.put("237621367686103002","SEATA_ROW_LOCK_jdbc:mysql://116.62.62.26/seata-storage^^^storage^^^1");
        GlobalLockVO globalLockVO = new GlobalLockVO();
        globalLockVO.setXid("192.168.158.80:8091:37621364385185792");
        globalLockVO.setTransactionId(37621364385185792L);
        globalLockVO.setBranchId(37621367686103000L);
        globalLockVO.setResourceId("jdbc:mysql://116.62.62.26/seata-order");
        globalLockVO.setTableName("order");
        globalLockVO.setPk("2188");
        globalLockVO.setRowKey("jdbc:mysql://116.62.62.26/seata-order^^^order^^^2188");
        globalLockVO.setGmtCreate(new Date());
        globalLockVO.setGmtModified(new Date());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.hmset(GLOBAL_LOCK_KEY,globallockMap);
            jedis.hmset(ROW_LOCK_KEY,BeanUtils.objectToMap(globalLockVO));
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Test
    public void testQueryGlobalslSession() {
        Long count = redisTransactionStoreManager.countByClobalSesisons(GlobalStatus.values());
        LOGGER.info("the count is:[{}]",count);
    }

    @Test
    public void testreadisQuery() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(0);
        param.setPageSize(5);
        param.setWithBranch(false);
        List<GlobalSession> globalSessionKeys = redisTransactionStoreManager.findGlobalSessionByPage(param.getPageNum(), param.getPageSize(), param.isWithBranch());
        LOGGER.info("the result size is:[{}]",globalSessionKeys.size());
        LOGGER.info("the globalSessionKeys is:[{}]",globalSessionKeys);
    }

    @Test
    public void testLimitAllSessions() {
        redisTransactionStoreManager.setLogQueryLimit(20);
        List<GlobalSession> globalSessions = redisTransactionStoreManager.readSession(GlobalStatus.values(), true);
        LOGGER.info("the limit All Sessions result is:[{}]",globalSessions);
    }

    @AfterAll
    public static void after() {
        server.stop();
        server = null;
    }

}
