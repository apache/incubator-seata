package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.GlobalLockParam;

public interface GlobalLockService {
    String queryGlobalLock(GlobalLockParam param);

    String deleteGlobalLock(GlobalLockParam param);

    String checkGlobalLock(String xid, String branchId);
}
