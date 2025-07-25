package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.ServerLogIndexParam;
import org.apache.seata.mcp.entity.param.ServerLogParam;

public interface ServerLogService {
    String analyseServerLog(ServerLogParam param);

    String getHistoryServerLogNums(ServerLogIndexParam param);
}
