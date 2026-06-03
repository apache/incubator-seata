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
package org.apache.seata.mcp.service;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.core.constant.SqlConstant;
import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.apache.seata.mcp.entity.vo.MysqlColumnInfo;
import org.apache.seata.mcp.entity.vo.MysqlTableInfo;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.apache.seata.mcp.store.SqlSafetyValidator;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class MysqlMetadataService {

    private final SqlExecutionTemplate sqlExecutionTemplate;

    private final SqlSafetyValidator sqlSafetyValidator;

    private final BusinessDataSourcesProperties businessDataSourcesProperties;

    public MysqlMetadataService(
            SqlExecutionTemplate sqlExecutionTemplate,
            SqlSafetyValidator sqlSafetyValidator,
            BusinessDataSourcesProperties businessDataSourcesProperties) {
        this.sqlExecutionTemplate = sqlExecutionTemplate;
        this.sqlSafetyValidator = sqlSafetyValidator;
        this.businessDataSourcesProperties = businessDataSourcesProperties;
    }

    public List<String> listSchemas(String resourceId) {
        return sqlExecutionTemplate.trustedQuery(resourceId, SqlConstant.LIST_SCHEMA_SQL).getRows().stream()
                .map(row -> String.valueOf(row.get("SCHEMA_NAME")))
                .collect(Collectors.toList());
    }

    public List<MysqlTableInfo> listTables(String resourceId, String schemaName) {
        validateAllowedSchema(resourceId, schemaName);
        return sqlExecutionTemplate
                .trustedQuery(resourceId, SqlConstant.GET_TABLE_NAME_SQL, schemaName)
                .getRows()
                .stream()
                .map(this::toTableInfo)
                .collect(Collectors.toList());
    }

    public List<MysqlColumnInfo> describeTable(String resourceId, String schemaName, String tableName) {
        validateAllowedSchema(resourceId, schemaName);
        if (!StringUtils.hasText(tableName)) {
            throw new StoreException("tableName cannot be empty");
        }
        return sqlExecutionTemplate
                .trustedQuery(resourceId, SqlConstant.GET_SCHEMA_SQL, schemaName, tableName)
                .getRows()
                .stream()
                .map(this::toColumnInfo)
                .collect(Collectors.toList());
    }

    public BusinessQueryResult explainSql(String resourceId, String sql) {
        sqlSafetyValidator.validateMysqlSelect(sql);
        return sqlExecutionTemplate.trustedQuery(resourceId, SqlConstant.MYSQL_EXPLAIN_PREFIX + sql);
    }

    private void validateAllowedSchema(String resourceId, String schemaName) {
        if (!StringUtils.hasText(schemaName)) {
            throw new StoreException("schemaName cannot be empty");
        }
        if (!businessDataSourcesProperties.isAllowedSchema(resourceId, schemaName)) {
            throw new StoreException("schemaName is not allowed: " + schemaName);
        }
    }

    private MysqlTableInfo toTableInfo(Map<String, Object> row) {
        MysqlTableInfo info = new MysqlTableInfo();
        info.setTableName(String.valueOf(row.get("TABLE_NAME")));
        Object comment = row.get("TABLE_COMMENT");
        info.setTableComment(comment == null ? "" : String.valueOf(comment));
        return info;
    }

    private MysqlColumnInfo toColumnInfo(Map<String, Object> row) {
        MysqlColumnInfo info = new MysqlColumnInfo();
        info.setColumnName(String.valueOf(row.get("COLUMN_NAME")));
        info.setDataType(String.valueOf(row.get("DATA_TYPE")));
        Object comment = row.get("COLUMN_COMMENT");
        info.setColumnComment(comment == null ? "" : String.valueOf(comment));
        return info;
    }
}
