package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
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

    private static final String GET_UNDO_LOG_SQL = "SELECT rollback_info FROM undo_log WHERE";

    private final String PARAM_BRANCH_ID = " branch_id = ?";

    private final String PARAM_XID = " xid = ?";

    private final String UNDO_LOG_STATUS = " log_status = ?";

    private final String UNDO_LOG_CREATE_TIME = " log_created BETWEEN ? AND ?";

    private final String UNDO_LOG_MODIFY_TIME = " log_modified BETWEEN ? AND ?";

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
    public List<byte[]> getUndoLogInfo(UndoLogParam param) {
        String sql = GET_UNDO_LOG_SQL;
        List<Object> params = new ArrayList<>();
        String branchId = param.getBranchId();
        String xid = param.getXid();
        String resourceId = param.getResourceId();
        Integer logStatus = param.getLogStatus();
        UndoLogParam.CreateTime logCreateTime = param.getLogCreateTime();
        UndoLogParam.ModifyTime logModifiedTime = param.getLogModifiedTime();
        int idx = 0;
        if(StringUtils.isBlank(resourceId)){
            throw new StoreException("you cannot query without resourceId");
        }
        if(StringUtils.isNotBlank(branchId)){
            sql += PARAM_BRANCH_ID;
            params.add(branchId);
        }
        if(StringUtils.isNotBlank(xid)){
            sql += PARAM_XID;
            params.add(xid);
        }
        if(logStatus != null){
            sql += UNDO_LOG_STATUS;
            params.add(logStatus);
        }
        if(logCreateTime != null){
            String startTime = logCreateTime.getStartTime();
            String endTime = logCreateTime.getEndTime();
            if(startTime != null && endTime != null){
                sql += UNDO_LOG_CREATE_TIME;
            }
            if(startTime != null){
                params.add(startTime);
            }
            if(endTime != null){
                params.add(endTime);
            }
        }
        if(logModifiedTime != null){
            String startTime = logModifiedTime.getStartTime();
            String endTime = logModifiedTime.getEndTime();
            if(startTime != null && endTime != null){
                sql += UNDO_LOG_MODIFY_TIME;
            }
            if(startTime != null){
                params.add(startTime);
            }
            if(endTime != null){
                params.add(endTime);
            }
        }
        List<byte[]> result = new ArrayList<>();
        List<Map<String, Object>> query = sqlExecutionTemplate.query(resourceId, sql, params.toArray());
        for(Map<String, Object> map : query){
            Object rollbackInfo = map.get("rollback_info");
            if(rollbackInfo != null){
                result.add((byte[])rollbackInfo);
            }
        }
        return result;
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
