package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessDataSourceServiceImpl implements BusinessDataSourceService {

    @Autowired
    private SqlExecutionTemplate sqlExecutionTemplate;

    private static final String GET_TABLE_NAME_SQL = "SELECT TABLE_NAME, TABLE_COMMENT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? ";

    private static final String GET_SCHEMA_SQL = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
            "WHERE TABLE_SCHEMA = ? AND TABLE_NAME = ?";

    private static final String GET_UNDO_LOG_SQL = "SELECT rollback_info FROM undo_log WHERE branch_id = ? AND xid = ?";

    @Override
    public List<String> getTableNamesBySchema(String resourceId) {
        String schema = getSchemaNameByResourceId(resourceId);
        if (StringUtils.isBlank(schema)) {
            throw new StoreException("failed to get schema by resourceId: " + resourceId);
        }else{
            List<Map<String, Object>> maps = sqlExecutionTemplate.query(resourceId,GET_TABLE_NAME_SQL,schema);
            return maps.stream().map(map -> {
                String tableName = String.valueOf(map.get("TABLE_NAME"));
                String tableComment = String.valueOf(map.get("TABLE_COMMENT"));
                return tableName + " (" + tableComment + ")";
            }).collect(Collectors.toList());
        }
    }

    @Override
    public List<Map<String, Object>> getTableSchemaByTableName(String resourceId, String tableName) {
        String schema = getSchemaNameByResourceId(resourceId);
        if (StringUtils.isBlank(schema)) {
            throw new StoreException("failed to get schema by resourceId: " + resourceId);
        }else{
            return sqlExecutionTemplate.query(resourceId,GET_SCHEMA_SQL,schema,tableName);
        }
    }

    @Override
    public List<Map<String, Object>> runSql(String sql, String resourceId) {
        return sqlExecutionTemplate.query(resourceId,sql);
    }

    @Override
    public byte[] getUndoLogInfo(String resourceId, String branchId, String xid) {
        List<Map<String, Object>> query = sqlExecutionTemplate.query(resourceId, GET_UNDO_LOG_SQL, branchId, xid);
        for(Map<String, Object> map : query){
            Object rollbackInfo = map.get("rollback_info");
            if(rollbackInfo != null){
                return (byte[])rollbackInfo;
            }
        }
        return new byte[0];
    }


    public String getSchemaNameByResourceId(String resourceId) {
        if (StringUtils.isBlank(resourceId)) {
            return "";
        }
        int idx = resourceId.lastIndexOf("/");
        if (idx != -1 && idx != resourceId.length() - 1) {
            return resourceId.substring(idx + 1);
        }
        return "";
    }
}
