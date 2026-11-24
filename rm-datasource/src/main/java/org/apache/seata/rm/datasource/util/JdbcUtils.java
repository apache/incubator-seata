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
import org.apache.seata.common.lock.ResourceLock;
import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.rm.DefaultResourceManager;
import org.apache.seata.sqlparser.SqlParserType;
import org.apache.seata.sqlparser.util.DbTypeParser;

import javax.sql.DataSource;
import javax.sql.XAConnection;
import javax.sql.XADataSource;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

public final class JdbcUtils {

    private static volatile DbTypeParser dbTypeParser;
    private static final ResourceLock RESOURCE_LOCK = new ResourceLock();

    static DbTypeParser getDbTypeParser() {
        if (dbTypeParser == null) {
            try (ResourceLock ignored = RESOURCE_LOCK.obtain()) {
                if (dbTypeParser == null) {
                    dbTypeParser = EnhancedServiceLoader.load(DbTypeParser.class, SqlParserType.SQL_PARSER_TYPE_DRUID);
                }
            }
        }
        return dbTypeParser;
    }

    private JdbcUtils() {}

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
    public static void initDataSourceResource(
            BaseDataSourceResource dataSourceResource, DataSource dataSource, String resourceGroupId) {
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

    public static void initXADataSourceResource(
            BaseDataSourceResource dataSourceResource, XADataSource dataSource, String resourceGroupId) {
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
            return (Driver) clazz.newInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new SQLException(e.getMessage(), e);
        }
    }

    /**
     * Set a parameter with the given JDBC type, widening the integer type when the value does not fit it.
     * <p>
     * MySQL reports unsigned integer columns as {@link Types#TINYINT}, {@link Types#SMALLINT},
     * {@link Types#INTEGER} or {@link Types#BIGINT}. A value above that signed range is read as {@link Byte},
     * {@link Short}, {@link Integer}, {@link Long} or {@link BigInteger}. Passing the column type to
     * {@code setObject} makes the driver narrow it, so {@code 4294967295} is sent as {@code -1}. The type is
     * widened to the narrowest of {@code SMALLINT}, {@code INTEGER}, {@code BIGINT} and {@code DECIMAL} that can
     * hold the value.
     *
     * @param ps the prepared statement
     * @param parameterIndex the parameter index
     * @param value the parameter value
     * @param targetSqlType the JDBC type of the column
     * @throws SQLException if the parameter cannot be set
     */
    public static void setObject(PreparedStatement ps, int parameterIndex, Object value, int targetSqlType)
            throws SQLException {
        ps.setObject(parameterIndex, value, getTargetSqlType(value, targetSqlType));
    }

    static int getTargetSqlType(Object value, int targetSqlType) {
        int signedBits = signedBits(targetSqlType);
        if (signedBits < 0) {
            return targetSqlType;
        }
        BigInteger number = toBigInteger(value);
        if (number == null) {
            return targetSqlType;
        }
        int bitLength = number.bitLength();
        if (bitLength <= signedBits) {
            return targetSqlType;
        }
        if (bitLength <= Short.SIZE - 1) {
            return Types.SMALLINT;
        }
        if (bitLength <= Integer.SIZE - 1) {
            return Types.INTEGER;
        }
        if (bitLength <= Long.SIZE - 1) {
            return Types.BIGINT;
        }
        return Types.DECIMAL;
    }

    /**
     * Signed magnitude of the JDBC integer type, excluding the sign bit. Negative when the type is not an integer.
     */
    private static int signedBits(int targetSqlType) {
        switch (targetSqlType) {
            case Types.TINYINT:
                return Byte.SIZE - 1;
            case Types.SMALLINT:
                return Short.SIZE - 1;
            case Types.INTEGER:
                return Integer.SIZE - 1;
            case Types.BIGINT:
                return Long.SIZE - 1;
            default:
                return -1;
        }
    }

    private static BigInteger toBigInteger(Object value) {
        if (value instanceof BigInteger) {
            return (BigInteger) value;
        }
        if (value instanceof Long) {
            return BigInteger.valueOf((Long) value);
        }
        if (value instanceof Integer) {
            return BigInteger.valueOf((Integer) value);
        }
        if (value instanceof Short) {
            return BigInteger.valueOf((Short) value);
        }
        if (value instanceof Byte) {
            return BigInteger.valueOf((Byte) value);
        }
        return null;
    }
}
