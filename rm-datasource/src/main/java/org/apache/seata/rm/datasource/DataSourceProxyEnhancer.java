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
package org.apache.seata.rm.datasource;

import org.apache.seata.core.rpc.netty.DataSourceConnectionPoolCollector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * DataSource proxy enhancer for connection pool monitoring integration.
 * This class provides hooks to register DataSource for monitoring after proxy initialization.
 *
 */
public class DataSourceProxyEnhancer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSourceProxyEnhancer.class);

    /**
     * Register DataSource monitoring after DataSourceProxy initialization
     *
     * @param dataSourceProxy Seata DataSource proxy
     */
    public static void onDataSourceProxyInitialized(DataSourceProxy dataSourceProxy) {
        try {
            // Get the underlying (target) DataSource
            DataSource targetDataSource = dataSourceProxy.getTargetDataSource();
            if (targetDataSource == null) {
                LOGGER.warn("Target DataSource is null in DataSourceProxy");
                return;
            }
            // Check whether the DataSource type is supported
            if (!DataSourceConnectionPoolCollector.isSupportedDataSource(targetDataSource)) {
                LOGGER.debug(
                        "Unsupported DataSource type for monitoring: {}",
                        targetDataSource.getClass().getName());
                return;
            }
            // Generate DataSource name
            String dataSourceName = generateDataSourceName(dataSourceProxy);
            // Register DataSource for monitoring
            DataSourceConnectionPoolCollector.registerDataSource(dataSourceName, targetDataSource);
            LOGGER.info(
                    "Registered DataSource [{}] for connection pool monitoring: {}",
                    dataSourceName,
                    targetDataSource.getClass().getSimpleName());
        } catch (Exception e) {
            LOGGER.warn("Failed to register DataSource for monitoring in DataSourceProxy", e);
        }
    }

    /**
     * Unregister DataSource monitoring when DataSourceProxy is destroyed
     *
     * @param dataSourceProxy Seata DataSource proxy
     */
    public static void onDataSourceProxyDestroyed(DataSourceProxy dataSourceProxy) {
        try {
            String dataSourceName = generateDataSourceName(dataSourceProxy);
            DataSourceConnectionPoolCollector.unregisterDataSource(dataSourceName);
            LOGGER.info("Unregistered DataSource [{}] from connection pool monitoring", dataSourceName);
        } catch (Exception e) {
            LOGGER.warn("Failed to unregister DataSource from monitoring in DataSourceProxy", e);
        }
    }

    /**
     * Generate DataSource name
     */
    private static String generateDataSourceName(DataSourceProxy dataSourceProxy) {
        // Use resource ID as DataSource name to ensure uniqueness
        String resourceId = dataSourceProxy.getResourceId();
        if (resourceId != null && !resourceId.isEmpty()) {
            return "seata-" + resourceId.substring(resourceId.lastIndexOf("/") + 1);
        }
        // Fallback: use object identity hash code
        return "seata-datasource-" + System.identityHashCode(dataSourceProxy);
    }

    /**
     * Manually register a DataSource
     */
    public static void registerDataSource(String name, DataSource dataSource) {
        if (name == null || dataSource == null) {
            throw new IllegalArgumentException("DataSource name and instance cannot be null");
        }
        try {
            DataSourceConnectionPoolCollector.registerDataSource(name, dataSource);
            LOGGER.info("Manually registered DataSource [{}] for monitoring", name);
        } catch (Exception e) {
            LOGGER.error("Failed to manually register DataSource [{}]", name, e);
        }
    }

    /**
     * Manually unregister a DataSource
     */
    public static void unregisterDataSource(String name) {
        if (name == null) {
            return;
        }
        try {
            DataSourceConnectionPoolCollector.unregisterDataSource(name);
            LOGGER.info("Manually unregistered DataSource [{}] from monitoring", name);
        } catch (Exception e) {
            LOGGER.error("Failed to manually unregister DataSource [{}]", name, e);
        }
    }
}
