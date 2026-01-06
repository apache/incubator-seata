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
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.rpc.RpcContext;
import org.apache.seata.server.BaseSpringBootTest;
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

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class DefaultTMDisconnectHandlerTest extends BaseSpringBootTest {

    @Mock
    private Core mockCore;

    @Mock
    private Configuration mockConfig;

    @Mock
    private SessionManager mockSessionManager;

    @Mock
    private GlobalSession mockGlobalSession1;

    @Mock
    private GlobalSession mockGlobalSession2;

    @Mock
    private RpcContext mockRpcContext;

    private DefaultTMDisconnectHandler tmDisconnectHandler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        tmDisconnectHandler = new DefaultTMDisconnectHandler(mockCore);
    }

    @Test
    void testHandleTMDisconnect_WhenDisabled_ShouldNotRollback() throws Exception {
        when(mockRpcContext.getTransactionServiceGroup()).thenReturn("test-group");

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class)) {
            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(false);

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            verifyNoInteractions(mockCore);
        }
    }

    @Test
    void testHandleTMDisconnect_WhenEnabled_ShouldRollbackMatchingSessions() throws Exception {
        String testServiceGroup = "test-service-group";
        String testApplicationId = "test-app";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn(testApplicationId);

        when(mockGlobalSession1.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession1.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession1.getApplicationId()).thenReturn(testApplicationId);
        when(mockGlobalSession1.getXid()).thenReturn("test-xid-1");

        when(mockGlobalSession2.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession2.getTransactionServiceGroup()).thenReturn("other-service-group");
        when(mockGlobalSession2.getApplicationId()).thenReturn(testApplicationId);
        when(mockGlobalSession2.getXid()).thenReturn("test-xid-2");

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(Arrays.asList(mockGlobalSession1, mockGlobalSession2));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            verify(mockGlobalSession1).changeGlobalStatus(GlobalStatus.TimeoutRollbacking);
            verify(mockCore).doGlobalRollback(mockGlobalSession1, false);

            verify(mockGlobalSession2, never()).changeGlobalStatus(any());
            verify(mockCore, never()).doGlobalRollback(eq(mockGlobalSession2), anyBoolean());
        }
    }

    @Test
    void testHandleTMDisconnect_WithNullRpcContext_ShouldNotProcess() {
        tmDisconnectHandler.handleTMDisconnect(null);

        verifyNoInteractions(mockCore);
    }

    @Test
    void testHandleTMDisconnect_WithNullServiceGroup_ShouldNotProcess() {
        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(null);

        tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

        verifyNoInteractions(mockCore);
    }

    @Test
    void testHandleTMDisconnect_ShouldOnlyRollbackBeginStatus() throws Exception {
        String testServiceGroup = "test-service-group";
        String testApplicationId = "test-app";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn(testApplicationId);

        when(mockGlobalSession1.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession1.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession1.getApplicationId()).thenReturn(testApplicationId);
        when(mockGlobalSession1.getXid()).thenReturn("test-xid-1");

        when(mockGlobalSession2.getStatus()).thenReturn(GlobalStatus.Committed);
        when(mockGlobalSession2.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession2.getApplicationId()).thenReturn(testApplicationId);
        when(mockGlobalSession2.getXid()).thenReturn("test-xid-2");

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(Arrays.asList(mockGlobalSession1, mockGlobalSession2));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            verify(mockGlobalSession1).changeGlobalStatus(GlobalStatus.TimeoutRollbacking);
            verify(mockCore).doGlobalRollback(mockGlobalSession1, false);

            verify(mockGlobalSession2, never()).changeGlobalStatus(any());
            verify(mockCore, never()).doGlobalRollback(eq(mockGlobalSession2), anyBoolean());
        }
    }

    @Test
    void testHandleTMDisconnect_ContinuesOnRollbackFailure() throws Exception {
        String testServiceGroup = "test-service-group";
        String testApplicationId = "test-app";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn(testApplicationId);

        when(mockGlobalSession1.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession1.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession1.getApplicationId()).thenReturn(testApplicationId);
        when(mockGlobalSession1.getXid()).thenReturn("test-xid-1");

        when(mockGlobalSession2.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession2.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession2.getApplicationId()).thenReturn(testApplicationId);
        when(mockGlobalSession2.getXid()).thenReturn("test-xid-2");

        doThrow(new TransactionException("Rollback failed")).when(mockCore).doGlobalRollback(mockGlobalSession1, false);

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(Arrays.asList(mockGlobalSession1, mockGlobalSession2));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            verify(mockGlobalSession1).changeGlobalStatus(GlobalStatus.TimeoutRollbacking);
            verify(mockCore).doGlobalRollback(mockGlobalSession1, false);

            verify(mockGlobalSession2).changeGlobalStatus(GlobalStatus.TimeoutRollbacking);
            verify(mockCore).doGlobalRollback(mockGlobalSession2, false);
        }
    }

    @Test
    void testHandleTMDisconnect_WithoutApplicationId_ShouldUseVgroupOnly() throws Exception {
        String testServiceGroup = "test-service-group";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn(null);

        when(mockGlobalSession1.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession1.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession1.getApplicationId()).thenReturn(null);
        when(mockGlobalSession1.getXid()).thenReturn("test-xid-1");

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(Collections.singletonList(mockGlobalSession1));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            verify(mockGlobalSession1).changeGlobalStatus(GlobalStatus.TimeoutRollbacking);
            verify(mockCore).doGlobalRollback(mockGlobalSession1, false);
        }
    }

    @Test
    void testHandleTMDisconnect_WithMismatchedApplicationId_ShouldNotRollback() throws Exception {
        String testServiceGroup = "test-service-group";

        when(mockRpcContext.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockRpcContext.getApplicationId()).thenReturn("app-1");

        when(mockGlobalSession1.getStatus()).thenReturn(GlobalStatus.Begin);
        when(mockGlobalSession1.getTransactionServiceGroup()).thenReturn(testServiceGroup);
        when(mockGlobalSession1.getApplicationId()).thenReturn("app-2");
        when(mockGlobalSession1.getXid()).thenReturn("test-xid-1");

        try (MockedStatic<ConfigurationFactory> configFactory = mockStatic(ConfigurationFactory.class);
                MockedStatic<SessionHolder> sessionHolder = mockStatic(SessionHolder.class)) {

            configFactory.when(ConfigurationFactory::getInstance).thenReturn(mockConfig);
            when(mockConfig.getBoolean(
                            ConfigurationKeys.ENABLE_ROLLBACK_WHEN_DISCONNECT,
                            DefaultValues.DEFAULT_ENABLE_ROLLBACK_WHEN_DISCONNECT))
                    .thenReturn(true);

            sessionHolder.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(Collections.singletonList(mockGlobalSession1));

            tmDisconnectHandler.handleTMDisconnect(mockRpcContext);

            verify(mockGlobalSession1, never()).changeGlobalStatus(any());
            verify(mockCore, never()).doGlobalRollback(any(), anyBoolean());
        }
    }
}
