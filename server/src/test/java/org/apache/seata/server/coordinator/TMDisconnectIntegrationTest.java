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
package org.apache.seata.server.coordinator;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TMDisconnectIntegrationTest extends BaseSpringBootTest {

    @Mock
    private Core mockCore;

    @Mock
    private Configuration mockConfig;

    @Mock
    private SessionManager mockSessionManager;

    @Mock
    private RpcContext mockRpcContext;

    private DefaultTMDisconnectHandler tmDisconnectHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tmDisconnectHandler = new DefaultTMDisconnectHandler(mockCore);
    }

    @Test
    void testIntegrationWithRealGlobalSession() throws Exception {
        String testServiceGroup = "test-service-group";
        String testApplicationId = "test-app";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn(testApplicationId);

        GlobalSession realGlobalSession = new GlobalSession(testApplicationId, testServiceGroup, "test-tx", 30000);
        realGlobalSession.setStatus(GlobalStatus.Begin);

        BranchSession branchSession = new BranchSession();
        branchSession.setTransactionId(realGlobalSession.getTransactionId());
        branchSession.setBranchId(1L);
        branchSession.setResourceGroupId("test-resource");
        branchSession.setResourceId("jdbc:mysql://localhost:3306/test");
        branchSession.setBranchType(BranchType.AT);
        realGlobalSession.addBranch(branchSession);

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(Collections.singletonList(realGlobalSession));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            assertEquals(GlobalStatus.TimeoutRollbacking, realGlobalSession.getStatus());
            verify(mockCore).doGlobalRollback(realGlobalSession, false);
        }
    }

    @Test
    void testMixedGlobalStatusSessions() throws Exception {
        String testServiceGroup = "test-service-group";
        String testApplicationId = "test-app";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn(testApplicationId);

        GlobalSession beginSession = new GlobalSession(testApplicationId, testServiceGroup, "tx1", 30000);
        beginSession.setStatus(GlobalStatus.Begin);

        GlobalSession committedSession = new GlobalSession(testApplicationId, testServiceGroup, "tx2", 30000);
        committedSession.setStatus(GlobalStatus.Committed);

        GlobalSession rollbackingSession = new GlobalSession(testApplicationId, testServiceGroup, "tx3", 30000);
        rollbackingSession.setStatus(GlobalStatus.Rollbacking);

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions())
                    .thenReturn(Arrays.asList(beginSession, committedSession, rollbackingSession));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            assertEquals(GlobalStatus.TimeoutRollbacking, beginSession.getStatus());
            assertEquals(GlobalStatus.Committed, committedSession.getStatus());
            assertEquals(GlobalStatus.Rollbacking, rollbackingSession.getStatus());

            verify(mockCore, times(1)).doGlobalRollback(beginSession, false);
            verify(mockCore, never()).doGlobalRollback(eq(committedSession), anyBoolean());
            verify(mockCore, never()).doGlobalRollback(eq(rollbackingSession), anyBoolean());
        }
    }
}
