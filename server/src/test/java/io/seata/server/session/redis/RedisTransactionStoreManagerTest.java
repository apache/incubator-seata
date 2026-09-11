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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import io.seata.common.XID;
import io.seata.common.exception.RedisException;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.common.util.BeanUtils;
import io.seata.common.util.CollectionUtils;
import io.seata.core.exception.TransactionException;
import io.seata.server.console.param.GlobalSessionParam;
import io.seata.server.console.vo.GlobalLockVO;
import io.seata.core.model.GlobalStatus;
import io.seata.server.console.vo.GlobalSessionVO;
import io.seata.server.session.GlobalSession;
import io.seata.server.session.SessionCondition;
import io.seata.server.session.SessionManager;
import io.seata.server.storage.redis.JedisPooledFactory;
import io.seata.server.storage.redis.session.RedisSessionManager;
import io.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;


import static io.seata.server.storage.SessionConverter.convertToGlobalSessionVo;

/**
 * @author doubleDimple
 */
@SpringBootTest
public class RedisTransactionStoreManagerTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(RedisTransactionStoreManagerTest.class);

    private static volatile RedisTransactionStoreManager redisTransactionStoreManager = null;
    private static volatile SessionManager sessionManager = null;

    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        MockRedisServer.getInstance();
        EnhancedServiceLoader.unloadAll();
        JedisPooledFactory.getJedisInstance().flushAll();
        redisTransactionStoreManager = RedisTransactionStoreManager.getInstance();
        RedisSessionManager redisSessionManager = new RedisSessionManager();
        redisSessionManager.setTransactionStoreManager(redisTransactionStoreManager);
        sessionManager = redisSessionManager;
    }

    @Test
    public synchronized void testBeginSortByTimeoutQuery() throws TransactionException, InterruptedException {
        GlobalSession session1 = GlobalSession.createGlobalSession("test1", "test2", "test001", 500);
        String xid1 = XID.generateXID(session1.getTransactionId());
        session1.setXid(xid1);
        session1.setTransactionId(session1.getTransactionId());
        session1.setBeginTime(System.currentTimeMillis());
        session1.setApplicationData("abc=878s1");
        session1.setStatus(GlobalStatus.Begin);
        GlobalSession session2 = GlobalSession.createGlobalSession("test3", "test4", "test002", 450);
        String xid2 = XID.generateXID(session2.getTransactionId());
        session2.setXid(xid2);
        session2.setTransactionId(session2.getTransactionId());
        session2.setBeginTime(System.currentTimeMillis());
        session2.setApplicationData("abc1=878s2");
        session2.setStatus(GlobalStatus.Begin);
        SessionCondition sessionCondition = new SessionCondition(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session1);
        sessionManager.addGlobalSession(session2);
        List<GlobalSession> list = sessionManager.findGlobalSessions(sessionCondition);
        for (GlobalSession globalSession : list) {
            LOGGER.info("sorted xid: {},timeout: {}",globalSession.getXid(),globalSession.getTimeout()+globalSession.getBeginTime());
        }
        List<GlobalSession> list2 = (List<GlobalSession>)sessionManager.allSessions();
        for (GlobalSession globalSession : list2) {
            LOGGER.info("xid: {},timeout: {}",globalSession.getXid(),globalSession.getTimeout()+globalSession.getBeginTime());
        }
        Assertions.assertEquals(2, list2.size());
        if(CollectionUtils.isNotEmpty(list)) {
            Assertions.assertEquals(xid1, list.size() > 1 ? list.get(1).getXid() : list.get(0));
            if (list.size() > 1 && list2.size() > 1) {
                Assertions.assertNotEquals(list2.get(0).getXid(), list.get(0).getXid());
            }
            sessionManager.removeGlobalSession(session1);
            sessionManager.removeGlobalSession(session2);
        }
    }

    @Test
    public synchronized void testInsertGlobalSessionDataAndQuery() throws TransactionException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(session.getTransactionId());
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session);

        GlobalSession session1 = GlobalSession.createGlobalSession("test1", "test2", "test001", 100);
        String xid1 = XID.generateXID(session1.getTransactionId());
        session1.setXid(xid1);
        session1.setTransactionId(session1.getTransactionId());
        session1.setBeginTime(System.currentTimeMillis());
        session1.setApplicationData("abc=878s1");
        session1.setStatus(GlobalStatus.UnKnown);
        sessionManager.addGlobalSession(session1);

        GlobalSession session2 = GlobalSession.createGlobalSession("test3", "test4", "test002", 100);
        String xid2 = XID.generateXID(session2.getTransactionId());
        session2.setXid(xid2);
        session2.setTransactionId(session2.getTransactionId());
        session2.setBeginTime(System.currentTimeMillis());
        session2.setApplicationData("abc1=878s2");
        session2.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session2);

        GlobalSession session3 = GlobalSession.createGlobalSession("test5", "test6", "test003", 100);
        String xid3 = XID.generateXID(session3.getTransactionId());
        session3.setXid(xid3);
        session3.setTransactionId(session3.getTransactionId());
        session3.setBeginTime(System.currentTimeMillis());
        session3.setApplicationData("abc2=878s3");
        session3.setStatus(GlobalStatus.Begin);
        sessionManager.addGlobalSession(session3);

        GlobalSession session4 = GlobalSession.createGlobalSession("test7", "test8", "test004", 100);
        String xid4 = XID.generateXID(session4.getTransactionId());
        session4.setXid(xid4);
        session4.setTransactionId(session4.getTransactionId());
        session4.setBeginTime(System.currentTimeMillis());
        session4.setApplicationData("abc3=878s1");
        session4.setStatus(GlobalStatus.Finished);
        sessionManager.addGlobalSession(session4);

        GlobalSession session5 = GlobalSession.createGlobalSession("test9", "test10", "test005", 100);
        String xid5 = XID.generateXID(session5.getTransactionId());
        session5.setXid(xid5);
        session5.setTransactionId(session5.getTransactionId());
        session5.setBeginTime(System.currentTimeMillis());
        session5.setApplicationData("abc3=878s1");
        session5.setStatus(GlobalStatus.Finished);
        sessionManager.addGlobalSession(session5);

        //first:  setLogQueryLimit > totalCount
        //second: setLogQueryLimit = totalCount
        //third:  setLogQueryLimit < totalCount
        redisTransactionStoreManager.setLogQueryLimit(3);
        List<GlobalSession> globalSessions = redisTransactionStoreManager.readSession(GlobalStatus.values(), false);
        LOGGER.info("the limit  Sessions result is:[{}]",globalSessions);
        LOGGER.info("the limit  Sessions result size is:[{}]",globalSessions.size());

        //first page
        final List<GlobalSession> globalSessions1 = redisTransactionStoreManager.findGlobalSessionByPage(1, 2, true);
        List<GlobalSessionVO> result = new ArrayList<>();
        convertToGlobalSessionVo(result,globalSessions1);
        LOGGER.info("the first page result is:[{}]",result);
        LOGGER.info("the first page result size is:[{}]",result.size());

        //second page
        final List<GlobalSession> globalSessions2 = redisTransactionStoreManager.findGlobalSessionByPage(2, 2, true);
        List<GlobalSessionVO> result1 = new ArrayList<>();
        convertToGlobalSessionVo(result1,globalSessions2);
        LOGGER.info("the second page result is:[{}]",result1);
        LOGGER.info("the second page result size is:[{}]",result1.size());

        //third page
        final List<GlobalSession> globalSessions3 = redisTransactionStoreManager.findGlobalSessionByPage(3, 2, true);
        List<GlobalSessionVO> result2 = new ArrayList<>();
        convertToGlobalSessionVo(result2,globalSessions3);
        LOGGER.info("the third page result is:[{}]",result2);
        LOGGER.info("the third page result size is:[{}]",result2.size());

        final List<GlobalSession> globalSessions4 = redisTransactionStoreManager.findGlobalSessionByPage(1, 5, true);
        List<GlobalSessionVO> result3 = new ArrayList<>();
        convertToGlobalSessionVo(result3,globalSessions4);
        LOGGER.info("the All page result is:[{}]",result3);
        LOGGER.info("the All page result size is:[{}]",result3.size());

        final List<GlobalSession> globalSessions5 = redisTransactionStoreManager.findGlobalSessionByPage(4, 2, true);
        List<GlobalSessionVO> result4 = new ArrayList<>();
        convertToGlobalSessionVo(result3,globalSessions5);
        LOGGER.info("the four page result is:[{}]",result4);
        LOGGER.info("the four page result size is:[{}]",result4.size());

        // test statusByPage
        GlobalSessionParam param = new GlobalSessionParam();
        // param.setPageNum(1);
        // param.setPageSize(1);
        // param.setStatus(1);
        final List<GlobalSession> globalSessionStatus = redisTransactionStoreManager.readSessionStatusByPage(param);
        LOGGER.info("the  globalSessionStatus result is:[{}]", globalSessionStatus);
        LOGGER.info("the  globalSessionStatus result size is:[{}]", globalSessionStatus);
        sessionManager.removeGlobalSession(session1);
        sessionManager.removeGlobalSession(session2);
        sessionManager.removeGlobalSession(session3);
        sessionManager.removeGlobalSession(session4);
        sessionManager.removeGlobalSession(session5);
        sessionManager.removeGlobalSession(session);
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
        globalLockVO.setGmtCreate(System.currentTimeMillis());
        globalLockVO.setGmtModified(System.currentTimeMillis());
        try (Jedis jedis = JedisPooledFactory.getJedisInstance()) {
            jedis.hmset(GLOBAL_LOCK_KEY,globallockMap);
            jedis.hmset(ROW_LOCK_KEY,BeanUtils.objectToMap(globalLockVO));
        } catch (Exception ex) {
            throw new RedisException(ex);
        }
    }

    @Test
    public void testQueryGlobalslSession() {
        Long count = redisTransactionStoreManager.countByGlobalSessions(GlobalStatus.values());
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
    public static void close(){
        redisTransactionStoreManager=null;
    }

}
