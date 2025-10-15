package org.apache.seata.mcp.controller.tools;

import org.apache.seata.common.result.PageResult;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.service.UndoLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class UndoLogToolsTest {

    private UndoLogTools tools;
    private UndoLogService service;

    @BeforeEach
    void setUp() throws Exception {
        tools = new UndoLogTools();
        service = mock(UndoLogService.class);
        java.lang.reflect.Field f = UndoLogTools.class.getDeclaredField("undoLogService");
        f.setAccessible(true);
        f.set(tools, service);
    }

    @Test
    void testAnalyzeUndoLogDelegatesAndReturns() {
        PageResult<Object> pr = PageResult.success();
        doReturn(pr).when(service).queryAndAnalyzeUndoLog(any(UndoLogParam.class));
        PageResult<?> res = tools.analyzeUndoLog(new UndoLogParam());
        assertEquals(pr, res);
        verify(service, times(1)).queryAndAnalyzeUndoLog(any());
    }
}
