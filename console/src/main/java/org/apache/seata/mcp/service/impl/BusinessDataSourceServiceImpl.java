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
import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.apache.seata.mcp.entity.vo.MysqlColumnInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceTestResult;
import org.apache.seata.mcp.entity.vo.MysqlTableInfo;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.apache.seata.mcp.service.MysqlMetadataService;
import org.apache.seata.mcp.store.DataSourceFactory;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class BusinessDataSourceServiceImpl implements BusinessDataSourceService {

    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z0-9_$]+");

    private final SqlExecutionTemplate sqlExecutionTemplate;

    private final BusinessDataSourcesProperties businessDataSourcesProperties;

    private final MysqlMetadataService mysqlMetadataService;

    public BusinessDataSourceServiceImpl(
            SqlExecutionTemplate sqlExecutionTemplate,
            BusinessDataSourcesProperties businessDataSourcesProperties,
            MysqlMetadataService mysqlMetadataService) {
        this.sqlExecutionTemplate = sqlExecutionTemplate;
        this.businessDataSourcesProperties = businessDataSourcesProperties;
        this.mysqlMetadataService = mysqlMetadataService;
    }

    @Override
    public List<MysqlDataSourceInfo> getMysqlDataSources() {
        return businessDataSourcesProperties.getMysqlDataSourceInfos();
    }

    @Override
    public String registerMysqlDataSource(MysqlDataSourceRegisterRequest request) {
        return businessDataSourcesProperties.registerMysqlDataSource(request);
    }

    @Override
    public String unregisterMysqlDataSource(String name) {
        String resourceId = businessDataSourcesProperties.unregisterMysqlDataSource(name);
        DataSourceFactory.removeDataSource(resourceId);
        return resourceId;
    }

    @Override
    public MysqlDataSourceTestResult testMysqlDataSource(MysqlDataSourceRegisterRequest request) {
        long start = System.currentTimeMillis();
        MysqlDataSourceTestResult result = new MysqlDataSourceTestResult();
        result.setValidationQuery(SqlConstant.MYSQL_VALIDATION_SQL);
        try {
            BusinessDataSourcesProperties.DataSourceProperties props =
                    businessDataSourcesProperties.buildDynamicMysqlProperties(request);
            Class.forName(props.getDriverClassName());
            try (Connection connection =
                            DriverManager.getConnection(props.getUrl(), props.getUsername(), props.getPassword());
                    PreparedStatement statement = connection.prepareStatement(SqlConstant.MYSQL_VALIDATION_SQL)) {
                statement.setQueryTimeout(5);
                statement.executeQuery();
            }
            result.setSuccess(true);
            result.setMessage("OK");
        } catch (Exception e) {
            result.setSuccess(false);
            result.setMessage("Connection test failed");
        }
        result.setElapsedMs(System.currentTimeMillis() - start);
        return result;
    }

    @Override
    public List<String> listMysqlSchemas(String resourceId) {
        return mysqlMetadataService.listSchemas(resourceId);
    }

    @Override
    public List<MysqlTableInfo> getMysqlTableNames(String resourceId, String schemaName) {
        return mysqlMetadataService.listTables(resourceId, schemaName);
    }

    @Override
    public List<MysqlColumnInfo> getMysqlTableSchema(String resourceId, String schemaName, String tableName) {
        return mysqlMetadataService.describeTable(resourceId, schemaName, tableName);
    }

    @Override
    public BusinessQueryResult runSql(String sql, String resourceId) {
        return sqlExecutionTemplate.query(resourceId, sql);
    }

    @Override
    public BusinessQueryResult queryMysqlTable(
            String resourceId,
            String schemaName,
            String tableName,
            List<String> columns,
            Map<String, Object> filters,
            Integer limit) {
        validateAllowedSchema(resourceId, schemaName);
        validateIdentifier("schemaName", schemaName);
        validateIdentifier("tableName", tableName);

        StringBuilder sql = new StringBuilder("SELECT ");
        if (columns == null || columns.isEmpty()) {
            sql.append("*");
        } else {
            sql.append(buildColumnList(columns));
        }
        sql.append(" FROM ").append(quote(schemaName)).append(".").append(quote(tableName));

        List<Object> params = new ArrayList<>();
        if (filters != null && !filters.isEmpty()) {
            sql.append(" WHERE ");
            boolean first = true;
            for (Map.Entry<String, Object> entry : filters.entrySet()) {
                validateIdentifier("filter column", entry.getKey());
                if (!first) {
                    sql.append(" AND ");
                }
                sql.append(quote(entry.getKey())).append(" = ?");
                params.add(entry.getValue());
                first = false;
            }
        }

        int maxRows = limit == null || limit <= 0 ? Integer.MAX_VALUE : limit;
        return sqlExecutionTemplate.queryWithMaxRows(resourceId, sql.toString(), maxRows, params.toArray());
    }

    @Override
    public BusinessQueryResult explainMysqlSql(String resourceId, String sql) {
        return mysqlMetadataService.explainSql(resourceId, sql);
    }

    private void validateAllowedSchema(String resourceId, String schemaName) {
        if (!businessDataSourcesProperties.isAllowedSchema(resourceId, schemaName)) {
            throw new StoreException("schemaName is not allowed: " + schemaName);
        }
    }

    private String buildColumnList(List<String> columns) {
        StringBuilder builder = new StringBuilder();
        for (String column : columns) {
            validateIdentifier("column", column);
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(quote(column));
        }
        return builder.toString();
    }

    private void validateIdentifier(String field, String value) {
        if (!StringUtils.hasText(value) || !IDENTIFIER_PATTERN.matcher(value).matches()) {
            throw new StoreException(field + " contains unsupported characters");
        }
    }

    private String quote(String identifier) {
        return "`" + identifier + "`";
    }
}
