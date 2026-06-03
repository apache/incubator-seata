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
package org.apache.seata.mcp.tools;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.apache.seata.mcp.entity.vo.MysqlColumnInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceTestResult;
import org.apache.seata.mcp.entity.vo.MysqlTableInfo;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springaicommunity.mcp.annotation.McpResource;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
public class BusinessDataSourceTools {

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessDataSourceTools.class);

    private final BusinessDataSourceService dataSourceService;

    private final ObjectMapper objectMapper;

    public BusinessDataSourceTools(BusinessDataSourceService dataSourceService, ObjectMapper objectMapper) {
        this.dataSourceService = dataSourceService;
        this.objectMapper = objectMapper;
    }

    @McpTool(description = "Register a dynamic MySQL business data source. Admin only.")
    public String registerMysqlDataSource(
            @McpToolParam(description = "MySQL data source registration request", required = true)
                    MysqlDataSourceRegisterRequest request) {
        requireAdmin();
        LOGGER.info("User tries to register MySQL business data source");
        return dataSourceService.registerMysqlDataSource(request);
    }

    @McpTool(description = "Unregister a dynamic MySQL business data source. Admin only.")
    public String unregisterMysqlDataSource(
            @McpToolParam(description = "The data source name", required = true) String name) {
        requireAdmin();
        LOGGER.info("User tries to unregister MySQL business data source: {}", name);
        return dataSourceService.unregisterMysqlDataSource(name);
    }

    @McpTool(description = "Test a MySQL business data source registration request. Admin only.")
    public MysqlDataSourceTestResult testMysqlDataSource(
            @McpToolParam(description = "MySQL data source registration request", required = true)
                    MysqlDataSourceRegisterRequest request) {
        requireAdmin();
        LOGGER.info("User tries to test MySQL business data source");
        return dataSourceService.testMysqlDataSource(request);
    }

    @McpTool(description = "Get all MySQL business data sources without sensitive fields")
    public List<MysqlDataSourceInfo> getMysqlDataSources() {
        LOGGER.info("User tries to get MySQL business data sources");
        return dataSourceService.getMysqlDataSources();
    }

    @McpTool(description = "List MySQL schemas in a business data source")
    public List<String> listMysqlSchemas(
            @McpToolParam(
                            description = "The identity of the data source, for example business-ds://biz",
                            required = true)
                    String resourceId) {
        LOGGER.info("User tries to list MySQL schemas, resourceId: {}", resourceId);
        return dataSourceService.listMysqlSchemas(resourceId);
    }

    @McpTool(description = "Get MySQL table names in a schema")
    public List<MysqlTableInfo> getMysqlTableNames(
            @McpToolParam(
                            description = "The identity of the data source, for example business-ds://biz",
                            required = true)
                    String resourceId,
            @McpToolParam(description = "MySQL schema name", required = true) String schemaName) {
        LOGGER.info("User tries to get MySQL table names, resourceId: {}, schemaName: {}", resourceId, schemaName);
        return dataSourceService.getMysqlTableNames(resourceId, schemaName);
    }

    @McpTool(description = "Get MySQL table columns in a schema")
    public List<MysqlColumnInfo> getMysqlTableSchema(
            @McpToolParam(
                            description = "The identity of the data source, for example business-ds://biz",
                            required = true)
                    String resourceId,
            @McpToolParam(description = "MySQL schema name", required = true) String schemaName,
            @McpToolParam(description = "MySQL table name", required = true) String tableName) {
        LOGGER.info(
                "User tries to get MySQL table schema, resourceId: {}, schemaName: {}, tableName: {}",
                resourceId,
                schemaName,
                tableName);
        return dataSourceService.getMysqlTableSchema(resourceId, schemaName, tableName);
    }

    @McpTool(description = "Query a MySQL table with optional column list, equality filters, and row limit")
    public BusinessQueryResult queryMysqlTable(
            @McpToolParam(
                            description = "The identity of the data source, for example business-ds://biz",
                            required = true)
                    String resourceId,
            @McpToolParam(description = "MySQL schema name", required = true) String schemaName,
            @McpToolParam(description = "MySQL table name", required = true) String tableName,
            @McpToolParam(description = "Column names to select", required = false) List<String> columns,
            @McpToolParam(description = "Equality filters keyed by column name", required = false)
                    Map<String, Object> filters,
            @McpToolParam(description = "Maximum rows to return", required = false) Integer limit) {
        LOGGER.info(
                "User tries to query MySQL table, resourceId: {}, schemaName: {}, tableName: {}",
                resourceId,
                schemaName,
                tableName);
        return dataSourceService.queryMysqlTable(resourceId, schemaName, tableName, columns, filters, limit);
    }

    @McpTool(description = "Explain a safe MySQL SELECT SQL statement")
    public BusinessQueryResult explainMysqlSql(
            @McpToolParam(
                            description = "The identity of the data source, for example business-ds://biz",
                            required = true)
                    String resourceId,
            @McpToolParam(description = "MySQL SELECT SQL statement", required = true) String sql) {
        LOGGER.info("User tries to explain MySQL sql, resourceId: {}", resourceId);
        return dataSourceService.explainMysqlSql(resourceId, sql);
    }

    @McpTool(description = "Execute a safe MySQL SELECT query against a business data source")
    public BusinessQueryResult runSql(
            @McpToolParam(description = "MySQL SELECT SQL statement", required = true) String sql,
            @McpToolParam(
                            description = "The identity of the data source, for example business-ds://biz",
                            required = true)
                    String resourceId) {
        LOGGER.info("User tries to run MySQL sql, resourceId: {}", resourceId);
        return dataSourceService.runSql(sql, resourceId);
    }

    @McpResource(
            name = "mysqlDataSources",
            title = "MySQL business data sources",
            uri = "mysql-db://datasources",
            description = "MySQL business data source list without sensitive fields",
            mimeType = "application/json")
    public String mysqlDataSourcesResource() {
        return toJson(dataSourceService.getMysqlDataSources());
    }

    @McpResource(
            name = "mysqlSchemas",
            title = "MySQL schemas",
            uri = "mysql-db://{resourceId}/schemas",
            description = "MySQL schema list for a business data source",
            mimeType = "application/json")
    public String mysqlSchemasResource(String resourceId) {
        return toJson(dataSourceService.listMysqlSchemas(resourceId));
    }

    @McpResource(
            name = "mysqlTables",
            title = "MySQL tables",
            uri = "mysql-db://{resourceId}/{schemaName}/tables",
            description = "MySQL table list for a business schema",
            mimeType = "application/json")
    public String mysqlTablesResource(String resourceId, String schemaName) {
        return toJson(dataSourceService.getMysqlTableNames(resourceId, schemaName));
    }

    @McpResource(
            name = "mysqlTableSchema",
            title = "MySQL table schema",
            uri = "mysql-db://{resourceId}/{schemaName}/{tableName}/schema",
            description = "MySQL column list for a business table",
            mimeType = "application/json")
    public String mysqlTableSchemaResource(String resourceId, String schemaName, String tableName) {
        return toJson(dataSourceService.getMysqlTableSchema(resourceId, schemaName, tableName));
    }

    private void requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            throw new AccessDeniedException("Admin authority is required");
        }
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        if (authorities == null || authorities.isEmpty()) {
            return;
        }
        boolean admin = authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ADMIN".equals(authority) || "ROLE_ADMIN".equals(authority));
        if (!admin) {
            throw new AccessDeniedException("Admin authority is required");
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new StoreException("Unable to serialize MCP resource");
        }
    }
}
