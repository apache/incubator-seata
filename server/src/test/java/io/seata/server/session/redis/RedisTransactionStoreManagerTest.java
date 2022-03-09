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
import java.util.*;

import com.github.fppt.jedismock.RedisServer;
import io.seata.common.XID;
import io.seata.common.exception.RedisException;
import io.seata.common.util.BeanUtils;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.vo.GlobalLockVO;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.storage.redis.session.RedisSessionManager;
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

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;
import static io.seata.server.storage.SessionConverter.convertToGlobalSessionVo;

/**
 * @author doubleDimple
 */
@SpringBootTest
public class RedisTransactionStoreManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTransactionStoreManagerTest.class);

    private static RedisServer server = null;
    private static RedisTransactionStoreManager redisTransactionStoreManager = null;
    private static SessionManager sessionManager = null;

    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        server = RedisServer.newRedisServer(6789);
        server.start();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "127.0.0.1", 6789, 60000));
        redisTransactionStoreManager = RedisTransactionStoreManager.getInstance();
        RedisSessionManager redisSessionManager = new RedisSessionManager();
        redisSessionManager.setTransactionStoreManager(redisTransactionStoreManager);
        sessionManager = redisSessionManager;
    }

    @Test
    public void testInsertGlobalSessionData() throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.UnKnown);
        sessionManager.addGlobalSession(session);

        GlobalSession session1 = GlobalSession.createGlobalSession("test1", "test1", "test001", 100);
        String xid1 = XID.generateXID(session.getTransactionId());
        session.setXid(xid1);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s1");
        session.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session1);

        GlobalSession session2 = GlobalSession.createGlobalSession("test2", "test2", "test002", 100);
        String xid2 = XID.generateXID(session.getTransactionId());
        session.setXid(xid2);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s1");
        session.setStatus(GlobalStatus.CommitRetrying);
        sessionManager.addGlobalSession(session2);

        GlobalSession session3 = GlobalSession.createGlobalSession("test3", "test3", "test003", 100);
        String xid3 = XID.generateXID(session.getTransactionId());
        session.setXid(xid3);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s1");
        session.setStatus(GlobalStatus.Committed);
        sessionManager.addGlobalSession(session3);

        GlobalSession session4 = GlobalSession.createGlobalSession("test4", "test4", "test004", 100);
        String xid4 = XID.generateXID(session.getTransactionId());
        session.setXid(xid4);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s1");
        session.setStatus(GlobalStatus.Finished);
        sessionManager.addGlobalSession(session4);

        redisTransactionStoreManager.setLogQueryLimit(0);
        List<GlobalSession> globalSessions = redisTransactionStoreManager.readSession(GlobalStatus.values(), true);
        LOGGER.info("the limit All Sessions result is:[{}]",globalSessions);

        //first page
        final List<GlobalSession> globalSessions1 = redisTransactionStoreManager.findGlobalSessionByPage(1, 2, true);
        List<GlobalSessionVO> result = new ArrayList<>();
        convertToGlobalSessionVo(result,globalSessions1);
        LOGGER.info("the first page result is:[{}]",result);

        //second page
        final List<GlobalSession> globalSessions2 = redisTransactionStoreManager.findGlobalSessionByPage(2, 2, true);
        List<GlobalSessionVO> result1 = new ArrayList<>();
        convertToGlobalSessionVo(result1,globalSessions2);
        LOGGER.info("the second page result is:[{}]",result1);

        //third page
        final List<GlobalSession> globalSessions3 = redisTransactionStoreManager.findGlobalSessionByPage(3, 2, true);
        List<GlobalSessionVO> result2 = new ArrayList<>();
        convertToGlobalSessionVo(result2,globalSessions3);
        LOGGER.info("the third page result is:[{}]",result2);

        final List<GlobalSession> globalSessions4 = redisTransactionStoreManager.findGlobalSessionByPage(1, 2, true);
        List<GlobalSessionVO> result3 = new ArrayList<>();
        convertToGlobalSessionVo(result3,globalSessions4);
        LOGGER.info("the third page result is:[{}]",result3);

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
