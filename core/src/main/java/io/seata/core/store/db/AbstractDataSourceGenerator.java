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
package io.seata.core.store.db;

import io.seata.common.exception.StoreException;
import io.seata.common.util.StringUtils;
import io.seata.config.Configuration;
import io.seata.config.ConfigurationFactory;
import io.seata.core.constants.ConfigurationKeys;
import io.seata.core.constants.DBType;

/**
 * The type Abstract data source generator.
 *
 * @author zhangsen
 */
public abstract class AbstractDataSourceGenerator implements DataSourceGenerator {

    /**
     * The constant CONFIG.
     */
    protected static final Configuration CONFIG = ConfigurationFactory.getInstance();

    private static final int DEFAULT_DB_MAX_CONN = 10;

    private static final int DEFAULT_DB_MIN_CONN = 1;

    /**
     * Get db type db type.
     *
     * @return the db type
     */
    protected DBType getDBType() {
        return DBType.valueof(CONFIG.getConfig(ConfigurationKeys.STORE_DB_TYPE));
    }

    /**
     * get db driver class name
     *
     * @return the db driver class name
     */
    protected String getDriverClassName() {
        String driverClassName = CONFIG.getConfig(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME);
        if (StringUtils.isBlank(driverClassName)) {
            throw new StoreException(
                String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME));
        }
        return driverClassName;
    }

    /**
     * Get url string.
     *
     * @return the string
     */
    protected String getUrl() {
        String url = CONFIG.getConfig(ConfigurationKeys.STORE_DB_URL);
        if (StringUtils.isBlank(url)) {
            throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_URL));
        }
        return url;
    }

    /**
     * Get user string.
     *
     * @return the string
     */
    protected String getUser() {
        String user = CONFIG.getConfig(ConfigurationKeys.STORE_DB_USER);
        if (StringUtils.isBlank(user)) {
            throw new StoreException(String.format("the {%s} can't be empty", ConfigurationKeys.STORE_DB_USER));
        }
        return user;
    }

    /**
     * Get password string.
     *
     * @return the string
     */
    protected String getPassword() {
        String password = CONFIG.getConfig(ConfigurationKeys.STORE_DB_PASSWORD);
        return password;
    }

    /**
     * Get min conn int.
     *
     * @return the int
     */
    protected int getMinConn() {
        int minConn = CONFIG.getInt(ConfigurationKeys.STORE_DB_MIN_CONN, DEFAULT_DB_MIN_CONN);
        return minConn < 0 ? DEFAULT_DB_MIN_CONN : minConn;
    }

    /**
     * Get max conn int.
     *
     * @return the int
     */
    protected int getMaxConn() {
        int maxConn = CONFIG.getInt(ConfigurationKeys.STORE_DB_MAX_CONN, DEFAULT_DB_MAX_CONN);
        return maxConn < 0 ? DEFAULT_DB_MAX_CONN : maxConn;
    }

    /**
     * Get driver name string.
     *
     * @param dbType the db type
     * @return the string
     */
    protected static String getDriverName(DBType dbType) {
        if (DBType.H2.equals(dbType)) {
            return "org.h2.Driver";
        } else if (DBType.MYSQL.equals(dbType)) {
            return "com.mysql.jdbc.Driver";
        } else if (DBType.ORACLE.equals(dbType)) {
            return "oracle.jdbc.OracleDriver";
        } else if (DBType.SYBAEE.equals(dbType)) {
            return "com.sybase.jdbc2.jdbc.SybDriver";
        } else if (DBType.SQLSERVER.equals(dbType)) {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        } else if (DBType.SQLITE.equals(dbType)) {
            return "org.sqlite.JDBC";
        } else if (DBType.POSTGRESQL.equals(dbType)) {
            return "org.postgresql.Driver";
        } else if (DBType.ACCESS.equals(dbType)) {
            return "com.hxtt.sql.access.AccessDriver";
        } else if (DBType.DB2.equals(dbType)) {
            return "com.ibm.db2.jcc.DB2Driver";
        } else {
            throw new StoreException("Unsupported database type, dbType:" + dbType);
        }
    }

    /**
     * Get validation query string.
     *
     * @param dbType the db type
     * @return the string
     */
    protected String getValidationQuery(DBType dbType) {
        if (DBType.ORACLE.equals(dbType)) {
            return "select sysdate from dual";
        } else {
            return "select 1";
        }
    }

}
