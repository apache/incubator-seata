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

import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.apache.seata.mcp.entity.vo.MysqlColumnInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceTestResult;
import org.apache.seata.mcp.entity.vo.MysqlTableInfo;

import java.util.List;
import java.util.Map;

public interface BusinessDataSourceService {
    List<MysqlDataSourceInfo> getMysqlDataSources();

    String registerMysqlDataSource(MysqlDataSourceRegisterRequest request);

    String unregisterMysqlDataSource(String name);

    MysqlDataSourceTestResult testMysqlDataSource(MysqlDataSourceRegisterRequest request);

    List<String> listMysqlSchemas(String resourceId);

    List<MysqlTableInfo> getMysqlTableNames(String resourceId, String schemaName);

    List<MysqlColumnInfo> getMysqlTableSchema(String resourceId, String schemaName, String tableName);

    BusinessQueryResult runSql(String sql, String resourceId);

    BusinessQueryResult queryMysqlTable(
            String resourceId,
            String schemaName,
            String tableName,
            List<String> columns,
            Map<String, Object> filters,
            Integer limit);

    BusinessQueryResult explainMysqlSql(String resourceId, String sql);
}
