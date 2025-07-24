package org.apache.seata.server.console.service;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.server.console.entity.param.ServerLogParam;
import org.apache.seata.server.console.entity.vo.ServerLogVO;

import java.util.List;

public interface ServerLogService {
    SingleResult<?> getServerLog(ServerLogParam serverLogParam);
}
