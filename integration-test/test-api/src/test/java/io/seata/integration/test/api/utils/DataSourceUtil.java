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
package io.seata.integration.test.api.utils;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.rm.datasource.DataSourceProxy;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.sql.DataSource;


/**
 * The type Data source util.
 */
public class DataSourceUtil {

    /**
     * The constant JDBC_PRO_PATH.
     */
    public static final String JDBC_PRO_PATH = "jdbc.properties";
    /**
     * The constant DATA_SOURCE_MAP.
     */
    public static final ConcurrentMap<String, DataSource> DATA_SOURCE_MAP = new ConcurrentHashMap<>();

    /**
     * Gets data source.
     *
     * @param name the name
     * @return the data source
     */
    public static DataSource getDataSource(String name) {
        String driverKey = "jdbc." + name + ".driver";
        String urlKey = "jdbc." + name + ".url";
        String userNameKey = "jdbc." + name + ".username";
        String pwdKey = "jdbc." + name + ".password";
        DataSource dataSource = new DruidDataSource();
        ((DruidDataSource) dataSource).setDriverClassName(PropertiesUtil.getPropertieValue(JDBC_PRO_PATH, driverKey));
        ((DruidDataSource) dataSource).setUrl(PropertiesUtil.getPropertieValue(JDBC_PRO_PATH, urlKey));
        ((DruidDataSource) dataSource).setUsername(PropertiesUtil.getPropertieValue(JDBC_PRO_PATH, userNameKey));
        ((DruidDataSource) dataSource).setPassword(PropertiesUtil.getPropertieValue(JDBC_PRO_PATH, pwdKey));
        return new DataSourceProxy(dataSource);
    }

    /**
     * Gets connection.
     *
     * @param name the name
     * @return the connection
     * @throws SQLException the sql exception
     */
    public static Connection getConnection(String name) throws SQLException {
        DATA_SOURCE_MAP.putIfAbsent(name, getDataSource(name));
        return DATA_SOURCE_MAP.get(name).getConnection();
    }

    /**
     * Execute update int.
     *
     * @param name the name
     * @param sql  the sql
     * @return the int
     * @throws SQLException the sql exception
     */
    public static int executeUpdate(String name, String sql) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        int result = 0;
        try {
            connection = getConnection(name);
            statement = connection.createStatement();
            result = statement.executeUpdate(sql);

        } catch (SQLException exx) {
            //todo
            throw exx;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException exx) {
            }
        }
        return result;
    }

    /**
     * Gets single result.
     *
     * @param name the name
     * @param sql  the sql
     * @return the single result
     * @throws SQLException the sql exception
     */
    public static String getSingleResult(String name, String sql) throws SQLException {
        Connection connection = null;
        Statement statement = null;
        String result = null;
        try {
            connection = getConnection(name);
            statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery(sql);
            resultSet.next();
            result = resultSet.getString(1);
        } catch (SQLException exx) {
            //todo
            throw exx;
        } finally {
            try {
                if (statement != null) {
                    statement.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (SQLException exx) {
            }
        }
        return result;
    }
}
