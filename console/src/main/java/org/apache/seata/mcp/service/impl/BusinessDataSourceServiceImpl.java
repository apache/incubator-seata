/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.constant.SqlConstant;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessDataSourceServiceImpl implements BusinessDataSourceService {

    @Autowired
    private SqlExecutionTemplate sqlExecutionTemplate;

    @Autowired
    private MCPProperties mcpProperties;

    @Override
    public List<String> getTableNamesBySchema(String resourceId) {
        String schema = getSchemaNameByResourceId(resourceId);
        if (StringUtils.isBlank(schema)) {
            throw new StoreException("failed to get schema by resourceId: " + resourceId);
        } else {
            List<Map<String, Object>> maps =
                    sqlExecutionTemplate.query(resourceId, SqlConstant.GET_TABLE_NAME_SQL, schema);
            return maps.stream()
                    .map(map -> {
                        String tableName = String.valueOf(map.get("TABLE_NAME"));
                        String tableComment = String.valueOf(map.get("TABLE_COMMENT"));
                        return tableName + " (" + tableComment + ")";
                    })
                    .collect(Collectors.toList());
        }
    }

    @Override
    public List<Map<String, Object>> getTableSchemaByTableName(String resourceId, String tableName) {
        String schema = getSchemaNameByResourceId(resourceId);
        if (StringUtils.isBlank(schema)) {
            throw new StoreException("failed to get schema by resourceId: " + resourceId);
        } else {
            return sqlExecutionTemplate.query(resourceId, SqlConstant.GET_SCHEMA_SQL, schema, tableName);
        }
    }

    @Override
    public List<Map<String, Object>> runSql(String sql, String resourceId) {
        return sqlExecutionTemplate.query(resourceId, sql);
    }

    @Override
    public List<byte[]> getUndoLogInfo(UndoLogParam param) {
        long max_time_duration = mcpProperties.getQueryDuration();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String sql = SqlConstant.GET_UNDO_LOG_SQL;
        List<Object> params = new ArrayList<>();
        String branchId = param.getBranchId();
        String xid = param.getXid();
        String resourceId = param.getResourceId();
        Integer logStatus = param.getLogStatus();
        UndoLogParam.CreateTime logCreateTime = param.getLogCreateTime();
        UndoLogParam.ModifyTime logModifiedTime = param.getLogModifiedTime();
        int idx = 0;
        if (StringUtils.isBlank(resourceId)) {
            throw new StoreException("you cannot query without resourceId");
        }
        if (StringUtils.isNotBlank(branchId)) {
            sql += SqlConstant.PARAM_BRANCH_ID_SQL;
            params.add(branchId);
        }
        if (StringUtils.isNotBlank(xid)) {
            sql += SqlConstant.PARAM_XID_SQL;
            params.add(xid);
        }
        if (logStatus != null) {
            sql += SqlConstant.UNDO_LOG_STATUS_SQL;
            params.add(logStatus);
        }
        if (logCreateTime != null) {
            String startTime = logCreateTime.getStartTime();
            String endTime = logCreateTime.getEndTime();
            if (startTime != null && endTime != null) {
                sql += SqlConstant.UNDO_LOG_CREATE_TIME_SQL;
                Long startTimestamp = LocalDateTime.parse(startTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                Long endTimestamp = LocalDateTime.parse(endTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                if (endTimestamp - startTimestamp > max_time_duration) {
                    throw new StoreException("The query time span is not allowed to exceed the max query duration");
                }
            }
            if (startTime != null) {
                params.add(startTime);
            }
            if (endTime != null) {
                params.add(endTime);
            }
        }
        if (logModifiedTime != null) {
            String startTime = logModifiedTime.getStartTime();
            String endTime = logModifiedTime.getEndTime();
            if (startTime != null && endTime != null) {
                sql += SqlConstant.UNDO_LOG_MODIFY_TIME_SQL;
                Long startTimestamp = LocalDateTime.parse(startTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                Long endTimestamp = LocalDateTime.parse(endTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                if (endTimestamp - startTimestamp > max_time_duration) {
                    throw new StoreException("The query time span is not allowed to exceed the max query duration");
                }
            }
            if (startTime != null) {
                params.add(startTime);
            }
            if (endTime != null) {
                params.add(endTime);
            }
        }
        List<byte[]> result = new ArrayList<>();
        List<Map<String, Object>> query = sqlExecutionTemplate.query(resourceId, sql, params.toArray());
        for (Map<String, Object> map : query) {
            Object rollbackInfo = map.get("rollback_info");
            if (rollbackInfo != null) {
                result.add((byte[]) rollbackInfo);
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
