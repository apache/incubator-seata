package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.service.ModifyConfirmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ModifyConfirmToolsTest {

    private ModifyConfirmTools tools;
    private ModifyConfirmService service;

    @BeforeEach
    void setUp() throws Exception {
        tools = new ModifyConfirmTools();
        service = mock(ModifyConfirmService.class);
        java.lang.reflect.Field f = ModifyConfirmTools.class.getDeclaredField("modifyConfirmService");
        f.setAccessible(true);
        f.set(tools, service);
    }

    @Test
    void testConfirmAndGetKey_BlankInputThrows() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> tools.confirmAndGetKey(" \t\n"));
        assertEquals("User confirmation string is required.", ex.getMessage());
        verify(service, never()).confirmAndGetKey();
    }

    @Test
    void testConfirmAndGetKey_WithoutKeywordThrows() {
        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () -> tools.confirmAndGetKey("请执行删除操作"));
        String msg = ex.getMessage();
        org.junit.jupiter.api.Assertions.assertTrue(msg.contains("Confirmation string must explicitly contain"));
        verify(service, never()).confirmAndGetKey();
    }

    @Test
    void testConfirmAndGetKey_SuccessWithChineseKeyword() {
        Map<String, String> ret = new HashMap<>();
        ret.put("modify_key", "k1");
        ret.put("expire_time", "60s");
        when(service.confirmAndGetKey()).thenReturn(ret);

        Map<String, String> res = tools.confirmAndGetKey("我已确认要删除指定全局会话，确认");
        assertEquals("k1", res.get("modify_key"));
        assertEquals("60s", res.get("expire_time"));
        verify(service, times(1)).confirmAndGetKey();
    }

    @Test
    void testConfirmAndGetKey_SuccessWithEnglishKeyword() {
        Map<String, String> ret = new HashMap<>();
        ret.put("modify_key", "k2");
        ret.put("expire_time", "60s");
        when(service.confirmAndGetKey()).thenReturn(ret);

        Map<String, String> res = tools.confirmAndGetKey("I confirm to delete the global session, confirm");
        assertEquals("k2", res.get("modify_key"));
        verify(service, times(1)).confirmAndGetKey();
    }
}
