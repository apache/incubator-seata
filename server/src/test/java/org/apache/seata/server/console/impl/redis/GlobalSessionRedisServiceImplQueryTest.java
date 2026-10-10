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

import org.apache.seata.common.holder.ObjectHolder;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.console.entity.param.GlobalSessionParam;
import org.apache.seata.server.console.entity.vo.GlobalSessionVO;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionCondition;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManager;
import org.apache.seata.server.storage.redis.store.RedisTransactionStoreManagerFactory;
import org.apache.seata.spring.boot.autoconfigure.SeataCoreEnvironmentPostProcessor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

import static org.apache.seata.common.Constants.OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Status filtering for an XID query must not fall back to the unfiltered session.
 */
class GlobalSessionRedisServiceImplQueryTest {

    @BeforeAll
    static void initConfiguration() {
        ObjectHolder.INSTANCE.setObject(OBJECT_KEY_SPRING_CONFIGURABLE_ENVIRONMENT, new MockEnvironment());
        SeataCoreEnvironmentPostProcessor.init();
    }

    @Test
    void queryByXidAndNonMatchingStatusReturnsEmpty() throws Exception {
        PageResult<GlobalSessionVO> result = query(GlobalStatus.RollbackRetrying.getCode());

        assertTrue(result.isSuccess());
        assertEquals(0, result.getTotal());
        assertEquals(0, result.getData().size());
    }

    @Test
    void queryByXidAndMatchingStatusKeepsTheSession() throws Exception {
        PageResult<GlobalSessionVO> result = query(GlobalStatus.Begin.getCode());

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getData().size());
        assertEquals("192.168.1.1:8091:1001", result.getData().get(0).getXid());
    }

    @Test
    void queryByXidWithoutStatusKeepsTheSession() throws Exception {
        PageResult<GlobalSessionVO> result = query(null);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getData().size());
    }

    @Test
    void queryByXidAndUnrecognizedStatusKeepsTheSession() throws Exception {
        PageResult<GlobalSessionVO> result = query(999);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getTotal());
        assertEquals(1, result.getData().size());
    }

    @Test
    void queryByMissingXidAndStatusReturnsEmpty() throws Exception {
        GlobalSession other = new GlobalSession();
        other.setXid("192.168.1.1:8091:9999");
        other.setStatus(GlobalStatus.Begin);
        Field branchSessions = GlobalSession.class.getDeclaredField("branchSessions");
        branchSessions.setAccessible(true);
        branchSessions.set(other, new ArrayList<>());

        RedisTransactionStoreManager store = mock(RedisTransactionStoreManager.class);
        when(store.readSession(any(SessionCondition.class))).thenReturn(Collections.emptyList());
        when(store.readSessionStatusByPage(any(GlobalSessionParam.class))).thenReturn(Collections.singletonList(other));
        when(store.countByGlobalSessions(any(GlobalStatus[].class))).thenReturn(1L);

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(20);
        param.setXid("192.168.1.1:8091:404");
        param.setStatus(GlobalStatus.Begin.getCode());
        param.setWithBranch(false);

        try (MockedStatic<RedisTransactionStoreManagerFactory> factory =
                mockStatic(RedisTransactionStoreManagerFactory.class)) {
            factory.when(RedisTransactionStoreManagerFactory::getInstance).thenReturn(store);
            PageResult<GlobalSessionVO> result = new GlobalSessionRedisServiceImpl().query(param);

            assertTrue(result.isSuccess());
            assertEquals(0, result.getTotal());
            assertEquals(0, result.getData().size());
            verify(store, never()).readSessionStatusByPage(any(GlobalSessionParam.class));
            verify(store, never()).countByGlobalSessions(any(GlobalStatus[].class));
        }
    }

    private PageResult<GlobalSessionVO> query(Integer status) throws Exception {
        GlobalSession session = new GlobalSession();
        session.setXid("192.168.1.1:8091:1001");
        session.setStatus(GlobalStatus.Begin);
        Field branchSessions = GlobalSession.class.getDeclaredField("branchSessions");
        branchSessions.setAccessible(true);
        branchSessions.set(session, new ArrayList<>());

        RedisTransactionStoreManager store = mock(RedisTransactionStoreManager.class);
        when(store.readSession(any(SessionCondition.class))).thenReturn(Collections.singletonList(session));

        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageNum(1);
        param.setPageSize(20);
        param.setXid(session.getXid());
        param.setStatus(status);
        param.setWithBranch(false);

        try (MockedStatic<RedisTransactionStoreManagerFactory> factory =
                mockStatic(RedisTransactionStoreManagerFactory.class)) {
            factory.when(RedisTransactionStoreManagerFactory::getInstance).thenReturn(store);
            return new GlobalSessionRedisServiceImpl().query(param);
        }
    }
}
