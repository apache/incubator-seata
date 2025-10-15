package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ModifyConfirmService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BranchSessionToolsTest {

    private BranchSessionTools tools;
    private MCPRPCService rpcService;
    private ModifyConfirmService confirmService;

    @BeforeEach
    void setUp() throws Exception {
        tools = new BranchSessionTools();
        rpcService = mock(MCPRPCService.class);
        confirmService = mock(ModifyConfirmService.class);

        java.lang.reflect.Field f1 = BranchSessionTools.class.getDeclaredField("mcpRPCService");
        f1.setAccessible(true);
        f1.set(tools, rpcService);

        java.lang.reflect.Field f2 = BranchSessionTools.class.getDeclaredField("modifyConfirmService");
        f2.setAccessible(true);
        f2.set(tools, confirmService);
    }

    @Test
    void testDeleteBranchSessionWithInvalidKey() {
        when(confirmService.isValidKey("k")).thenReturn(false);
        String res = tools.deleteBranchSession(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("The modify key is not available", res);
    }

    @Test
    void testStopBranchSessionWithInvalidKey() {
        when(confirmService.isValidKey("bad")).thenReturn(false);
        String res = tools.stopBranchSession(new NameSpaceDetail(), "x", "b", "bad");
        assertEquals("The modify key is not available", res);
    }

    @Test
    void testStartBranchRetryWithInvalidKey() {
        when(confirmService.isValidKey("bad")).thenReturn(false);
        String res = tools.startBranchRetry(new NameSpaceDetail(), "x", "b", "bad");
        assertEquals("The modify key is not available", res);
    }

    @Test
    void testDeleteBranchSessionSuccess() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.deleteCallTC(
                        any(), eq(RPCConstant.BRANCH_SESSION_BASE_URL + "/deleteBranchSession"), any(), any(), any()))
                .thenReturn("ok");
        String res = tools.deleteBranchSession(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("ok", res);
        verify(rpcService, times(1))
                .deleteCallTC(
                        any(), eq(RPCConstant.BRANCH_SESSION_BASE_URL + "/deleteBranchSession"), any(), any(), any());
    }

    @Test
    void testDeleteBranchSessionFailureBlank() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.deleteCallTC(any(), any(), any(), any(), any())).thenReturn(" ");
        String res = tools.deleteBranchSession(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("delete branch session failed, xid: x, branchId: b", res);
    }

    @Test
    void testStopBranchSessionSuccessAndFailure() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.putCallTC(
                        any(), eq(RPCConstant.BRANCH_SESSION_BASE_URL + "/stopBranchSession"), any(), any(), any()))
                .thenReturn("");
        String res1 = tools.stopBranchSession(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("stop branch session failed, xid: x, branchId: b", res1);

        when(rpcService.putCallTC(any(), any(), any(), any(), any())).thenReturn("ok");
        String res2 = tools.stopBranchSession(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("ok", res2);
    }

    @Test
    void testStartBranchRetrySuccessAndFailure() {
        when(confirmService.isValidKey("k")).thenReturn(true);
        when(rpcService.putCallTC(
                        any(), eq(RPCConstant.BRANCH_SESSION_BASE_URL + "/startBranchSession"), any(), any(), any()))
                .thenReturn(null);
        String res1 = tools.startBranchRetry(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("start branch session failed, xid: x, branchId: b", res1);

        when(rpcService.putCallTC(any(), any(), any(), any(), any())).thenReturn("ok");
        String res2 = tools.startBranchRetry(new NameSpaceDetail(), "x", "b", "k");
        assertEquals("ok", res2);
    }
}
