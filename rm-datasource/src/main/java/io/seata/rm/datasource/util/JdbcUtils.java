/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.rm.datasource.util;

import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.rm.BaseDataSourceResource;
import io.seata.rm.DefaultResourceManager;
import io.seata.rm.datasource.xa.Holdable;
import io.seata.sqlparser.SqlParserType;
import io.seata.sqlparser.util.DbTypeParser;
import io.seata.sqlparser.util.JdbcConstants;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * @author ggndnn
 * @author sharajava
 */
public final class JdbcUtils {

    private static volatile DbTypeParser dbTypeParser;

    private JdbcUtils() {
    }

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

    /**
     * Get database type by JDBC connection URL via {@link DbTypeParser#parseFromJdbcUrl}.
     *
     * @param jdbcUrl JDBC database connection URL
     * @return the database type
     */
    public static String getDbType(String jdbcUrl) {
        return getDbTypeParser().parseFromJdbcUrl(jdbcUrl).toLowerCase();
    }

    /**
     * Converting the database type to compatible one.
     * <ol><li> For OceanBase:
     * Detect the compatibility mode of OceanBase based on the physical database name or validation query.
     * <ul><li>For oceanbase-client 1.x: JDBC url starting with 'jdbc:oceanbase:' indicates
     * that the running mode is MYSQL, while one starting with 'jdbc:oceanbase:oracle:' indicates ORACLE mode.</li>
     * <li>For oceanbase-client 2.x: The format of the JDBC url is 'jdbc:oceanbase[:oracle][:hamode]:',
     * where oracle is version-compatible options and has no practical effect.</li>
     * <li> OceanBase in MySQL mode is directly delegated to MySQL.
     * In druid, the SQLStatementParser for generic SQL statements is returned when db type is OCEANBASE,
     * not specified for MySQL.</li>
     * <li> Note: db type parser of druid recognizes it by url prefix (only adapted to old version driver).</li></ul>
     * <li> For Mariadb: Be delegated to MySQL.</li></ol>
     *  @param rawDbType raw database type
     *  @param conn database connection for further determination of type
     *  @return the compatible database type
     */
    public static String convertCompatibleDbType(String rawDbType, final Connection conn) throws SQLException {
        if (JdbcConstants.OCEANBASE.equals(rawDbType) || JdbcConstants.OCEANBASE_ORACLE.equals(rawDbType)) {
            String databaseName = conn.getMetaData().getDatabaseProductName().toLowerCase();
            if (JdbcConstants.MYSQL.contains(databaseName)) {
                return JdbcConstants.MYSQL;
            } else if (JdbcConstants.ORACLE.contains(databaseName)) {
                return JdbcConstants.OCEANBASE_ORACLE;
            } else {
                try (Statement statement = conn.createStatement();
                     ResultSet rs = statement.executeQuery("SELECT * FROM DUAL")) {
                    if (!rs.next()) {
                        throw new SQLException("Validation query for OceanBase(Oracle mode) didn't return a row");
                    }
                    return JdbcConstants.OCEANBASE_ORACLE;
                } catch (SQLException e) {
                    return JdbcConstants.MYSQL;
                }
            }
        } else if (JdbcConstants.MARIADB.equals(rawDbType)) {
            return JdbcConstants.MYSQL;
        }
        return rawDbType;
    }


    public static <T extends Holdable> void initDataSourceResource(BaseDataSourceResource<T> dataSourceResource,
                                                                   DataSource dataSource, String resourceGroupId) {
        dataSourceResource.setResourceGroupId(resourceGroupId);
        try (Connection connection = dataSource.getConnection()) {
            initDataResource(dataSourceResource, connection);
        } catch (SQLException e) {
            throw new IllegalStateException("can not init DataSourceResource with " + dataSource, e);
        }
        DefaultResourceManager.get().registerResource(dataSourceResource);
    }

    public static <T extends Holdable> void initXADataSourceResource(BaseDataSourceResource<T> dataSourceResource,
                                                                     XADataSource dataSource, String resourceGroupId) {
        dataSourceResource.setResourceGroupId(resourceGroupId);
        try {
            XAConnection xaConnection = dataSource.getXAConnection();
            try (Connection connection = xaConnection.getConnection()) {
                initDataResource(dataSourceResource, connection);
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

    private static <T extends Holdable> void initDataResource(BaseDataSourceResource<T> dataSourceResource,
                                                              Connection connection) throws SQLException {
        String jdbcUrl = connection.getMetaData().getURL();
        dataSourceResource.setResourceId(buildResourceId(jdbcUrl));
        String driverClassName = com.alibaba.druid.util.JdbcUtils.getDriverClassName(jdbcUrl);
        dataSourceResource.setDriver(loadDriver(driverClassName));
        dataSourceResource.setDbType(com.alibaba.druid.util.JdbcUtils.getDbType(jdbcUrl, driverClassName));
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
            return (Driver) clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }
}
