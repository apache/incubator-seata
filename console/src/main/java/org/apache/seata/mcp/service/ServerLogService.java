package org.apache.seata.mcp.service;

import org.apache.seata.common.result.PageResult;
import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;

public interface ServerLogService {
    PageResult<String> analyseServerLogFile(NameSpaceDetail nameSpaceDetail, ServerLogParam param);
}
