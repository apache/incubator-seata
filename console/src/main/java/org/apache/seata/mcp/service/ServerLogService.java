package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.ServerLogIndexParam;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;

public interface ServerLogService {
    String analyseServerLog(NameSpaceDetail nameSpaceDetail, ServerLogParam param);

    String getHistoryServerLogNums(NameSpaceDetail nameSpaceDetail, ServerLogIndexParam param);
}
