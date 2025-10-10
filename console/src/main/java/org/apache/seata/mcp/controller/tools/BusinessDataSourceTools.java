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
package org.apache.seata.mcp.controller.tools;

import org.apache.seata.mcp.annotation.Tool;
import org.apache.seata.mcp.annotation.ToolParam;
import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class BusinessDataSourceTools {

    @Autowired
    private BusinessDataSourceService dataSourceService;

    private static final Logger LOGGER = LoggerFactory.getLogger(BusinessDataSourceTools.class);

    @Tool(
            description =
                    "Get the identity and name of the business data source. Important!!!: key is name, value is resourceId")
    public Map<String, String> getResourceIds() {
        LOGGER.info("User try to get resource ids");
        return BusinessDataSourcesProperties.getDataSourcesNamesAndResourceIds();
    }

    @Tool(description = "Get all available table names")
    public List<String> getTableNames(
            @ToolParam(description = "The identity of the data source, start with jdbc://", required = true)
                    String resourceId) {
        LOGGER.info("User try to get all table names, resource id {}", resourceId);
        return dataSourceService.getTableNamesBySchema(resourceId);
    }

    @Tool(description = "Obtained by table nameSchema")
    public List<Map<String, Object>> getTableSchema(
            @ToolParam(description = "Table Name", required = true) String tableName,
            @ToolParam(description = "The identity of the data source, start with jdbc://", required = true)
                    String resourceId) {
        LOGGER.info("User try to get table schema, tableName: {}, resourceId: {}", tableName, resourceId);
        return dataSourceService.getTableSchemaByTableName(resourceId, tableName);
    }

    @Tool(description = "Execute the SQL query result, It can only be used to query business data!!!")
    public List<Map<String, Object>> runSql(
            @ToolParam(description = "SQL statement, String type", required = true) String sql,
            @ToolParam(description = "The identity of the data source, start with jdbc://", required = true)
                    String resourceId) {
        LOGGER.info("User try to run sql: {}, resourceId: {}", sql, resourceId);
        List<Map<String, Object>> result = dataSourceService.runSql(sql, resourceId);
        result.add(
                Collections.singletonMap(
                        "Important!!!",
                        "If it is related to data analysis, statistics, etc., Please generate a table and attach an analysis statement to the user for viewing"));
        return result;
    }
}
