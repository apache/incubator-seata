package org.apache.seata.mcp.controller;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.mcp.manager.MCPServerManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ControlMcpControllerTest {

    private ControlMcpController controller;
    private MCPServerManager manager;

    @BeforeEach
    void setUp() throws Exception {
        controller = new ControlMcpController();
        manager = mock(MCPServerManager.class);
        java.lang.reflect.Field f = ControlMcpController.class.getDeclaredField("mcpServerEndpointProvider");
        f.setAccessible(true);
        f.set(controller, manager);
    }

    @Test
    void testChangeStatusStart() {
        when(manager.isRunning()).thenReturn(false);
        SingleResult<?> res = controller.changeStatus("start");
        assertTrue(res.isSuccess());
        verify(manager, times(1)).resume();
    }

    @Test
    void testChangeStatusStop() {
        when(manager.isRunning()).thenReturn(true);
        SingleResult<?> res = controller.changeStatus("stop");
        assertTrue(res.isSuccess());
        verify(manager, times(1)).pause();
    }

    @Test
    void testChangeStatusDefault() {
        SingleResult<?> res = controller.changeStatus("unknown");
        assertFalse(res.isSuccess());
    }

    @Test
    void testGetStatusRunning() {
        when(manager.isRunning()).thenReturn(true);
        SingleResult<?> res = controller.getStatus();
        assertTrue(res.isSuccess());
        assertEquals("MCP Service is Running", res.getData());
    }

    @Test
    void testGetStatusStopped() {
        when(manager.isRunning()).thenReturn(false);
        SingleResult<?> res = controller.getStatus();
        assertTrue(res.isSuccess());
        assertEquals("MCP Service is Stopped", res.getData());
    }

    @Test
    void testStartMcpServiceWhenNotRunning() {
        when(manager.isRunning()).thenReturn(false);
        SingleResult<?> res = controller.startMcpService();
        assertTrue(res.isSuccess());
        verify(manager, times(1)).resume();
    }

    @Test
    void testStartMcpServiceWhenRunning() {
        when(manager.isRunning()).thenReturn(true);
        SingleResult<?> res = controller.startMcpService();
        assertFalse(res.isSuccess());
    }

    @Test
    void testStopMcpServiceWhenRunning() {
        when(manager.isRunning()).thenReturn(true);
        SingleResult<?> res = controller.stopMcpService();
        assertTrue(res.isSuccess());
        verify(manager, times(1)).pause();
    }

    @Test
    void testStopMcpServiceWhenNotRunning() {
        when(manager.isRunning()).thenReturn(false);
        SingleResult<?> res = controller.stopMcpService();
        assertFalse(res.isSuccess());
    }
}
