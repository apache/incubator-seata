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
package org.apache.seata.server.console.impl.file;

import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.GlobalSessionParam;
import org.apache.seata.server.console.entity.vo.GlobalSessionVO;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.apache.seata.server.session.SessionManager;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.mockito.Mockito.*;

public class GlobalSessionFileServiceImplTest extends BaseSpringBootTest {

    private GlobalSessionFileServiceImpl service;
    private SessionManager mockSessionManager;
    private GlobalSession mockGlobalSession;

    @BeforeEach
    public void setUp() {
        service = new GlobalSessionFileServiceImpl();
        mockSessionManager = mock(SessionManager.class);
        mockGlobalSession = mock(GlobalSession.class);
    }

    @Test
    public void testQuery_WithInvalidPageSize() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageSize(0);
        param.setPageNum(1);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.query(param));
        Assertions.assertEquals("wrong pageSize or pageNum", exception.getMessage());
    }

    @Test
    public void testQuery_WithInvalidPageNum() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageSize(10);
        param.setPageNum(0);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.query(param));
        Assertions.assertEquals("wrong pageSize or pageNum", exception.getMessage());
    }

    @Test
    public void testQuery_WithNegativePageSize() {
        GlobalSessionParam param = new GlobalSessionParam();
        param.setPageSize(-1);
        param.setPageNum(1);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.query(param));
        Assertions.assertEquals("wrong pageSize or pageNum", exception.getMessage());
    }

    @Test
    public void testQuery_WithValidParams_EmptyResult() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalSessionParam param = new GlobalSessionParam();
            param.setPageSize(10);
            param.setPageNum(1);

            Collection<GlobalSession> emptySessions = new ArrayList<>();
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(emptySessions);

            PageResult<GlobalSessionVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getData().isEmpty());
        }
    }

    @Test
    public void testQuery_WithXidFilter() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalSessionParam param = new GlobalSessionParam();
            param.setPageSize(10);
            param.setPageNum(1);
            param.setXid("192.168.1.1:8091:123456");

            List<GlobalSession> sessions = new ArrayList<>();
            when(mockGlobalSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.getApplicationId()).thenReturn("test-app");
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);
            when(mockGlobalSession.getTransactionName()).thenReturn("test-tx");
            when(mockGlobalSession.getTransactionServiceGroup()).thenReturn("default");
            when(mockGlobalSession.getBeginTime()).thenReturn(System.currentTimeMillis());
            sessions.add(mockGlobalSession);

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(sessions);

            PageResult<GlobalSessionVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.getData().isEmpty());
        }
    }

    @Test
    public void testQuery_WithStatusFilter() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalSessionParam param = new GlobalSessionParam();
            param.setPageSize(10);
            param.setPageNum(1);
            param.setStatus(GlobalStatus.Begin.getCode());

            List<GlobalSession> sessions = new ArrayList<>();
            when(mockGlobalSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.getApplicationId()).thenReturn("test-app");
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);
            when(mockGlobalSession.getTransactionName()).thenReturn("test-tx");
            when(mockGlobalSession.getTransactionServiceGroup()).thenReturn("default");
            when(mockGlobalSession.getBeginTime()).thenReturn(System.currentTimeMillis());
            sessions.add(mockGlobalSession);

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(sessions);

            PageResult<GlobalSessionVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.getData().isEmpty());
        }
    }

    @Test
    public void testQuery_WithApplicationIdFilter() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalSessionParam param = new GlobalSessionParam();
            param.setPageSize(10);
            param.setPageNum(1);
            param.setApplicationId("test-app");

            List<GlobalSession> sessions = new ArrayList<>();
            when(mockGlobalSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockGlobalSession.getApplicationId()).thenReturn("test-app");
            when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Begin);
            when(mockGlobalSession.getTransactionName()).thenReturn("test-tx");
            when(mockGlobalSession.getTransactionServiceGroup()).thenReturn("default");
            when(mockGlobalSession.getBeginTime()).thenReturn(System.currentTimeMillis());
            sessions.add(mockGlobalSession);

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(sessions);

            PageResult<GlobalSessionVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.getData().isEmpty());
        }
    }
}
