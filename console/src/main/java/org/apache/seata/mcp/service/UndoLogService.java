package org.apache.seata.mcp.service;

public interface UndoLogService {
    String queryAndAnalyzeUndoLog(String resourceId,String branchId, String xid);
}
