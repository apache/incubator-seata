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
package org.apache.seata.server.console.impl;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.console.entity.param.GlobalLockParam;
import org.apache.seata.server.session.BranchSession;
import org.apache.seata.server.session.GlobalSession;
import org.apache.seata.server.session.SessionHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AbstractLockServiceTest extends BaseSpringBootTest {

    private TestAbstractLockService service;
    private GlobalSession mockGlobalSession;
    private BranchSession mockBranchSession;

    private static class TestAbstractLockService extends AbstractLockService {
        @Override
        public org.apache.seata.common.result.PageResult<org.apache.seata.server.console.entity.vo.GlobalLockVO> query(
                GlobalLockParam param) {
            return null;
        }

        @Override
        public SingleResult<Void> deleteLock(GlobalLockParam param) {
            return null;
        }
    }

    @BeforeEach
    public void setUp() {
        service = new TestAbstractLockService();
        mockGlobalSession = mock(GlobalSession.class);
        mockBranchSession = mock(BranchSession.class);
    }

    @Test
    public void testCheck_WithValidParams() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            when(mockBranchSession.getBranchId()).thenReturn(123L);
            List<BranchSession> branchSessions = new ArrayList<>();
            branchSessions.add(mockBranchSession);
            when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);

            SingleResult<Boolean> result = service.check("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
            Assertions.assertTrue(result.getData());
        }
    }

    @Test
    public void testCheck_WithBlankXid() {
        SingleResult<Boolean> result = service.check("", "123");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(result.getData());
    }

    @Test
    public void testCheck_WithBlankBranchId() {
        SingleResult<Boolean> result = service.check("192.168.1.1:8091:123456", "");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(result.getData());
    }

    @Test
    public void testCheck_WithInvalidBranchId() {
        SingleResult<Boolean> result = service.check("192.168.1.1:8091:123456", "abc");

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(result.getData());
    }

    @Test
    public void testCheck_WithNullSession() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(null);

            SingleResult<Boolean> result = service.check("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
            Assertions.assertFalse(result.getData());
        }
    }

    @Test
    public void testCheck_WithBranchNotFound() {
        try (MockedStatic<SessionHolder> sessionHolderMock = Mockito.mockStatic(SessionHolder.class)) {
            List<BranchSession> branchSessions = new ArrayList<>();
            when(mockGlobalSession.getBranchSessions()).thenReturn(branchSessions);
            sessionHolderMock
                    .when(() -> SessionHolder.findGlobalSession("192.168.1.1:8091:123456"))
                    .thenReturn(mockGlobalSession);

            SingleResult<Boolean> result = service.check("192.168.1.1:8091:123456", "123");

            Assertions.assertTrue(result.isSuccess());
            Assertions.assertFalse(result.getData());
        }
    }

    @Test
    public void testCheckDeleteLock_WithValidParams() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        Assertions.assertDoesNotThrow(() -> service.checkDeleteLock(param));
    }

    @Test
    public void testCheckDeleteLock_WithBlankXid() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("Wrong parameter for xid", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithBlankBranchId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("Wrong parameter for branchId", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithInvalidBranchId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("abc");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("Wrong parameter for branchId, branch Id is not number", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithBlankTableName() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("");
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithNullTableName() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName(null);
        param.setPk("1");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithBlankPk() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("");
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithNullPk() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk(null);
        param.setResourceId("jdbc:mysql://localhost:3306/test");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithBlankResourceId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId("");

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }

    @Test
    public void testCheckDeleteLock_WithNullResourceId() {
        GlobalLockParam param = new GlobalLockParam();
        param.setXid("192.168.1.1:8091:123456");
        param.setBranchId("123");
        param.setTableName("test_table");
        param.setPk("1");
        param.setResourceId(null);

        IllegalArgumentException exception =
                Assertions.assertThrows(IllegalArgumentException.class, () -> service.checkDeleteLock(param));
        Assertions.assertEquals("tableName or resourceId or pk can not be empty", exception.getMessage());
    }
}
