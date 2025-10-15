package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.ServerLogPageVO;
import org.apache.seata.mcp.service.ServerLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ServerLogToolsTest {

    private ServerLogTools tools;
    private ServerLogService service;

    @BeforeEach
    void setUp() throws Exception {
        tools = new ServerLogTools();
        service = mock(ServerLogService.class);
        java.lang.reflect.Field f = ServerLogTools.class.getDeclaredField("logService");
        f.setAccessible(true);
        f.set(tools, service);
    }

    @Test
    void testGetServerLogFileDelegatesToService() {
        ServerLogParam param = new ServerLogParam();
        param.setLogType("error");
        param.setPage(2);
        ServerLogPageVO<String> page = new ServerLogPageVO<>();
        page.setHasMorePages(true);
        page.setData(Arrays.asList("l1", "l2"));
        when(service.analyseServerLogFile(any(), any())).thenReturn(page);
        ServerLogPageVO<String> res = tools.getServerLogFile(new NameSpaceDetail(), param);
        assertEquals(2, res.getData().size());
        verify(service, times(1)).analyseServerLogFile(any(), any());
    }
}
