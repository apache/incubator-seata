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
import org.apache.seata.common.result.PageResult;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.entity.constant.SqlConstant;
import org.apache.seata.mcp.entity.param.UndoLogParam;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.apache.seata.mcp.entity.vo.UndoLogVO;
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
        if (sql.contains("undo_log")) {
            throw new StoreException(
                    "If you do not use SQL to query undo_log data, use analyzeUndoLog to query and analyze undo_log");
        }
        return sqlExecutionTemplate.query(resourceId, sql);
    }

    @Override
    public PageResult<UndoLogVO> getUndoLogInfo(UndoLogParam param) {
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
        int pageNum = param.getPageNum();
        int pageSize = param.getPageSize();
        int offset = getOffsetAndValidationPageQuerySql(pageNum, pageSize);
        if (StringUtils.isBlank(resourceId)) {
            throw new StoreException("you cannot query without resourceId");
        }
        int paramCounts = 0;
        if (StringUtils.isNotBlank(branchId)) {
            sql += SqlConstant.PARAM_BRANCH_ID_SQL;
            params.add(branchId);
            paramCounts++;
        }
        if (StringUtils.isNotBlank(xid)) {
            sql += SqlConstant.PARAM_XID_SQL;
            params.add(xid);
            paramCounts++;
        }
        if (logStatus != null) {
            sql += SqlConstant.UNDO_LOG_STATUS_SQL;
            params.add(logStatus);
            paramCounts++;
        }
        boolean containsTimeDuration = false;
        if (logCreateTime != null) {
            String startTime = logCreateTime.getStartTime();
            String endTime = logCreateTime.getEndTime();
            if (startTime != null && endTime != null) {
                sql += SqlConstant.UNDO_LOG_CREATE_TIME_SQL;
                containsTimeDuration = true;
                Long startTimestamp = LocalDateTime.parse(startTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                Long endTimestamp = LocalDateTime.parse(endTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                if (endTimestamp - startTimestamp > max_time_duration) {
                    throw new StoreException(
                            "The query time span is not allowed to exceed the max query duration(milliseconds): "
                                    + max_time_duration);
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
                if (containsTimeDuration) {
                    sql += " AND" + SqlConstant.UNDO_LOG_MODIFY_TIME_SQL;
                } else {
                    sql += SqlConstant.UNDO_LOG_MODIFY_TIME_SQL;
                    containsTimeDuration = true;
                }
                Long startTimestamp = LocalDateTime.parse(startTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                Long endTimestamp = LocalDateTime.parse(endTime, formatter)
                        .atZone(ZoneId.systemDefault())
                        .toInstant()
                        .toEpochMilli();
                if (endTimestamp - startTimestamp > max_time_duration) {
                    throw new StoreException(
                            "The query time span is not allowed to exceed the max query duration(milliseconds): "
                                    + max_time_duration);
                }
            }
            if (startTime != null) {
                params.add(startTime);
            }
            if (endTime != null) {
                params.add(endTime);
            }
        }
        if (containsTimeDuration) {
            for (int i = 0; i < paramCounts; i++) {
                sql = sql.replaceFirst("#", "AND");
            }
        } else {
            for (int i = 1; i < paramCounts; i++) {
                sql = sql.replaceFirst("#", "AND");
            }
        }
        sql = sql.replaceAll("#", "");
        sql += SqlConstant.UNDO_LOG_ORDER + SqlConstant.PAGE_QUERY;
        sql = sql.replaceFirst("%", String.valueOf(pageSize));
        sql = sql.replaceFirst("%", String.valueOf(offset));
        Object[] objects = params.toArray();
        return sqlExecutionTemplate.queryForUndoLogs(resourceId, sql, pageNum, pageSize, objects);
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

    public int getOffsetAndValidationPageQuerySql(int pageNum, int pageSize) {
        int offset = (pageNum - 1) * pageSize;
        if (pageNum < 1) {
            throw new IllegalArgumentException("The page number must be greater than 0");
        }
        if (pageSize < 0) {
            throw new IllegalArgumentException("The page size must be greater than 0");
        }
        if (pageSize > SqlConstant.MAX_PAGE_SIZE) {
            throw new IllegalArgumentException("Exceeding the maximum pageSize: " + SqlConstant.MAX_PAGE_SIZE);
        }
        if (offset > SqlConstant.MAX_OFFSET_THRESHOLD) {
            throw new StoreException("Exceeding the maximum offset: " + SqlConstant.MAX_OFFSET_THRESHOLD);
        }
        return offset;
    }
}
