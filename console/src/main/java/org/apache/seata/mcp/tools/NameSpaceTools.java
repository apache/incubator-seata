package org.apache.seata.mcp.tools;

import org.apache.seata.mcp.core.constant.RPCConstant;
import org.apache.seata.mcp.service.MCPRPCService;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.stereotype.Service;

@Service
public class NameSpaceTools {

    private final MCPRPCService mcpRPCService;

    public NameSpaceTools(MCPRPCService mcpRPCService) {
        this.mcpRPCService = mcpRPCService;
    }

    @McpTool(description = "Get the namespace and cluster or vgroup where all TC/Servers are located")
    public String getTCNameSpaces() {
        return mcpRPCService.getCallNameSpace(RPCConstant.GET_NAMESPACE_PATH, null, null, null);
    }
}
