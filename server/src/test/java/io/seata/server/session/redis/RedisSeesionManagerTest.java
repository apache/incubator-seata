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

import java.sql.SQLException;
import com.alibaba.fastjson.JSON;
import io.seata.common.XID;
import io.seata.common.util.StringUtils;
import io.seata.core.exception.TransactionException;
import io.seata.core.model.BranchStatus;
import io.seata.core.model.BranchType;
import io.seata.core.model.GlobalStatus;
import io.seata.core.store.BranchTransactionDO;
import io.seata.core.store.GlobalTransactionDO;
import io.seata.server.UUIDGenerator;
import io.seata.server.session.BranchSession;
import io.seata.server.session.GlobalSession;
import io.seata.server.store.SessionStorable;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import static org.mockito.Mockito.mock;

/**
 * @author funkye
 */
public class RedisSeesionManagerTest {

    // global transaction prefix
    private static final String DEFAULT_REDIS_SEATA_GLOBAL_PREFIX = "SEATA-GLOBAL-";

    // the prefix of the branchs transaction
    private static final String DEFAULT_REDIS_SEATA_XID_BRANCHS_PREFIX = "SEATA-XID-BRANCHS-";

    // the prefix of the branch transaction
    private static final String DEFAULT_REDIS_SEATA_BRANCH_PREFIX = "SEATA-BRANCH-";

    // global transaction id PREFIX
    private static final String DEFAULT_SEATA_TRANSACTION_ID_GLOBAL_PREFIX = "SEATA-TRANSACTION-ID-GLOBAL-";

    @Test
    public void test_addGlobalSession() {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        GlobalTransactionDO globalTransactionDO = convertGlobalTransactionDO(session);
        try (Jedis jedis = mock(Jedis.class)) {
            String keys = DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + globalTransactionDO.getXid();
            jedis.set(keys, JSON.toJSONString(globalTransactionDO));
            keys = DEFAULT_SEATA_TRANSACTION_ID_GLOBAL_PREFIX + globalTransactionDO.getTransactionId();
            jedis.set(keys, JSON.toJSONString(globalTransactionDO));
        }
    }

    @Test
    public void test_updateGlobalSessionStatus() throws TransactionException, SQLException {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        GlobalTransactionDO globalTransactionDO = convertGlobalTransactionDO(session);
        try (Jedis jedis = mock(Jedis.class)) {
            String keys = DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + globalTransactionDO.getXid();
            jedis.set(keys, JSON.toJSONString(globalTransactionDO));
            keys = DEFAULT_SEATA_TRANSACTION_ID_GLOBAL_PREFIX + globalTransactionDO.getTransactionId();
            jedis.set(keys, JSON.toJSONString(globalTransactionDO));
            session.setStatus(GlobalStatus.Committing);
            globalTransactionDO = convertGlobalTransactionDO(session);
            keys = DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + globalTransactionDO.getXid();
            jedis.set(keys, JSON.toJSONString(globalTransactionDO));
        }
    }

    @Test
    public void test_removeGlobalSession() throws Exception {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        GlobalTransactionDO globalTransactionDO = convertGlobalTransactionDO(session);
        try (Jedis jedis = mock(Jedis.class)) {
            String key = DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + globalTransactionDO.getXid();
            jedis.set(key, JSON.toJSONString(globalTransactionDO));
            jedis.del(key);
        }
    }

    @Test
    public void test_findGlobalSession() throws Exception {
        GlobalSession session = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(session.getTransactionId());
        session.setXid(xid);
        session.setTransactionId(146757978);
        session.setBeginTime(System.currentTimeMillis());
        session.setApplicationData("abc=878s");
        session.setStatus(GlobalStatus.Begin);
        GlobalTransactionDO globalTransactionDO = convertGlobalTransactionDO(session);
        try (Jedis jedis = mock(Jedis.class)) {
            String key = DEFAULT_REDIS_SEATA_GLOBAL_PREFIX + globalTransactionDO.getXid();
            jedis.set(key, JSON.toJSONString(globalTransactionDO));
            jedis.get(key);
        }
    }

    @Test
    public void test_addBranchSession() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        BranchTransactionDO branchTransactionDO = convertBranchTransactionDO(branchSession);
        try (Jedis jedis = mock(Jedis.class)) {
            String key = DEFAULT_REDIS_SEATA_BRANCH_PREFIX + branchTransactionDO.getBranchId();
            if (jedis.get(key) == null) {
                jedis.lpush(DEFAULT_REDIS_SEATA_XID_BRANCHS_PREFIX + branchTransactionDO.getXid(), key);
            }
            jedis.set(key, JSON.toJSONString(branchTransactionDO));
        }
    }

    @Test
    public void test_updateBranchSessionStatus() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setStatus(BranchStatus.PhaseOne_Done);
        BranchTransactionDO branchTransactionDO = convertBranchTransactionDO(branchSession);
        try (Jedis jedis = mock(Jedis.class)) {
            String key = DEFAULT_REDIS_SEATA_BRANCH_PREFIX + branchTransactionDO.getBranchId();
            if (jedis.get(key) == null) {
                jedis.lpush(DEFAULT_REDIS_SEATA_XID_BRANCHS_PREFIX + branchTransactionDO.getXid(), key);
            }
            jedis.set(key, JSON.toJSONString(branchTransactionDO));
            branchSession.setStatus(BranchStatus.PhaseOne_Timeout);
            branchTransactionDO = convertBranchTransactionDO(branchSession);
            jedis.set(key, JSON.toJSONString(branchTransactionDO));
        }
    }

    @Test
    public void test_removeBranchSession() throws Exception {
        GlobalSession globalSession = GlobalSession.createGlobalSession("test", "test", "test123", 100);
        String xid = XID.generateXID(globalSession.getTransactionId());
        globalSession.setXid(xid);
        globalSession.setTransactionId(146757978);
        globalSession.setBeginTime(System.currentTimeMillis());
        globalSession.setApplicationData("abc=878s");
        globalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setBranchId(UUIDGenerator.generateUUID());
        branchSession.setXid(xid);
        branchSession.setTransactionId(globalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("my_test_tx_group");
        branchSession.setResourceId("tb_1");
        branchSession.setLockKey("t_1");
        branchSession.setBranchType(BranchType.AT);
        branchSession.setApplicationData("{\"data\":\"test\"}");
        branchSession.setStatus(BranchStatus.PhaseOne_Done);
        BranchTransactionDO branchTransactionDO = convertBranchTransactionDO(branchSession);
        try (Jedis jedis = mock(Jedis.class)) {
            String key = DEFAULT_REDIS_SEATA_BRANCH_PREFIX + branchTransactionDO.getBranchId();
            jedis.del(key);
        }
    }

    private GlobalTransactionDO convertGlobalTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof GlobalSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        GlobalSession globalSession = (GlobalSession)session;

        GlobalTransactionDO globalTransactionDO = new GlobalTransactionDO();
        globalTransactionDO.setXid(globalSession.getXid());
        globalTransactionDO.setStatus(globalSession.getStatus().getCode());
        globalTransactionDO.setApplicationId(globalSession.getApplicationId());
        globalTransactionDO.setBeginTime(globalSession.getBeginTime());
        globalTransactionDO.setTimeout(globalSession.getTimeout());
        globalTransactionDO.setTransactionId(globalSession.getTransactionId());
        globalTransactionDO.setTransactionName(globalSession.getTransactionName());
        globalTransactionDO.setTransactionServiceGroup(globalSession.getTransactionServiceGroup());
        globalTransactionDO.setApplicationData(globalSession.getApplicationData());
        return globalTransactionDO;
    }

    private BranchTransactionDO convertBranchTransactionDO(SessionStorable session) {
        if (session == null || !(session instanceof BranchSession)) {
            throw new IllegalArgumentException(
                "the parameter of SessionStorable is not available, SessionStorable:" + StringUtils.toString(session));
        }
        BranchSession branchSession = (BranchSession)session;

        BranchTransactionDO branchTransactionDO = new BranchTransactionDO();
        branchTransactionDO.setXid(branchSession.getXid());
        branchTransactionDO.setBranchId(branchSession.getBranchId());
        branchTransactionDO.setBranchType(branchSession.getBranchType().name());
        branchTransactionDO.setClientId(branchSession.getClientId());
        branchTransactionDO.setResourceGroupId(branchSession.getResourceGroupId());
        branchTransactionDO.setTransactionId(branchSession.getTransactionId());
        branchTransactionDO.setApplicationData(branchSession.getApplicationData());
        branchTransactionDO.setResourceId(branchSession.getResourceId());
        branchTransactionDO.setStatus(branchSession.getStatus().getCode());
        return branchTransactionDO;
    }
}
