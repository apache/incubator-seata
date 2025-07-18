package org.apache.seata.server.console.service;

import org.apache.seata.server.console.entity.param.ServerLogParam;

public interface ServerLogService {
    String getServerLog(ServerLogParam serverLogParam);
}
