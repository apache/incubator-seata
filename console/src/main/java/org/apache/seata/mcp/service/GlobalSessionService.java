package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.GlobalSessionParam;

import java.util.List;

public interface GlobalSessionService {
    String queryGlobalSession(GlobalSessionParam param);

    String deleteGlobalSession(String xid);

    String forceDeleteGlobalSession(String xid);

    String stopGlobalSession(String xid);

    String startGlobalSession(String xid);

    String sendCommitOrRollback(String xid);

    String changeGlobalStatus(String xid);

    List<String> getAbnormalSessions(Long startTime, Long endTime);
}
