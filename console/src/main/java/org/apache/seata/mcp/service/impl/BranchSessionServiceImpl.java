package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.service.BranchSessionService;
import org.apache.seata.mcp.service.MCPRPCService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class BranchSessionServiceImpl implements BranchSessionService {

    @Autowired
    @Lazy
    private MCPRPCService mcpRPCService;

    @Override
    public String deleteBranchSession(String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.deleteCallTC(
                RPCConstant.BRANCH_SESSION_BASE_URL + "/deleteBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("delete branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }

    @Override
    public String forceDeleteBranchSession(String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.deleteCallTC(
                RPCConstant.BRANCH_SESSION_BASE_URL + "/forceDeleteBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("force delete branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }

    @Override
    public String stopBranchSession(String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.putCallTC(
                RPCConstant.BRANCH_SESSION_BASE_URL + "/stopBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("stop branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }

    @Override
    public String startBranchRetry(String xid, String branchId) {
        Map<String, String> pathParams = new HashMap<>();
        pathParams.put("xid", xid);
        pathParams.put("branchId", branchId);
        String result = mcpRPCService.putCallTC(
                RPCConstant.BRANCH_SESSION_BASE_URL + "/startBranchSession", null, pathParams, null);
        if (StringUtils.isBlank(result)) {
            return String.format("start branch session failed, xid: %s, branchId: %s", xid, branchId);
        } else {
            return result;
        }
    }
}
