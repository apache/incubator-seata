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
package org.apache.seata.server.storage;

import org.apache.seata.core.model.BranchStatus;
import org.apache.seata.core.model.BranchType;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.core.store.BranchTransactionDO;
import org.apache.seata.core.store.GlobalTransactionDO;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

/**
 * SessionConverter Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionConverter Test")
class SessionConverterTest {

    @Mock
    private GlobalTransactionDO mockGlobalTransactionDO;

    @Mock
    private BranchTransactionDO mockBranchTransactionDO;

    @Mock
    private GlobalSession mockGlobalSession;

    @Mock
    private BranchSession mockBranchSession;

    @BeforeEach
    void setUp() {
        when(mockGlobalSession.getXid()).thenReturn("test-xid");
        when(mockGlobalSession.getTransactionId()).thenReturn(1L);
        when(mockGlobalSession.getStatus()).thenReturn(GlobalStatus.Committing);
        when(mockGlobalSession.getApplicationId()).thenReturn("appId");
        when(mockGlobalSession.getTransactionServiceGroup()).thenReturn("serviceGroup");
        when(mockGlobalSession.getTransactionName()).thenReturn("txName");
        when(mockGlobalSession.getTimeout()).thenReturn(60000);
        when(mockGlobalSession.getBeginTime()).thenReturn(System.currentTimeMillis());

        when(mockBranchSession.getXid()).thenReturn("test-xid");
        when(mockBranchSession.getTransactionId()).thenReturn(1L);
        when(mockBranchSession.getBranchId()).thenReturn(1L);
        when(mockBranchSession.getBranchType()).thenReturn(BranchType.AT);
        when(mockBranchSession.getStatus()).thenReturn(BranchStatus.Registered);
    }

    @Test
    @DisplayName("test convertGlobalSession with null returns null")
    void testConvertGlobalSessionWithNullReturnsNull() {
        GlobalSession result = SessionConverter.convertGlobalSession((GlobalTransactionDO) null);
        assertNull(result);
    }

    @Test
    @DisplayName("test convertGlobalSession with valid DO")
    void testConvertGlobalSessionWithValidDO() {
        when(mockGlobalTransactionDO.getApplicationId()).thenReturn("app1");
        when(mockGlobalTransactionDO.getTransactionServiceGroup()).thenReturn("group1");
        when(mockGlobalTransactionDO.getTransactionName()).thenReturn("tx1");
        when(mockGlobalTransactionDO.getTimeout()).thenReturn(60000);
        when(mockGlobalTransactionDO.getXid()).thenReturn("xid1");
        when(mockGlobalTransactionDO.getTransactionId()).thenReturn(1L);
        when(mockGlobalTransactionDO.getStatus()).thenReturn(0);

        GlobalSession result = SessionConverter.convertGlobalSession(mockGlobalTransactionDO);

        assertNotNull(result);
        assertEquals("xid1", result.getXid());
        assertEquals(1L, result.getTransactionId());
    }

    @Test
    @DisplayName("test convertBranchSession with null returns null")
    void testConvertBranchSessionWithNullReturnsNull() {
        BranchSession result = SessionConverter.convertBranchSession((BranchTransactionDO) null);
        assertNull(result);
    }

    @Test
    @DisplayName("test convertBranchSession with valid DO")
    void testConvertBranchSessionWithValidDO() {
        when(mockBranchTransactionDO.getXid()).thenReturn("xid1");
        when(mockBranchTransactionDO.getTransactionId()).thenReturn(1L);
        when(mockBranchTransactionDO.getBranchId()).thenReturn(1L);
        when(mockBranchTransactionDO.getBranchType()).thenReturn("AT");
        when(mockBranchTransactionDO.getStatus()).thenReturn(0);

        BranchSession result = SessionConverter.convertBranchSession(mockBranchTransactionDO);

        assertNotNull(result);
        assertEquals("xid1", result.getXid());
        assertEquals(1L, result.getBranchId());
    }

    @Test
    @DisplayName("test convertGlobalTransactionDO creates DO from session")
    void testConvertGlobalTransactionDOCreatesDoFromSession() {
        GlobalTransactionDO result = SessionConverter.convertGlobalTransactionDO(mockGlobalSession);

        assertNotNull(result);
        assertEquals("test-xid", result.getXid());
        assertEquals(1L, result.getTransactionId());
    }

    @Test
    @DisplayName("test convertBranchTransactionDO with valid session")
    void testConvertBranchTransactionDOWithValidSession() {
        BranchTransactionDO result = SessionConverter.convertBranchTransactionDO(mockBranchSession);

        assertNotNull(result);
        assertEquals("test-xid", result.getXid());
        assertEquals(1L, result.getBranchId());
    }

    @Test
    @DisplayName("test convertToBranchSession with empty list returns empty set")
    void testConvertToBranchSessionWithEmptyListReturnsEmptySet() {
        Set<org.apache.seata.server.console.entity.vo.BranchSessionVO> result =
                SessionConverter.convertToBranchSession(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("test convertToBranchSession with valid list")
    void testConvertToBranchSessionWithValidList() {
        List<BranchSession> branches = new ArrayList<>();
        branches.add(mockBranchSession);

        Set<org.apache.seata.server.console.entity.vo.BranchSessionVO> result =
                SessionConverter.convertToBranchSession(branches);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("test convertGlobalSession with empty list returns empty list")
    void testConvertGlobalSessionWithEmptyListReturnsEmptyList() {
        List<org.apache.seata.server.console.entity.vo.GlobalSessionVO> result =
                SessionConverter.convertGlobalSession(Collections.emptyList());

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("test convertGlobalSession with valid list")
    void testConvertGlobalSessionWithValidList() {
        List<GlobalSession> sessions = new ArrayList<>();
        when(mockGlobalSession.getBranchSessions()).thenReturn(Collections.emptyList());
        sessions.add(mockGlobalSession);

        List<org.apache.seata.server.console.entity.vo.GlobalSessionVO> result =
                SessionConverter.convertGlobalSession(sessions);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("test convertGlobalTransactionDO throws exception for invalid session type")
    void testConvertGlobalTransactionDOThrowsExceptionForInvalidSessionType() {
        assertThrows(IllegalArgumentException.class, () -> {
            SessionConverter.convertGlobalTransactionDO(mockBranchSession);
        });
    }

    @Test
    @DisplayName("test convertBranchTransactionDO throws exception for invalid session type")
    void testConvertBranchTransactionDOThrowsExceptionForInvalidSessionType() {
        assertThrows(IllegalArgumentException.class, () -> {
            SessionConverter.convertBranchTransactionDO(mockGlobalSession);
        });
    }
}
