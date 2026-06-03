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

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.apache.seata.common.exception.StoreException;
import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class DataSourceFactory {

    private static final Map<String, DataSource> dataSourceMap = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceFactory.class);

    @PostConstruct
    public void init() {
        DataSourceFactory.initAllDataSources();
    }

    @PreDestroy
    public void destroy() {
        dataSourceMap.forEach(DataSourceFactory::closeDataSource);
        dataSourceMap.clear();
    }

    public static void initAllDataSources() {
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                BusinessDataSourcesProperties.getDatasources();
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

    public static void removeErrorDataSource(String resourceId, Exception e) {
        closeDataSource(resourceId, dataSourceMap.remove(resourceId));
        LOGGER.info("Delete Business DataSource, resourceId: {}", resourceId);
        throw new StoreException("The Business DataSource: " + resourceId + " can't be connected");
    }

    public static void removeDataSource(String resourceId) {
        closeDataSource(resourceId, dataSourceMap.remove(resourceId));
        LOGGER.info("Delete Business DataSource, resourceId: {}", resourceId);
    }

    private static void closeDataSource(String resourceId, DataSource dataSource) {
        if (dataSource instanceof AutoCloseable) {
            try {
                ((AutoCloseable) dataSource).close();
            } catch (Exception e) {
                LOGGER.warn("Close Business DataSource failed, resourceId: {}", resourceId, e);
            }
        }
    }

    public static DataSource createDataSource(
            BusinessDataSourcesProperties.DataSourceProperties dataSourceProperties, String resourceId) {
        if (dataSourceProperties == null) {
            throw new StoreException("Cannot find datasource properties:" + resourceId);
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
