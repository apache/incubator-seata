package org.apache.seata.mcp.service;

import org.apache.seata.mcp.entity.param.UndoLogParam;

import java.util.List;
import java.util.Map;

public interface BusinessDataSourceService {
    List<String> getTableNamesBySchema(String resourceId);

    List<Map<String, Object>> getTableSchemaByTableName(String resourceId,String tableName);

    List<Map<String, Object>> runSql(String sql,String resourceId);

    List<byte[]> getUndoLogInfo(UndoLogParam undoLogParam);
}
