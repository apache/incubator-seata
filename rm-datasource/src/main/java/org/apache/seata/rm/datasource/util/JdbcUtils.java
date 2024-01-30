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
package org.apache.seata.rm.datasource.util;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.util.DbTypeParser;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;


public final class JdbcUtils {

    private static volatile DbTypeParser dbTypeParser;

    static DbTypeParser getDbTypeParser() {
        if (dbTypeParser == null) {
            synchronized (JdbcUtils.class) {
                if (dbTypeParser == null) {
                    dbTypeParser = EnhancedServiceLoader.load(DbTypeParser.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
                }
            }
        }
        return dbTypeParser;
    }

    private JdbcUtils() {
    }

    public static String getDbType(String jdbcUrl) {
        return getDbTypeParser().parseFromJdbcUrl(jdbcUrl).toLowerCase();
    }

    /**
     * Init a DataSourceResource instance with DataSource instance and given resource group ID.
     *
     * @param dataSourceResource the DataSourceResource instance
     * @param dataSource the DataSource instance
     * @param resourceGroupId the given resource group ID
     */
    public static void initDataSourceResource(BaseDataSourceResource dataSourceResource, DataSource dataSource, String resourceGroupId) {
        dataSourceResource.setResourceGroupId(resourceGroupId);
        try (Connection connection = dataSource.getConnection()) {
            String jdbcUrl = connection.getMetaData().getURL();
            dataSourceResource.setResourceId(buildResourceId(jdbcUrl));
            String driverClassName = com.alibaba.druid.util.JdbcUtils.getDriverClassName(jdbcUrl);
            dataSourceResource.setDriver(loadDriver(driverClassName));
            dataSourceResource.setDbType(JdbcUtils.getDbType(jdbcUrl));
        } catch (SQLException e) {
            throw new IllegalStateException("can not init DataSourceResource with " + dataSource, e);
        }
        DefaultResourceManager.get().registerResource(dataSourceResource);
    }

    public static void initXADataSourceResource(BaseDataSourceResource dataSourceResource, XADataSource dataSource, String resourceGroupId) {
        dataSourceResource.setResourceGroupId(resourceGroupId);
        try {
            XAConnection xaConnection = dataSource.getXAConnection();
            try (Connection connection = xaConnection.getConnection()) {
                String jdbcUrl = connection.getMetaData().getURL();
                dataSourceResource.setResourceId(buildResourceId(jdbcUrl));
                String driverClassName = com.alibaba.druid.util.JdbcUtils.getDriverClassName(jdbcUrl);
                dataSourceResource.setDriver(loadDriver(driverClassName));
                dataSourceResource.setDbType(JdbcUtils.getDbType(jdbcUrl));
            } finally {
                if (xaConnection != null) {
                    xaConnection.close();
                }
            }
        } catch (SQLException e) {
            throw new IllegalStateException("can not get XAConnection from DataSourceResource with " + dataSource, e);
        }
        DefaultResourceManager.get().registerResource(dataSourceResource);
    }

    public static String buildResourceId(String jdbcUrl) {
        if (jdbcUrl.contains("?")) {
            return jdbcUrl.substring(0, jdbcUrl.indexOf('?'));
        }
        return jdbcUrl;
    }

    public static Driver loadDriver(String driverClassName) throws SQLException {
        Class<?> clazz = null;
        try {
            ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
            if (contextLoader != null) {
                clazz = contextLoader.loadClass(driverClassName);
            }
        } catch (ClassNotFoundException e) {
            // skip
        }

        if (clazz == null) {
            try {
                clazz = Class.forName(driverClassName);
            } catch (ClassNotFoundException e) {
                throw new SQLException(e.getMessage(), e);
            }
        }

        try {
            return (Driver)clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
