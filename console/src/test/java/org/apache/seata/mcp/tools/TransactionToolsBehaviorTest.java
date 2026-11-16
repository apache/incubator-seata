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
package org.apache.seata.mcp.tools;

import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.mcp.core.props.MCPProperties;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.core.utils.DateUtils;
import org.apache.seata.mcp.entity.dto.McpGlobalLockParamDto;
import org.apache.seata.mcp.entity.dto.McpGlobalSessionParamDto;
import org.apache.seata.mcp.entity.param.McpGlobalAbnormalSessionParam;
import org.apache.seata.mcp.entity.param.McpGlobalLockDeleteParam;
import org.apache.seata.mcp.entity.param.McpGlobalLockParam;
import org.apache.seata.mcp.entity.param.McpGlobalSessionParam;
import org.apache.seata.mcp.service.ConsoleApiService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.endsWith;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class TransactionToolsBehaviorTest {
    private final ConsoleApiService remote = mock(ConsoleApiService.class);
    private final ModifyConfirmService confirmation = mock(ModifyConfirmService.class);
    private final MCPProperties properties = mock(MCPProperties.class);
    private final NameSpaceDetail namespace = new NameSpaceDetail();
    private final ObjectMapper mapper = new ObjectMapper();
    private final GlobalSessionTools sessions = new GlobalSessionTools(remote, properties, mapper, confirmation);
    private final GlobalLockTools locks = new GlobalLockTools(remote, properties, confirmation, mapper);
    private final BranchSessionTools branches = new BranchSessionTools(remote, confirmation);

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void queryCompletesMissingTimeBoundAndCopiesFilters(boolean start, boolean end) {
        when(properties.getQueryDuration()).thenReturn(3600000L);
        McpGlobalSessionParamDto dto = sessionDto();
        dto.setXid("xid");
        dto.setApplicationId("app");
        dto.setTransactionName("order");
        dto.setStatus(1);
        dto.setWithBranch(true);
        if (start) {
            dto.setTimeStart("2026-01-01 10:00:00");
        }
        if (end) {
            dto.setTimeEnd("2026-01-01 11:00:00");
        }
        when(remote.getCallTC(eq(namespace), anyString(), any(), isNull(), isNull()))
                .thenReturn("{\"data\":[],\"total\":0}");
        assertNotNull(sessions.queryGlobalSession(namespace, dto).getData());
        ArgumentCaptor<McpGlobalSessionParam> captor = ArgumentCaptor.forClass(McpGlobalSessionParam.class);
        verify(remote).getCallTC(eq(namespace), endsWith("/query"), captor.capture(), isNull(), isNull());
        McpGlobalSessionParam actual = captor.getValue();
        assertEquals("xid", actual.getXid());
        assertEquals("app", actual.getApplicationId());
        assertEquals("order", actual.getTransactionName());
        assertEquals(Integer.valueOf(1), actual.getStatus());
        assertTrue(actual.isWithBranch());
        assertEquals(1, actual.getPageNum());
        assertEquals(10, actual.getPageSize());
        if (start || end) {
            assertEquals(DateUtils.convertToTimeStampFromDateTime("2026-01-01 10:00:00"), actual.getTimeStart());
            assertEquals(3600000L, actual.getTimeEnd() - actual.getTimeStart());
        } else {
            assertNull(actual.getTimeStart());
            assertNull(actual.getTimeEnd());
        }
    }

    @ParameterizedTest
    @CsvSource({"false,false", "true,false", "false,true", "true,true"})
    void lockQueryCopiesFiltersAndCompletesTimeWindow(boolean start, boolean end) {
        when(properties.getQueryDuration()).thenReturn(3600000L);
        McpGlobalLockParamDto dto = lockDto();
        dto.setXid("xid");
        dto.setTransactionId("123");
        dto.setBranchId("456");
        dto.setTableName("orders");
        dto.setPk("42");
        dto.setResourceId("jdbc:test");
        if (start) {
            dto.setTimeStart("2026-01-01 10:00:00");
        }
        if (end) {
            dto.setTimeEnd("2026-01-01 11:00:00");
        }
        when(remote.getCallTC(eq(namespace), anyString(), any(), isNull(), isNull()))
                .thenReturn("{\"data\":[],\"total\":0}");
        assertNotNull(locks.queryGlobalLock(namespace, dto).getData());
        ArgumentCaptor<McpGlobalLockParam> captor = ArgumentCaptor.forClass(McpGlobalLockParam.class);
        verify(remote).getCallTC(eq(namespace), endsWith("/query"), captor.capture(), isNull(), isNull());
        McpGlobalLockParam actual = captor.getValue();
        assertEquals("xid", actual.getXid());
        assertEquals("123", actual.getTransactionId());
        assertEquals("456", actual.getBranchId());
        assertEquals("orders", actual.getTableName());
        assertEquals("42", actual.getPk());
        assertEquals("jdbc:test", actual.getResourceId());
        assertEquals(1, actual.getPageNum());
        assertEquals(10, actual.getPageSize());
        if (start || end) {
            assertEquals(3600000L, actual.getTimeEnd() - actual.getTimeStart());
        } else {
            assertNull(actual.getTimeStart());
            assertNull(actual.getTimeEnd());
        }
    }

    @Test
    void excessiveTimeWindowsNeverReachRemoteService() {
        when(properties.getQueryDuration()).thenReturn(3600000L);
        McpGlobalSessionParamDto session = sessionDto();
        session.setTimeStart("2026-01-01 00:00:00");
        session.setTimeEnd("2026-01-02 00:00:00");
        McpGlobalLockParamDto lock = lockDto();
        lock.setTimeStart(session.getTimeStart());
        lock.setTimeEnd(session.getTimeEnd());
        assertNull(sessions.queryGlobalSession(namespace, session).getData());
        assertNull(locks.queryGlobalLock(namespace, lock).getData());
        verifyNoInteractions(remote);
    }

    @ParameterizedTest
    @ValueSource(strings = {"null", "not-json"})
    void invalidRemoteJsonReturnsFailedPage(String response) {
        when(remote.getCallTC(eq(namespace), anyString(), any(), isNull(), isNull()))
                .thenReturn(response);
        assertNull(sessions.queryGlobalSession(namespace, sessionDto()).getData());
        assertNull(locks.queryGlobalLock(namespace, lockDto()).getData());
    }

    @Test
    void abnormalSessionsMergeOnlyNonemptyResults() {
        when(properties.getQueryDuration()).thenReturn(3600000L);
        McpGlobalAbnormalSessionParam param = new McpGlobalAbnormalSessionParam();
        param.setPageNum(2);
        param.setWithBranch(false);
        param.setTimeStart("2026-01-01 10:00:00");
        param.setTimeEnd("2026-01-01 11:00:00");
        when(remote.getCallTC(eq(namespace), anyString(), any(), isNull(), isNull()))
                .thenAnswer(invocation -> {
                    McpGlobalSessionParam query = invocation.getArgument(2);
                    assertEquals(30, query.getPageSize());
                    assertEquals(2, query.getPageNum());
                    assertFalse(query.isWithBranch());
                    return query.getStatus() == GlobalStatus.CommitFailed.getCode()
                            ? "{\"data\":[{\"xid\":\"failed\"}]}"
                            : "{\"data\":null}";
                });
        assertEquals(1, sessions.getAbnormalSessions(namespace, param).size());
        verify(remote, times(3)).getCallTC(eq(namespace), anyString(), any(), isNull(), isNull());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "deleteGlobalSession",
                "stopGlobalSession",
                "startGlobalSession",
                "sendCommitOrRollback",
                "changeGlobalStatus"
            })
    void globalMutationsRequireKeyAndForwardXid(String method) {
        Supplier<String> action =
                switch (method) {
                    case "deleteGlobalSession" -> () -> sessions.deleteGlobalSession(namespace, "xid", "key");
                    case "stopGlobalSession" -> () -> sessions.stopGlobalSession(namespace, "xid", "key");
                    case "startGlobalSession" -> () -> sessions.startGlobalSession(namespace, "xid", "key");
                    case "sendCommitOrRollback" -> () -> sessions.sendCommitOrRollback(namespace, "xid", "key");
                    case "changeGlobalStatus" -> () -> sessions.changeGlobalStatus(namespace, "xid", "key");
                    default -> throw new IllegalArgumentException(method);
                };
        assertEquals("The modify key is not available", action.get());
        verifyNoInteractions(remote);
        when(confirmation.isValidKey("key")).thenReturn(true);
        when(remote.deleteCallTC(eq(namespace), anyString(), isNull(), eq(Map.of("xid", "xid")), isNull()))
                .thenReturn("ok", "");
        when(remote.putCallTC(eq(namespace), anyString(), isNull(), eq(Map.of("xid", "xid")), isNull()))
                .thenReturn("ok", "");
        assertEquals("ok", action.get());
        assertTrue(action.get().contains("failed, xid: xid"));
        if ("deleteGlobalSession".equals(method)) {
            verify(remote, times(2))
                    .deleteCallTC(eq(namespace), endsWith("/" + method), isNull(), eq(Map.of("xid", "xid")), isNull());
        } else {
            verify(remote, times(2))
                    .putCallTC(eq(namespace), endsWith("/" + method), isNull(), eq(Map.of("xid", "xid")), isNull());
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"deleteBranchSession", "stopBranchSession", "startBranchRetry"})
    void branchMutationsRequireKeyAndForwardBothIdentifiers(String method) {
        Supplier<String> action =
                switch (method) {
                    case "deleteBranchSession" -> () -> branches.deleteBranchSession(namespace, "xid", "branch", "key");
                    case "stopBranchSession" -> () -> branches.stopBranchSession(namespace, "xid", "branch", "key");
                    case "startBranchRetry" -> () -> branches.startBranchRetry(namespace, "xid", "branch", "key");
                    default -> throw new IllegalArgumentException(method);
                };
        assertEquals("The modify key is not available", action.get());
        verifyNoInteractions(remote);
        when(confirmation.isValidKey("key")).thenReturn(true);
        Map<String, String> identifiers = Map.of("xid", "xid", "branchId", "branch");
        when(remote.deleteCallTC(eq(namespace), anyString(), isNull(), eq(identifiers), isNull()))
                .thenReturn("ok", "");
        when(remote.putCallTC(eq(namespace), anyString(), isNull(), eq(identifiers), isNull()))
                .thenReturn("ok", "");
        assertEquals("ok", action.get());
        assertTrue(action.get().contains("failed, xid: xid, branchId: branch"));
        String endpoint = "startBranchRetry".equals(method) ? "startBranchSession" : method;
        if ("deleteBranchSession".equals(method)) {
            verify(remote, times(2))
                    .deleteCallTC(eq(namespace), endsWith("/" + endpoint), isNull(), eq(identifiers), isNull());
        } else {
            verify(remote, times(2))
                    .putCallTC(eq(namespace), endsWith("/" + endpoint), isNull(), eq(identifiers), isNull());
        }
    }

    @Test
    void lockDeleteRequiresKeyAndReportsEmptyResponses() {
        McpGlobalLockDeleteParam param = new McpGlobalLockDeleteParam();
        param.setXid("xid");
        param.setBranchId("branch");
        param.setPk("1");
        param.setResourceId("jdbc:test");
        param.setTableName("orders");
        assertEquals("1", param.getPk());
        assertEquals("jdbc:test", param.getResourceId());
        assertEquals("orders", param.getTableName());
        assertEquals("The modify key is not available", locks.deleteGlobalLock(namespace, param, "key"));
        verifyNoInteractions(remote);
        when(confirmation.isValidKey("key")).thenReturn(true);
        when(remote.deleteCallTC(eq(namespace), endsWith("/delete"), same(param), isNull(), isNull()))
                .thenReturn("ok", "");
        assertEquals("ok", locks.deleteGlobalLock(namespace, param, "key"));
        assertEquals(
                "delete global lock failed, xid: xid, branchId: branch",
                locks.deleteGlobalLock(namespace, param, "key"));
        when(remote.getCallTC(
                        eq(namespace),
                        endsWith("/check"),
                        isNull(),
                        eq(Map.of("xid", "xid", "branchId", "branch")),
                        isNull()))
                .thenReturn("locked", "");
        assertEquals("locked", locks.checkGlobalLock(namespace, "xid", "branch"));
        assertEquals(
                "check global lock failed, xid: xid, branchId: branch",
                locks.checkGlobalLock(namespace, "xid", "branch"));
    }

    private McpGlobalSessionParamDto sessionDto() {
        McpGlobalSessionParamDto dto = new McpGlobalSessionParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        return dto;
    }

    private McpGlobalLockParamDto lockDto() {
        McpGlobalLockParamDto dto = new McpGlobalLockParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        return dto;
    }
}
