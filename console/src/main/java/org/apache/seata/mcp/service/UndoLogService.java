package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.UndoLogParam;

public interface UndoLogService {
    String queryAndAnalyzeUndoLog(UndoLogParam undoLogParam);
}
