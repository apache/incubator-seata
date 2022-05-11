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

import java.lang.reflect.Constructor;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import javax.sql.XAConnection;
import javax.transaction.xa.XAException;
import com.alibaba.druid.util.JdbcUtils;
import com.alibaba.druid.util.MySqlUtils;
import com.alibaba.druid.util.PGUtils;
import io.seata.rm.BaseDataSourceResource;
import io.seata.sqlparser.util.JdbcConstants;
import org.mariadb.jdbc.MariaDbConnection;
import org.mariadb.jdbc.MariaXaConnection;
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

    public static XAConnection createXAConnection(Connection physicalConn, Driver driver, String dbType) throws SQLException {
        if (JdbcConstants.MYSQL.equals(dbType)) {
            return MySqlUtils.createXAConnection(driver, physicalConn);
        } else {
            switch (dbType) {
                case JdbcConstants.ORACLE:
                    try {
                        // https://github.com/alibaba/druid/issues/3707
                        // before Druid issue fixed, just make ORACLE XA connection in my way.
                        // return OracleUtils.OracleXAConnection(physicalConn);
                        String physicalConnClassName = physicalConn.getClass().getName();
                        if ("oracle.jdbc.driver.T4CConnection".equals(physicalConnClassName)) {
                            return createOracleXAConnection(physicalConn, "oracle.jdbc.driver.T4CXAConnection");
                        } else {
                            return createOracleXAConnection(physicalConn, "oracle.jdbc.xa.client.OracleXAConnection");
                        }
                    } catch (XAException xae) {
                        throw new SQLException("create xaConnection error", xae);
                    }
                case JdbcConstants.MARIADB:
                    return new MariaXaConnection((MariaDbConnection)physicalConn);
                case JdbcConstants.POSTGRESQL:
                    return PGUtils.createXAConnection(physicalConn);
                default:
                    throw new SQLException("xa not support dbType: " + dbType);
            }
        }
    }

    private static XAConnection createOracleXAConnection(Connection physicalConnection, String xaConnectionClassName) throws XAException, SQLException {
        try {
            Class xaConnectionClass = Class.forName(xaConnectionClassName);
            Constructor<XAConnection> constructor = xaConnectionClass.getConstructor(Connection.class);
            constructor.setAccessible(true);
            return constructor.newInstance(physicalConnection);
        } catch (Exception e) {
            LOGGER.warn("Failed to create Oracle XA Connection " + xaConnectionClassName + " on " + physicalConnection);
            if (e instanceof XAException) {
                throw (XAException) e;
            } else {
                throw new SQLException(e);
            }
        }

    }
}
