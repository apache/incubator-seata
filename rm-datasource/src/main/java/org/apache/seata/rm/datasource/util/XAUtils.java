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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.sql.XAConnection;
import javax.transaction.xa.XAException;

import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.druid.util.MySqlUtils;
import com.alibaba.druid.util.PGUtils;

import org.apache.seata.rm.BaseDataSourceResource;
import org.apache.seata.sqlparser.util.JdbcConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class XAUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(XAUtils.class);

    public static String getDbType(String jdbcUrl, String driverClassName) {
        return JdbcUtils.getDbType(jdbcUrl, driverClassName);
    }

    public static XAConnection createXAConnection(Connection physicalConn, BaseDataSourceResource dataSourceResource) throws SQLException {
        return createXAConnection(physicalConn, dataSourceResource.getDriver(), dataSourceResource.getDbType());
    }

    public static XAConnection createXAConnection(Connection physicalConn, Driver driver, String dbType)
        throws SQLException {
        if (JdbcConstants.MYSQL.equals(dbType)) {
            return MySqlUtils.createXAConnection(driver, physicalConn);
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
                    default:
                        throw new SQLException("xa not support dbType: " + dbType);
                }
            } catch (Exception xae) {
                throw new SQLException("create xaConnection error", xae);
            }
        }
    }

    private static XAConnection createXAConnection(Connection physicalConnection, String xaConnectionClassName,
                                                   String dbType) throws XAException, SQLException {
        try {
            Class xaConnectionClass = Class.forName(xaConnectionClassName);
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
                throw (XAException)e;
            } else {
                throw new SQLException(e);
            }
        }

    }

    private static Constructor<XAConnection> getConstructorByDBType(Class xaConnectionClass, String dbType) throws SQLException {
        try {
            switch (dbType) {
                case JdbcConstants.ORACLE:
                    return xaConnectionClass.getConstructor(Connection.class);
                case JdbcConstants.MARIADB:
                    //MariaXaConnection(MariaDbConnection connection)
                    Class mariaXaConnectionClass = Class.forName("org.mariadb.jdbc.MariaDbConnection");
                    return xaConnectionClass.getConstructor(mariaXaConnectionClass);
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
                case JdbcConstants.MARIADB:
                    Class mariaDbConnectionClass = Class.forName("org.mariadb.jdbc.MariaDbConnection");
                    if (mariaDbConnectionClass.isInstance(params[0])) {
                        Object mariaDbConnectionInstance = mariaDbConnectionClass.cast(params[0]);
                        result.add(mariaDbConnectionInstance);
                        return result;
                    }
                default:
                    throw new SQLException("xa reflect not support dbType: " + dbType);
            }
        } catch (Exception e) {
            throw new SQLException(e);
        }
    }
}
