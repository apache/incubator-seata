package org.apache.seata.mcp.controller.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.dto.GlobalLockParamDto;
import org.apache.seata.mcp.entity.param.GlobalLockDeleteParam;
import org.apache.seata.mcp.entity.param.GlobalLockParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.GlobalLockVO;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.apache.seata.mcp.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalLockToolsTest {

    private GlobalLockTools tools;
    private MCPRPCService rpcService;
    private MCPProperties config;
    private ModifyConfirmService confirmService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() throws Exception {
        tools = new GlobalLockTools();
        rpcService = mock(MCPRPCService.class);
        config = new MCPProperties();
        confirmService = mock(ModifyConfirmService.class);
        objectMapper = new ObjectMapper();

        java.lang.reflect.Field f1 = GlobalLockTools.class.getDeclaredField("mcpRPCService");
        f1.setAccessible(true);
        f1.set(tools, rpcService);

        java.lang.reflect.Field f2 = GlobalLockTools.class.getDeclaredField("configuration");
        f2.setAccessible(true);
        f2.set(tools, config);

        java.lang.reflect.Field f3 = GlobalLockTools.class.getDeclaredField("modifyConfirmService");
        f3.setAccessible(true);
        f3.set(tools, confirmService);

        java.lang.reflect.Field f4 = GlobalLockTools.class.getDeclaredField("objectMapper");
        f4.setAccessible(true);
        f4.set(tools, objectMapper);
    }

    @Test
    void testQueryGlobalLockWithSpanExceeded() {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            config.setQueryDuration(3600_000L);
            GlobalLockParamDto dto = new GlobalLockParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2025-10-01 10:00:00");
            dto.setTimeEnd("2025-10-02 10:00:01");
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2025-10-01 10:00:00"))
                    .thenReturn(0L);
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2025-10-02 10:00:01"))
                    .thenReturn(3600_000L + 1);
            mocked.when(() -> DateUtils.judgeExceedTimeDuration(0L, 3600_000L + 1, 3600_000L))
                    .thenReturn(true);
            mocked.when(() -> DateUtils.convertToHourFromTimeStamp(3600_000L)).thenReturn(1L);
            PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
            assertFalse(res.isSuccess());
            verify(rpcService, never()).getCallTC(any(), any(), any(), any(), any());
        }
    }

    @Test
    void testQueryGlobalLockWithOnlyStartAddsOneDayEndAndParses() {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            GlobalLockParamDto dto = new GlobalLockParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2020-01-01 00:00:00");

            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2020-01-01 00:00:00"))
                    .thenReturn(1000L);

            String body =
                    "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":null,\"pageNum\":null,\"total\":0,\"pages\":0,\"data\":[]}";
            ArgumentCaptor<GlobalLockParam> paramCaptor = ArgumentCaptor.forClass(GlobalLockParam.class);
            when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), any(), any(), any()))
                    .thenReturn(body);

            PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
            assertTrue(res.isSuccess());
            assertNotNull(res.getData());

            verify(rpcService)
                    .getCallTC(
                            any(),
                            eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"),
                            paramCaptor.capture(),
                            any(),
                            any());
            GlobalLockParam sent = paramCaptor.getValue();
            assertEquals(1000L, sent.getTimeStart());
            assertEquals(1000L + DateUtils.ONE_DAY_TIMESTAMP, sent.getTimeEnd());
        }
    }

    @Test
    void testQueryGlobalLockWithOnlyEndTime() {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            GlobalLockParamDto dto = new GlobalLockParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2020-01-01 00:00:00");
            dto.setTimeEnd("2020-01-01 00:00:00");

            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2020-01-01 00:00:00"))
                    .thenReturn(1000L);

            String body =
                    "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":null,\"pageNum\":null,\"total\":0,\"pages\":0,\"data\":[]}";
            ArgumentCaptor<GlobalLockParam> paramCaptor = ArgumentCaptor.forClass(GlobalLockParam.class);
            when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), any(), any(), any()))
                    .thenReturn(body);

            PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
            assertTrue(res.isSuccess());
            assertNotNull(res.getData());

            verify(rpcService)
                    .getCallTC(
                            any(),
                            eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"),
                            paramCaptor.capture(),
                            any(),
                            any());
            GlobalLockParam sent = paramCaptor.getValue();
            assertNotNull(sent.getTimeStart());
            assertNotNull(sent.getTimeEnd());
        }
    }

    @Test
    void testQueryGlobalLockWithBothNullTimesSetsNullsAndParses() {
        GlobalLockParamDto dto = new GlobalLockParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        ArgumentCaptor<GlobalLockParam> paramCaptor = ArgumentCaptor.forClass(GlobalLockParam.class);
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":null,\"pageNum\":null,\"total\":0,\"pages\":0,\"data\":[]}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);
        PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
        assertTrue(res.isSuccess());
        verify(rpcService)
                .getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), paramCaptor.capture(), any(), any());
        GlobalLockParam sent = paramCaptor.getValue();
        assertNull(sent.getTimeStart());
        assertNull(sent.getTimeEnd());
    }

    @Test
    void testQueryGlobalLockWithOnlyEndSetsTimesToNull() {
        GlobalLockParamDto dto = new GlobalLockParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        dto.setTimeEnd("2025-10-01 10:00:00");
        ArgumentCaptor<GlobalLockParam> paramCaptor = ArgumentCaptor.forClass(GlobalLockParam.class);
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":null,\"pageNum\":null,\"total\":0,\"pages\":0,\"data\":[]}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);
        PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
        assertTrue(res.isSuccess());
        verify(rpcService)
                .getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), paramCaptor.capture(), any(), any());
        GlobalLockParam sent = paramCaptor.getValue();
        assertNull(sent.getTimeStart());
        assertNull(sent.getTimeEnd());
    }

    @Test
    void testQueryGlobalLockWithStartAndEndWithinLimit_ParsesAndKeepsTimes() {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            config.setQueryDuration(3600_000L);
            GlobalLockParamDto dto = new GlobalLockParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2025-10-01 10:00:00");
            dto.setTimeEnd("2025-10-01 10:30:00");

            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2025-10-01 10:00:00"))
                    .thenReturn(1000L);
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2025-10-01 10:30:00"))
                    .thenReturn(2000L);
            mocked.when(() -> DateUtils.judgeExceedTimeDuration(1000L, 2000L, 3600_000L))
                    .thenReturn(false);

            ArgumentCaptor<GlobalLockParam> paramCaptor = ArgumentCaptor.forClass(GlobalLockParam.class);
            String body =
                    "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":null,\"pageNum\":null,\"total\":0,\"pages\":0,\"data\":[]}";
            when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"), any(), any(), any()))
                    .thenReturn(body);

            PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
            assertTrue(res.isSuccess());

            verify(rpcService)
                    .getCallTC(
                            any(),
                            eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/query"),
                            paramCaptor.capture(),
                            any(),
                            any());
            GlobalLockParam sent = paramCaptor.getValue();
            assertEquals(1000L, sent.getTimeStart());
            assertEquals(2000L, sent.getTimeEnd());
        }
    }

    @Test
    void testQueryGlobalLockNullResultReturnsFailure() {
        GlobalLockParamDto dto = new GlobalLockParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(rpcService.getCallTC(any(), any(), any(), any(), any())).thenReturn("null");
        PageResult<GlobalLockVO> res = tools.queryGlobalLock(new NameSpaceDetail(), dto);
        assertFalse(res.isSuccess());
    }

    @Test
    void testQueryGlobalLockJsonParseErrorThrowsRuntime() {
        GlobalLockParamDto dto = new GlobalLockParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(rpcService.getCallTC(any(), any(), any(), any(), any())).thenReturn("not-json");
        assertThrows(RuntimeException.class, () -> tools.queryGlobalLock(new NameSpaceDetail(), dto));
    }

    @Test
    void testDeleteGlobalLockInvalidKey() {
        when(confirmService.isValidKey("k")).thenReturn(false);
        String res = tools.deleteGlobalLock(
                new NameSpaceDetail(), new org.apache.seata.mcp.entity.param.GlobalLockDeleteParam(), "k");
        assertEquals("The modify key is not available", res);
    }

    @Test
    void testDeleteGlobalLockCallsRpcAndHandlesBlank() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.deleteCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/delete"), any(), any(), any()))
                .thenReturn(" ");
        String res = tools.deleteGlobalLock(new NameSpaceDetail(), new GlobalLockDeleteParam(), "k");
        assertEquals("delete global lock failed", res);
    }

    @Test
    void testDeleteGlobalLockOk() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.deleteCallTC(any(), any(), any(), any(), any())).thenReturn("ok");
        String res = tools.deleteGlobalLock(new NameSpaceDetail(), new GlobalLockDeleteParam(), "k");
        assertEquals("ok", res);
    }

    @Test
    void testCheckGlobalLock() {
        ArgumentCaptor<java.util.Map> pathCaptor = ArgumentCaptor.forClass(java.util.Map.class);
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/check"), any(), any(), any()))
                .thenReturn("");
        String res1 = tools.checkGlobalLock(new NameSpaceDetail(), "x", "b");
        assertEquals("check global lock failed, xid: x, branchId: b", res1);

        when(rpcService.getCallTC(any(), any(), any(), any(), any())).thenReturn("ok");
        String res2 = tools.checkGlobalLock(new NameSpaceDetail(), "x", "b");
        assertEquals("ok", res2);

        verify(rpcService, atLeastOnce())
                .getCallTC(any(), eq(RPCConstant.GLOBAL_LOCK_BASE_URL + "/check"), any(), pathCaptor.capture(), any());
        java.util.Map sentPath = pathCaptor.getValue();
        assertEquals("x", sentPath.get("xid"));
        assertEquals("b", sentPath.get("branchId"));
    }
}
