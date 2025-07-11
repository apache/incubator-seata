package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.GlobalSessionParam;

public interface GlobalSessionService {
    String queryGlobalSession(GlobalSessionParam param);

    String deleteGlobalSession(String xid);

    String forceDeleteGlobalSession(String xid);

    String stopGlobalSession(String xid);

    String startGlobalSession(String xid);

    String sendCommitOrRollback(String xid);

    String changeGlobalStatus(String xid);
}
