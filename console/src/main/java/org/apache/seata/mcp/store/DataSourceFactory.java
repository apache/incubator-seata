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
package org.apache.seata.mcp.store;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DataSourceFactory {

    private static final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    public static void initAllDataSources() {
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                BusinessDataSourcesProperties.getDatasources();
        if (datasources == null) return;

        datasources.forEach(
                (resourceId, props) -> dataSourceMap.computeIfAbsent(resourceId, key -> createDataSource(props, key)));
    }

    public static DataSource getDataSource(String resourceId) {
        return dataSourceMap.computeIfAbsent(resourceId, key -> {
            BusinessDataSourcesProperties.DataSourceProperties props =
                    BusinessDataSourcesProperties.getDatasources().get(key);
            if (props == null) {
                throw new StoreException("Cannot find datasource properties: " + key);
            }
            return createDataSource(props, key);
        });
    }

    public static DataSource createDataSource(
            BusinessDataSourcesProperties.DataSourceProperties dataSourceProperties, String resourceId) {
        if (dataSourceProperties == null) {
            throw new StoreException("Cannot find datasource properties:" + dataSourceProperties);
        }

        String type = dataSourceProperties.getDatasource();
        switch (type) {
            case "druid":
                DruidDataSourceProvider druidDataSourceProvider = new DruidDataSourceProvider();
                return druidDataSourceProvider.generateByResourceId(resourceId);
            case "hikari":
                HikariDataSourceProvider hikariDataSourceProvider = new HikariDataSourceProvider();
                return hikariDataSourceProvider.generateByResourceId(resourceId);
            case "dbcp":
                DbcpDataSourceProvider dbcpDataSourceProvider = new DbcpDataSourceProvider();
                return dbcpDataSourceProvider.generateByResourceId(resourceId);
            default:
                throw new StoreException("Unknown datasource type:" + type);
        }
    }
}
