package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.ServerLogIndexParam;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.service.ServerLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ServerLogTools {

    @Autowired
    private ServerLogService logService;

    @Tool(description = "Get the latest or history running logs on the server side")
    public String getServerLog(@ToolParam(description = "server log file query parameters. when getting history logs, curLogNum and logTime are both required",required = true)ServerLogParam param){
        return logService.analyseServerLog(param);
    }

    @Tool(description = "Obtain the server run log index nums of the specified type or creation time, If you do not specify a type, it is an all-type log，logTime is required")
    public String getHistoryServerLogNums(@ToolParam(description = "Log file index query parameters",required = true) ServerLogIndexParam param){
        return logService.getHistoryServerLogNums(param);
    }
}
