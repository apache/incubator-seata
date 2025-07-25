package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.RPCConstant;
import org.apache.seata.mcp.entity.param.ServerLogIndexParam;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.service.MCPRPCService;
import org.apache.seata.mcp.service.ServerLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerLogServiceImpl implements ServerLogService {

    @Autowired
    private MCPRPCService mcpRPCService;

    @Override
    public String analyseServerLog(ServerLogParam param) {
        String result = mcpRPCService.getCallTC(RPCConstant.SERVER_LOG_BASE_URL + "/getServerLog", param, null, null);
        if (StringUtils.isBlank(result)) {
            return "analyse server log failed";
        } else {
            return result;
        }
    }

    @Override
    public String getHistoryServerLogNums(ServerLogIndexParam param) {
        String result = mcpRPCService.getCallTC(RPCConstant.SERVER_LOG_BASE_URL + "/getHistoryServerLogNums", param, null, null);
        if (StringUtils.isBlank(result)) {
            return "get history server log nums failed";
        } else {
            return result;
        }
    }


}
