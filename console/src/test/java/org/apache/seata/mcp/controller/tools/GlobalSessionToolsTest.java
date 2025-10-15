package org.apache.seata.mcp.controller.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.result.PageResult;
import org.apache.seata.core.model.GlobalStatus;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.dto.GlobalSessionParamDto;
import org.apache.seata.mcp.entity.param.GlobalAbnormalSessionParam;
import org.apache.seata.mcp.entity.param.GlobalSessionParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.GlobalSessionVO;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.apache.seata.mcp.utils.DateUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GlobalSessionToolsTest {

    private GlobalSessionTools tools;
    private MCPRPCService rpcService;
    private MCPProperties config;
    private ObjectMapper objectMapper;
    private ModifyConfirmService confirmService;

    @BeforeEach
    void setUp() throws Exception {
        tools = new GlobalSessionTools();
        rpcService = mock(MCPRPCService.class);
        config = new MCPProperties();
        objectMapper = new ObjectMapper();
        confirmService = mock(ModifyConfirmService.class);

        java.lang.reflect.Field f1 = GlobalSessionTools.class.getDeclaredField("mcpRPCService");
        f1.setAccessible(true);
        f1.set(tools, rpcService);

        java.lang.reflect.Field f2 = GlobalSessionTools.class.getDeclaredField("configuration");
        f2.setAccessible(true);
        f2.set(tools, config);

        java.lang.reflect.Field f3 = GlobalSessionTools.class.getDeclaredField("objectMapper");
        f3.setAccessible(true);
        f3.set(tools, objectMapper);

        java.lang.reflect.Field f4 = GlobalSessionTools.class.getDeclaredField("modifyConfirmService");
        f4.setAccessible(true);
        f4.set(tools, confirmService);
    }

    @Test
    void testQueryGlobalSession_SpanExceededThrows() {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            config.setQueryDuration(3600_000L);
            GlobalSessionParamDto dto = new GlobalSessionParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2025-10-01 00:00:00");
            dto.setTimeEnd("2025-10-02 01:00:00");
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2025-10-01 00:00:00"))
                    .thenReturn(0L);
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2025-10-02 01:00:00"))
                    .thenReturn(3600_000L + 1);
            mocked.when(() -> DateUtils.judgeExceedTimeDuration(0L, 3600_000L + 1, 3600_000L))
                    .thenReturn(true);
            mocked.when(() -> DateUtils.convertToHourFromTimeStamp(3600_000L)).thenReturn(1L);
            assertThrows(IllegalArgumentException.class, () -> tools.queryGlobalSession(new NameSpaceDetail(), dto));
        }
    }

    @Test
    void testQueryGlobalSession_OnlyStartAddsOneDayEndAndParses() throws Exception {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            GlobalSessionParamDto dto = new GlobalSessionParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2020-01-01 00:00:00");
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2020-01-01 00:00:00"))
                    .thenReturn(1000L);

            String body =
                    "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":10,\"pageNum\":1,\"total\":0,\"pages\":0,\"data\":[]}";
            ArgumentCaptor<GlobalSessionParam> captor = ArgumentCaptor.forClass(GlobalSessionParam.class);
            when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                    .thenReturn(body);

            PageResult<GlobalSessionVO> res = tools.queryGlobalSession(new NameSpaceDetail(), dto);
            assertTrue(res.isSuccess());
            verify(rpcService)
                    .getCallTC(
                            any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), captor.capture(), any(), any());
            GlobalSessionParam sent = captor.getValue();
            assertEquals(1000L, sent.getTimeStart());
            assertEquals(1000L + DateUtils.ONE_DAY_TIMESTAMP, sent.getTimeEnd());
        }
    }

    @Test
    void testQueryGlobalSession_WithStartAndEndTime() throws Exception {
        try (MockedStatic<DateUtils> mocked = mockStatic(DateUtils.class)) {
            GlobalSessionParamDto dto = new GlobalSessionParamDto();
            dto.setPageNum(1);
            dto.setPageSize(10);
            dto.setTimeStart("2020-01-01 00:00:00");
            dto.setTimeEnd("2020-01-02 00:00:00");
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2020-01-01 00:00:00"))
                    .thenReturn(1000L);
            mocked.when(() -> DateUtils.convertToTimeStampFromDateTime("2020-01-02 00:00:00"))
                    .thenReturn(1200L);

            String body =
                    "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":10,\"pageNum\":1,\"total\":0,\"pages\":0,\"data\":[]}";
            ArgumentCaptor<GlobalSessionParam> captor = ArgumentCaptor.forClass(GlobalSessionParam.class);
            when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                    .thenReturn(body);

            PageResult<GlobalSessionVO> res = tools.queryGlobalSession(new NameSpaceDetail(), dto);
            assertTrue(res.isSuccess());
            verify(rpcService)
                    .getCallTC(
                            any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), captor.capture(), any(), any());
            GlobalSessionParam sent = captor.getValue();
            assertEquals(1000L, sent.getTimeStart());
        }
    }

    @Test
    void testQueryGlobalSession_BothNullTimesSetsNulls() throws Exception {
        GlobalSessionParamDto dto = new GlobalSessionParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        ArgumentCaptor<GlobalSessionParam> captor = ArgumentCaptor.forClass(GlobalSessionParam.class);
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":10,\"pageNum\":1,\"total\":0,\"pages\":0,\"data\":[]}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);
        PageResult<GlobalSessionVO> res = tools.queryGlobalSession(new NameSpaceDetail(), dto);
        assertTrue(res.isSuccess());
        verify(rpcService)
                .getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), captor.capture(), any(), any());
        GlobalSessionParam sent = captor.getValue();
        assertNull(sent.getTimeStart());
        assertNull(sent.getTimeEnd());
    }

    @Test
    void testQueryGlobalSession_ErrorJsonFormat() throws Exception {
        GlobalSessionParamDto dto = new GlobalSessionParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        ArgumentCaptor<GlobalSessionParam> captor = ArgumentCaptor.forClass(GlobalSessionParam.class);
        String body = "k";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);
        assertThrows(RuntimeException.class, () -> tools.queryGlobalSession(new NameSpaceDetail(), dto));
    }

    @Test
    void testQueryGlobalSession_NullResultReturnsFailure() {
        GlobalSessionParamDto dto = new GlobalSessionParamDto();
        dto.setPageNum(1);
        dto.setPageSize(10);
        when(rpcService.getCallTC(any(), any(), any(), any(), any())).thenReturn("null");
        PageResult<GlobalSessionVO> res = tools.queryGlobalSession(new NameSpaceDetail(), dto);
        assertFalse(res.isSuccess());
    }

    @Test
    void testQueryGlobalSession_ParseErrorThrowsRuntime() throws Exception {
        GlobalSessionParamDto dto = new GlobalSessionParamDto();
        when(rpcService.getCallTC(any(), any(), any(), any(), any())).thenReturn("not-json");
        assertThrows(RuntimeException.class, () -> tools.queryGlobalSession(new NameSpaceDetail(), dto));
    }

    @Test
    void testDeleteGlobalSession_InvalidKeyAndBlank() {
        when(confirmService.isValidKey("k")).thenReturn(false);
        String res1 = tools.deleteGlobalSession(new NameSpaceDetail(), "x", "k");
        assertEquals("The modify key is not available", res1);

        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.deleteCallTC(
                        any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/deleteGlobalSession"), any(), any(), any()))
                .thenReturn(" ");
        String res2 = tools.deleteGlobalSession(new NameSpaceDetail(), "x", "k");
        assertEquals("delete global session failed, xid: x", res2);
    }

    @Test
    void testDeleteGlobalSession_Ok() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.deleteCallTC(any(), any(), any(), any(), any())).thenReturn("ok");
        String res = tools.deleteGlobalSession(new NameSpaceDetail(), "x", "k");
        assertEquals("ok", res);
    }

    @Test
    void testDeleteGlobalSession_ErrorModifyKey() {
        String res = tools.deleteGlobalSession(new NameSpaceDetail(), "x", "k");
        assertEquals("The modify key is not available", res);
    }

    @Test
    void testStopStartChange_SendCommitRollback() {
        when(confirmService.isValidKey("k")).thenReturn(true);

        when(rpcService.putCallTC(
                        any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/stopGlobalSession"), any(), any(), any()))
                .thenReturn("");
        assertEquals(
                "stop global session retry failed, xid: x", tools.stopGlobalSession(new NameSpaceDetail(), "x", "k"));

        when(rpcService.putCallTC(
                        any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/startGlobalSession"), any(), any(), any()))
                .thenReturn("");
        assertEquals(
                "start the global session retry failed, xid: x",
                tools.startGlobalSession(new NameSpaceDetail(), "x", "k"));

        when(rpcService.putCallTC(
                        any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/sendCommitOrRollback"), any(), any(), any()))
                .thenReturn("");
        assertEquals(
                "send global session to commit or rollback to rm failed, xid: x",
                tools.sendCommitOrRollback(new NameSpaceDetail(), "x", "k"));

        when(rpcService.putCallTC(
                        any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/changeGlobalStatus"), any(), any(), any()))
                .thenReturn("");
        assertEquals(
                "change the global session status failed, xid: x",
                tools.changeGlobalStatus(new NameSpaceDetail(), "x", "k"));

        when(rpcService.putCallTC(any(), any(), any(), any(), any())).thenReturn("ok");
        assertEquals("ok", tools.stopGlobalSession(new NameSpaceDetail(), "x", "k"));
        assertEquals("ok", tools.startGlobalSession(new NameSpaceDetail(), "x", "k"));
        assertEquals("ok", tools.sendCommitOrRollback(new NameSpaceDetail(), "x", "k"));
        assertEquals("ok", tools.changeGlobalStatus(new NameSpaceDetail(), "x", "k"));
    }

    @Test
    void testStopStartChange_SendCommitRollback_ErrorModifyKey() {
        assertEquals("The modify key is not available", tools.stopGlobalSession(new NameSpaceDetail(), "x", "k"));
        assertEquals("The modify key is not available", tools.startGlobalSession(new NameSpaceDetail(), "x", "k"));
        assertEquals("The modify key is not available", tools.sendCommitOrRollback(new NameSpaceDetail(), "x", "k"));
        assertEquals("The modify key is not available", tools.changeGlobalStatus(new NameSpaceDetail(), "x", "k"));
    }

    @Test
    void testGetAbnormalSessions_FillsStatusesAndLimits200() {
        List<GlobalSessionVO> batch = new ArrayList<>();
        for (int i = 0; i < 210; i++) {
            GlobalSessionVO vo = new GlobalSessionVO();
            vo.setStatus(GlobalStatus.CommitFailed.getCode());
            batch.add(vo);
        }
        String dataArray = new ObjectMapper().valueToTree(batch).toString();
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":100,\"pageNum\":1,\"total\":210,\"pages\":3,\"data\":"
                        + dataArray + "}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);

        GlobalAbnormalSessionParam abnormal = new GlobalAbnormalSessionParam();
        List<GlobalSessionVO> res = tools.getAbnormalSessions(new NameSpaceDetail(), abnormal);
        assertTrue(res.size() <= 200);
        assertFalse(res.isEmpty());
    }

    @Test
    void testGetAbnormalSessions_FillsStatuses() {
        List<GlobalSessionVO> batch = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            GlobalSessionVO vo = new GlobalSessionVO();
            vo.setStatus(GlobalStatus.CommitFailed.getCode());
            batch.add(vo);
        }
        String dataArray = new ObjectMapper().valueToTree(batch).toString();
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":30,\"pageNum\":1,\"total\":90,\"pages\":3,\"data\":"
                        + dataArray + "}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);

        GlobalAbnormalSessionParam abnormal = new GlobalAbnormalSessionParam();
        List<GlobalSessionVO> res = tools.getAbnormalSessions(new NameSpaceDetail(), abnormal);
        assertEquals(90, res.size());
    }

    @Test
    void testGetAbnormalSessionsWithEmptyData() throws NoSuchFieldException, IllegalAccessException {
        List<GlobalSessionVO> batch = new ArrayList<>();
        String dataArray = new ObjectMapper().valueToTree(batch).toString();
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":30,\"pageNum\":1,\"total\":90,\"pages\":3,\"data\":"
                        + dataArray + "}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);

        GlobalAbnormalSessionParam abnormal = new GlobalAbnormalSessionParam();
        Field field = tools.getClass().getDeclaredField("exceptionStatus");
        List<Integer> abnormalStatus = Arrays.asList(10, 11, 12);
        field.setAccessible(true);
        field.set(tools, abnormalStatus);
        List<GlobalSessionVO> res = tools.getAbnormalSessions(new NameSpaceDetail(), abnormal);
        assertTrue(res.isEmpty());
    }

    @Test
    void testGetAbnormalSessionsWithNullData() {
        String body =
                "{\"code\":\"200\",\"message\":\"success\",\"pageSize\":30,\"pageNum\":1,\"total\":90,\"pages\":3}";
        when(rpcService.getCallTC(any(), eq(RPCConstant.GLOBAL_SESSION_BASE_URL + "/query"), any(), any(), any()))
                .thenReturn(body);
        GlobalAbnormalSessionParam abnormal = new GlobalAbnormalSessionParam();
        List<GlobalSessionVO> res = tools.getAbnormalSessions(new NameSpaceDetail(), abnormal);
        assertTrue(res.isEmpty());
    }
}
