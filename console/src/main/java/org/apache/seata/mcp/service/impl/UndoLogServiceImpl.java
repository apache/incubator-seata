package org.apache.seata.mcp.service.impl;

import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.parser.FastjsonUndoLogParser;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.service.UndoLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class UndoLogServiceImpl implements UndoLogService {

    @Autowired
    private BusinessDataSourceService dataSourceService;

    @Override
    public String queryAndAnalyzeUndoLog(UndoLogParam param) {
        // 1. 先根据参数查询对应rm的undo_log数据
        List<byte[]> undoLogInfo = dataSourceService.getUndoLogInfo(param);
        if (undoLogInfo.isEmpty()) {
            return "failed to get undo log info";
        }
        // 2. 再通过FastJsonParser将undoLogInfo反序列化为BranchUndoLog
        FastjsonUndoLogParser parser = new FastjsonUndoLogParser();
        List<String> result = new ArrayList<>();
        for (byte[] bytes : undoLogInfo) {
            result.add(parser.decode(bytes));
        }
        return result.toString();
    }
}
