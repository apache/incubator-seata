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

    @Tool(description = "根据branchId,xid分析对应rm的undolog数据")
    public String analyzeUndoLog(@ToolParam(description = "数据源唯一标识",required = true) String resourceId,
                                        @ToolParam(description = "分支事务id",required = true) String branchId,
                                        @ToolParam(description = "全局事务id",required = true) String xid){
        return undoLogService.queryAndAnalyzeUndoLog(resourceId, branchId, xid);
    }

}
