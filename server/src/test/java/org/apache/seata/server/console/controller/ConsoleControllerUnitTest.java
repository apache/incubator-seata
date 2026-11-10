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
package org.apache.seata.server.console.controller;

import org.apache.seata.common.result.*;
import org.apache.seata.server.console.entity.param.*;
import org.apache.seata.server.console.service.*;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConsoleControllerUnitTest {
    @Test
    void globalSessionServiceRoutesCommands() {
        GlobalSessionService service = mock(GlobalSessionService.class);
        GlobalSessionController controller = new GlobalSessionController();
        ReflectionTestUtils.setField(controller, "globalSessionService", service);
        SingleResult<Void> result = SingleResult.success();
        when(service.deleteGlobalSession("xid")).thenReturn(result);
        assertSame(result, controller.deleteGlobalSession("xid"));
        verify(service).deleteGlobalSession("xid");
        when(service.forceDeleteGlobalSession("xid")).thenReturn(result);
        assertSame(result, controller.forceDeleteGlobalSession("xid"));
        verify(service).forceDeleteGlobalSession("xid");
        when(service.stopGlobalRetry("xid")).thenReturn(result);
        assertSame(result, controller.stopGlobalSession("xid"));
        verify(service).stopGlobalRetry("xid");
        when(service.startGlobalRetry("xid")).thenReturn(result);
        assertSame(result, controller.startGlobalSession("xid"));
        verify(service).startGlobalRetry("xid");
        when(service.sendCommitOrRollback("xid")).thenReturn(result);
        assertSame(result, controller.sendCommitOrRollback("xid"));
        verify(service).sendCommitOrRollback("xid");
        when(service.changeGlobalStatus("xid")).thenReturn(result);
        assertSame(result, controller.changeGlobalStatus("xid"));
        verify(service).changeGlobalStatus("xid");
    }

    @Test
    void branchSessionServiceRoutesCommands() {
        BranchSessionService service = mock(BranchSessionService.class);
        BranchSessionController controller = new BranchSessionController();
        ReflectionTestUtils.setField(controller, "branchSessionService", service);
        SingleResult<Void> result = SingleResult.success();
        when(service.deleteBranchSession("xid", "7")).thenReturn(result);
        assertSame(result, controller.deleteBranchSession("xid", "7"));
        verify(service).deleteBranchSession("xid", "7");
        when(service.forceDeleteBranchSession("xid", "7")).thenReturn(result);
        assertSame(result, controller.forceDeleteBranchSession("xid", "7"));
        verify(service).forceDeleteBranchSession("xid", "7");
        when(service.stopBranchRetry("xid", "7")).thenReturn(result);
        assertSame(result, controller.stopBranchSession("xid", "7"));
        verify(service).stopBranchRetry("xid", "7");
        when(service.startBranchRetry("xid", "7")).thenReturn(result);
        assertSame(result, controller.startBranchRetry("xid", "7"));
        verify(service).startBranchRetry("xid", "7");
    }

    @Test
    void globalQueryPropagatesServiceFailure() {
        GlobalSessionService service = mock(GlobalSessionService.class);
        GlobalSessionController controller = new GlobalSessionController();
        ReflectionTestUtils.setField(controller, "globalSessionService", service);
        GlobalSessionParam param = new GlobalSessionParam();
        PageResult result = PageResult.failure("invalid", "bad request");
        when(service.query(param)).thenReturn(result);
        assertSame(result, controller.query(param));
    }

    @Test
    void lockControllerPreservesQueriesAndCommands() {
        GlobalLockService service = mock(GlobalLockService.class);
        GlobalLockController controller = new GlobalLockController();
        ReflectionTestUtils.setField(controller, "globalLockService", service);
        GlobalLockParam param = new GlobalLockParam();
        PageResult page = PageResult.failure("invalid", "bad request");
        when(service.query(param)).thenReturn(page);
        assertSame(page, controller.query(param));
        SingleResult<Void> deleted = SingleResult.success();
        when(service.deleteLock(param)).thenReturn(deleted);
        assertSame(deleted, controller.delete(param));
    }
}
