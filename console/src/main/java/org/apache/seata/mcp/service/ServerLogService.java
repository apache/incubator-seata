package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.ServerLogParam;
import org.apache.seata.mcp.entity.pojo.NameSpaceDetail;
import org.apache.seata.mcp.entity.vo.ServerLogPageVO;

public interface ServerLogService {
    ServerLogPageVO<String> analyseServerLogFile(NameSpaceDetail nameSpaceDetail, ServerLogParam param);
}
