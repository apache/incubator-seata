package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.service.UndoLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UndoLogTools {

    @Autowired
    private UndoLogService undoLogService;

    @Tool(description = "analyze the undo_log data of rm")
    public String analyzeUndoLog(
            @ToolParam(description = "UndoLog Query parameters", required = true) UndoLogParam undoLogParam) {
        return undoLogService.queryAndAnalyzeUndoLog(undoLogParam);
    }
}
