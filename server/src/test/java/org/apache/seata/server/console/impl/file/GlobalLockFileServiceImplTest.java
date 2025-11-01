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
import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.GlobalLockParam;
import org.apache.seata.server.console.entity.vo.GlobalLockVO;
import org.apache.seata.server.console.exception.ConsoleException;
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

public class GlobalLockFileServiceImplTest extends BaseSpringBootTest {

    private GlobalLockFileServiceImpl service;
    private SessionManager mockSessionManager;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession;

    @BeforeEach
    public void setUp() {
        service = new GlobalLockFileServiceImpl();
        mockSessionManager = mock(SessionManager.class);
        mockGlobalSession = mock(GlobalSession.class);
        mockBranchSession = mock(BranchSession.class);
    }

    @Test
    public void testQuery_WithInvalidPageSize() {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageSize(0);
        param.setPageNum(1);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.query(param));
        Assertions.assertEquals("wrong pageSize or pageNum", exception.getMessage());
    }

    @Test
    public void testQuery_WithInvalidPageNum() {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageSize(10);
        param.setPageNum(0);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.query(param));
        Assertions.assertEquals("wrong pageSize or pageNum", exception.getMessage());
    }

    @Test
    public void testQuery_WithNegativePageSize() {
        GlobalLockParam param = new GlobalLockParam();
        param.setPageSize(-1);
        param.setPageNum(1);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.query(param));
        Assertions.assertEquals("wrong pageSize or pageNum", exception.getMessage());
    }

    @Test
    public void testQuery_WithInvalidTransactionId_SetsToNull() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalLockParam param = new GlobalLockParam();
            param.setPageSize(10);
            param.setPageNum(1);
            param.setTransactionId("invalid_number");

            Collection<GlobalSession> emptySessions = new ArrayList<>();
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(emptySessions);

            // Should not throw exception, invalid transactionId is set to null
            PageResult<GlobalLockVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getData().isEmpty());
        }
    }

    @Test
    public void testQuery_WithInvalidBranchId_SetsToNull() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalLockParam param = new GlobalLockParam();
            param.setPageSize(10);
            param.setPageNum(1);
            param.setBranchId("invalid_number");

            Collection<GlobalSession> emptySessions = new ArrayList<>();
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(emptySessions);

            // Should not throw exception, invalid branchId is set to null
            PageResult<GlobalLockVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getData().isEmpty());
        }
    }

    @Test
    public void testQuery_WithValidParams_EmptyResult() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalLockParam param = new GlobalLockParam();
            param.setPageSize(10);
            param.setPageNum(1);

            Collection<GlobalSession> emptySessions = new ArrayList<>();
            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.allSessions()).thenReturn(emptySessions);

            PageResult<GlobalLockVO> result = service.query(param);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.getData().isEmpty());
        }
    }

    @Test
    public void testDeleteLock_WithBlankXid() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteLock(param));
        Assertions.assertEquals("Wrong parameter for xid", exception.getMessage());
    }

    @Test
    public void testDeleteLock_WithBlankBranchId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteLock(param));
        Assertions.assertEquals("Wrong parameter for branchId", exception.getMessage());
    }

    @Test
    public void testDeleteLock_WithInvalidBranchId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("abc");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteLock(param));
        Assertions.assertEquals("Wrong parameter for branchId, branch Id is not number", exception.getMessage());
    }

    @Test
    public void testDeleteLock_WithBlankTableName() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testDeleteLock_WithBlankResourceId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testDeleteLock_WithBlankPk() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.deleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testDeleteLock_WithNullGlobalSession() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalLockParam param = new GlobalLockParam();
            param.setXid("192.168.1.1:8091:123456");
            param.setBranchId("123");
            param.setTableName("test_table");
            param.setPk("1");
            param.setResourceId("jdbc:mysql://localhost:3306/test");

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.findGlobalSession("192.168.1.1:8091:123456", true))
                    .thenReturn(null);

            // Should return success even if session not found
            SingleResult<Void> result = service.deleteLock(param);

            Assertions.assertNotNull(result);
            Assertions.assertTrue(result.isSuccess());
        }
    }

    @Test
    public void testDeleteLock_WithMultipleBranchSessions() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            GlobalLockParam param = new GlobalLockParam();
            param.setXid("192.168.1.1:8091:123456");
            param.setBranchId("123");
            param.setTableName("test_table");
            param.setPk("1");
            param.setResourceId("jdbc:mysql://localhost:3306/test");

            BranchSession mockBranchSession2 = mock(BranchSession.class);
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            when(mockBranchSession2.getBranchId()).thenReturn(123L);

            List<BranchSession> branchSessions = new ArrayList<>();
            branchSessions.add(mockBranchSession);
            branchSessions.add(mockBranchSession2); // Add second branch session with same ID

            when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);

            sessionHolderMock.when(SessionHolder::getRootSessionManager).thenReturn(mockSessionManager);
            when(mockSessionManager.findGlobalSession("192.168.1.1:8091:123456", true))
                    .thenReturn(mockGlobalSession);

            // Should throw ConsoleException when branch session size != 1
            Assertions.assertThrows(ConsoleException.class, () -> service.deleteLock(param));
        }
    }
}
