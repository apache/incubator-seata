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

import com.github.fppt.jedismock.RedisServer;
import io.seata.common.XID;
import io.seata.core.console.param.GlobalSessionParam;
import io.seata.core.console.result.PageResult;
import io.seata.core.console.vo.GlobalSessionVO;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.server.UUIDGenerator;
import io.seata.server.console.service.GlobalSessionService;
import io.seata.server.session.BranchSession;
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
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import javax.annotation.Resource;
import java.io.IOException;
import java.util.List;

import static io.seata.common.DefaultValues.DEFAULT_TX_GROUP;

/**
 * @author funkye
 */
@SpringBootTest
public class GlobalSessionRedisQueryTest {
    private static RedisServer server = null;
    private static SessionManager sessionManager = null;

    @Resource
    private GlobalSessionService redisGlobalSessionService;

    @BeforeAll
    public static void start(ApplicationContext context) throws IOException {
        server = RedisServer.newRedisServer(6789);
        server.start();
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMinIdle(1);
        poolConfig.setMaxIdle(10);
        JedisPooledFactory.getJedisPoolInstance(new JedisPool(poolConfig, "47.105.165.195", 6879, 60000,"ren1004940725@"));
        RedisTransactionStoreManager transactionStoreManager = RedisTransactionStoreManager.getInstance();
        RedisSessionManager redisSessionManager = new RedisSessionManager();
        redisSessionManager.setTransactionStoreManager(transactionStoreManager);
        sessionManager = redisSessionManager;
    }

    @Test
    public void testQueryGlobslSession(){
        GlobalSessionParam param = new GlobalSessionParam();
        param.setXid("1231421312241");
        PageResult<GlobalSessionVO> query = redisGlobalSessionService.query(param);
    }



    @AfterAll
    public static void after() {
        server.stop();
        server = null;
    }

}
