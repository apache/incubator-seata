package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.service.UndoLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UndoLogTools {

    @Autowired
    private UndoLogService undoLogService;

    @Tool(description = "Based on the branchId, xid analyzes the undo_log data of rm")
    public String analyzeUndoLog(@ToolParam(description = "Unique identifier of the data source",required = true) String resourceId,
                                        @ToolParam(description = "Branch transaction ID",required = true) String branchId,
                                        @ToolParam(description = "Global transaction ID",required = true) String xid){
        return undoLogService.queryAndAnalyzeUndoLog(resourceId, branchId, xid);
    }

}
