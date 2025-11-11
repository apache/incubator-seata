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
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.vo.BranchSessionVO;
import org.apache.seata.server.session.BranchSession;
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

public class BranchSessionFileServiceImplTest extends BaseSpringBootTest {

    private BranchSessionFileServiceImpl service;
    private SessionManager mockSessionManager;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession;

    @BeforeEach
    public void setUp() {
        service = new BranchSessionFileServiceImpl();
        mockSessionManager = mock(SessionManager.class);
        mockGlobalSession = mock(GlobalSession.class);
        mockBranchSession = mock(BranchSession.class);
    }

    @Test
    public void testQueryByXid_WithBlankXid() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.queryByXid(""));
        Assertions.assertEquals("xid should not be blank", exception.getMessage());
    }

    @Test
    public void testQueryByXid_WithNullXid() {
        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.queryByXid(null));
        Assertions.assertEquals("xid should not be blank", exception.getMessage());
    }

    @Test
    public void testQueryByXid_WithValidXid_NotFound() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            Collection<GlobalSession> emptySessions = new ArrayList<>();
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(emptySessions);

            PageResult<BranchSessionVO> result = service.queryByXid("192.168.1.1:8091:123456");

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getData().isEmpty());
            Assertions.assertEquals(0, result.getTotal());
        }
    }

    @Test
    public void testQueryByXid_WithValidXid_Found() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            List<GlobalSession> sessions = new ArrayList<>();
            when(mockGlobalSession.getXid()).thenReturn("192.168.1.1:8091:123456");

            // Mock BranchSession with required fields
            when(mockBranchSession.getBranchType()).thenReturn(org.apache.seata.core.model.BranchType.AT);
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            when(mockBranchSession.getXid()).thenReturn("192.168.1.1:8091:123456");
            when(mockBranchSession.getTransactionId()).thenReturn(123456L);
            when(mockBranchSession.getResourceId()).thenReturn("jdbc:mysql://localhost:3306/test");
            when(mockBranchSession.getApplicationData()).thenReturn("{}");
            when(mockBranchSession.getStatus()).thenReturn(org.apache.seata.core.model.BranchStatus.Registered);

            List<BranchSession> branchSessions = new ArrayList<>();
            branchSessions.add(mockBranchSession);
            when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);
            sessions.add(mockGlobalSession);

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(sessions);

            PageResult<BranchSessionVO> result = service.queryByXid("192.168.1.1:8091:123456");

            Assertions.assertNotNull(result);
            Assertions.assertFalse(result.getData().isEmpty());
        }
    }

    @Test
    public void testQueryByXid_WithDifferentXid_NotFound() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            List<GlobalSession> sessions = new ArrayList<>();
            when(mockGlobalSession.getXid()).thenReturn("192.168.1.1:8091:999999");
            sessions.add(mockGlobalSession);

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(sessions);

            PageResult<BranchSessionVO> result = service.queryByXid("192.168.1.1:8091:123456");

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getData().isEmpty());
        }
    }
}
