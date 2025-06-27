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

import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.druid.util.MySqlUtils;
import com.alibaba.druid.util.PGUtils;
import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class XAUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XAUtils.class);

    // 达梦数据库特定的类名
    private static final String DM_XA_CONNECTION_CLASS = "dm.jdbc.driver.DmdbXAConnection";
    private static final String DM_CONNECTION_CLASS = "dm.jdbc.driver.DmdbConnection";

    public static String getDbType(String jdbcUrl, String driverClassName) {
        return JdbcUtils.getDbType(jdbcUrl, driverClassName);
    }

    public static XAConnection createXAConnection(Connection physicalConn, BaseDataSourceResource dataSourceResource)
            throws SQLException {
        return createXAConnection(physicalConn, dataSourceResource.getDriver(), dataSourceResource.getDbType());
    }

    /**
     * 创建达梦数据库XA连接
     */
    private static XAConnection createDmXAConnection(Connection physicalConnection) throws SQLException {
        try {
            Class<?> xaConnectionClass = Class.forName(DM_XA_CONNECTION_CLASS);

            // 达梦使用的构造方法参数是DmdbConnection
            Class<?> dmConnectionClass = Class.forName(DM_CONNECTION_CLASS);

            // 确保连接是达梦连接
            if (!dmConnectionClass.isInstance(physicalConnection)) {
                throw new SQLException("Physical connection must be instance of " + DM_CONNECTION_CLASS);
            }
            Constructor<?> constructor = xaConnectionClass.getConstructor(dmConnectionClass);
            return (XAConnection) constructor.newInstance(physicalConnection);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Required DM JDBC driver classes not found. Ensure you're using DM JDBC driver version 8+", e);
        } catch (NoSuchMethodException e) {
            throw new SQLException("DM XAConnection constructor not found", e);
        } catch (Exception e) {
            throw new SQLException("Failed to create DM XA Connection", e);
        }
    }

    public static XAConnection createXAConnection(Connection physicalConn, Driver driver, String dbType)
            throws SQLException {
        if (JdbcConstants.MYSQL.equals(dbType)) {
            return MySqlUtils.createXAConnection(driver, physicalConn);
        } else if (JdbcConstants.DM.equals(dbType)) {
            return createDmXAConnection(physicalConn);
        } else {
            try {
                switch (dbType) {
                    case JdbcConstants.ORACLE:
                        // https://github.com/alibaba/druid/issues/3707
                        // before Druid issue fixed, just make ORACLE XA connection in my way.
                        // return OracleUtils.OracleXAConnection(physicalConn);
                        String physicalConnClassName = physicalConn.getClass().getName();
                        if ("oracle.jdbc.driver.T4CConnection".equals(physicalConnClassName)) {
                            return createXAConnection(physicalConn, "oracle.jdbc.driver.T4CXAConnection", dbType);
                        } else {
                            return createXAConnection(physicalConn, "oracle.jdbc.xa.client.OracleXAConnection", dbType);
                        }
                    case JdbcConstants.MARIADB:
                        return createXAConnection(physicalConn, "org.mariadb.jdbc.MariaXaConnection", dbType);
                    case JdbcConstants.POSTGRESQL:
                        return PGUtils.createXAConnection(physicalConn);
                    case JdbcConstants.KINGBASE:
                        return createXAConnection(physicalConn, "com.kingbase8.xa.KBXAConnection", dbType);
                    default:
                        throw new SQLException("xa not support dbType: " + dbType);
                }
            } catch (Exception xae) {
                throw new SQLException("create xaConnection error", xae);
            }
        }
    }

    private static XAConnection createXAConnection(
            Connection physicalConnection, String xaConnectionClassName, String dbType)
            throws XAException, SQLException {
        try {
            Class<?> xaConnectionClass = Class.forName(xaConnectionClassName);
            Constructor<XAConnection> constructor = getConstructorByDBType(xaConnectionClass, dbType);
            if (constructor == null) {
                throw new SQLException("xa not support dbType: " + dbType);
            }
            constructor.setAccessible(true);
            List<Object> params = getInitargsByDBType(dbType, physicalConnection);
            return constructor.newInstance(params.toArray(new Object[0]));
        } catch (Exception e) {
            LOGGER.warn("Failed to create XA Connection " + xaConnectionClassName + " on " + physicalConnection);
            if (e instanceof XAException) {
                throw (XAException) e;
            } else {
                throw new SQLException(e);
            }
        }
    }

    private static Constructor<XAConnection> getConstructorByDBType(Class xaConnectionClass, String dbType)
            throws SQLException {
        try {
            switch (dbType) {
                case JdbcConstants.ORACLE:
                    return xaConnectionClass.getConstructor(Connection.class);
                case JdbcConstants.MARIADB:
                    // MariaXaConnection(MariaDbConnection connection)
                    Class<?> mariaXaConnectionClass = Class.forName("org.mariadb.jdbc.MariaDbConnection");
                    return xaConnectionClass.getConstructor(mariaXaConnectionClass);
                case JdbcConstants.KINGBASE:
                    Class<?> kingbaseConnectionClass = Class.forName("com.kingbase8.core.BaseConnection");
                    return xaConnectionClass.getConstructor(kingbaseConnectionClass);
                case JdbcConstants.DM:
                    // 达梦
                    Class<?> dmConnectionClass = Class.forName(DM_CONNECTION_CLASS);
                    return xaConnectionClass.getConstructor(dmConnectionClass);
                default:
                    throw new SQLException("xa reflect not support dbType: " + dbType);
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }

    private static <T> List<T> getInitargsByDBType(String dbType, Object... params) throws SQLException {
        List result = new ArrayList<>();
        if (params.length == 0) {
            return null;
        }
        if (!(params[0] instanceof Connection)) {
            throw new SQLException("not support params: " + Arrays.toString(params));
        }

        try {
            switch (dbType) {
                case JdbcConstants.ORACLE:
                    result.add(params[0]);
                    return result;
                case JdbcConstants.KINGBASE:
                    result.add(params[0]);
                    return result;
                case JdbcConstants.MARIADB:
                    Class mariaDbConnectionClass = Class.forName("org.mariadb.jdbc.MariaDbConnection");
                    if (mariaDbConnectionClass.isInstance(params[0])) {
                        Object mariaDbConnectionInstance = mariaDbConnectionClass.cast(params[0]);
                        result.add(mariaDbConnectionInstance);
                        return result;
                    }
                case JdbcConstants.DM:
                    Class<?> dmConnectionClass = Class.forName(DM_CONNECTION_CLASS);
                    if (dmConnectionClass.isInstance(params[0])) {
                        result.add(dmConnectionClass.cast(params[0]));
                        return (List<T>) result;
                    }
                    break;
                default:
                    throw new SQLException("xa reflect not support dbType: " + dbType);
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
