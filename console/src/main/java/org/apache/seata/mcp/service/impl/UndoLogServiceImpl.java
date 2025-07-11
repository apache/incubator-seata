package org.apache.seata.mcp.service.impl;

import org.apache.seata.mcp.parser.FastjsonUndoLogParser;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.service.UndoLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UndoLogServiceImpl implements UndoLogService {

    @Autowired
    private BusinessDataSourceService dataSourceService;

    @Override
    public String queryAndAnalyzeUndoLog(String resourceId,String branchId, String xid) {
        // 1. 先根据参数查询对应rm的undolog数据
        byte[] undoLogInfo = dataSourceService.getUndoLogInfo(resourceId, branchId, xid);
        // 2. 再通过FastJsonParser将undoLogInfo反序列化为BranchUndoLog
        FastjsonUndoLogParser parser = new FastjsonUndoLogParser();
        return parser.decode(undoLogInfo);
    }
}
