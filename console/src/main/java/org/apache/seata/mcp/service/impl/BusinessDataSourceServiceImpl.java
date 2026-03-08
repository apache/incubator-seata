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
import org.apache.seata.mcp.core.constant.SqlConstant;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BusinessDataSourceServiceImpl implements BusinessDataSourceService {

    private final SqlExecutionTemplate sqlExecutionTemplate;

    public BusinessDataSourceServiceImpl(SqlExecutionTemplate sqlExecutionTemplate) {
        this.sqlExecutionTemplate = sqlExecutionTemplate;
    }

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
