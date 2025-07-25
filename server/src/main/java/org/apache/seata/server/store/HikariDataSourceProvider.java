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
package org.apache.seata.server.store;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.IsolationLevel;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.core.store.db.AbstractDataSourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_HIKARI_IDLE_TIMEOUT;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_HIKARI_KEEPALIVE_TIME;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_HIKARI_MAX_LIFE_TIME;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_HIKARI_VALIDATION_TIMEOUT;

/**
 * The hikari datasource provider
 */
@LoadLevel(name = "hikari")
public class HikariDataSourceProvider extends AbstractDataSourceProvider {

    private static final Logger logger = LoggerFactory.getLogger(HikariDataSourceProvider.class);

    @Override
    public DataSource doGenerate() {
        Properties properties = new Properties();
        properties.setProperty("dataSource.cachePrepStmts", "true");
        properties.setProperty("dataSource.prepStmtCacheSize", "250");
        properties.setProperty("dataSource.prepStmtCacheSqlLimit", "2048");
        properties.setProperty("dataSource.useServerPrepStmts", "true");
        properties.setProperty("dataSource.useLocalSessionState", "true");
        properties.setProperty("dataSource.rewriteBatchedStatements", "true");
        properties.setProperty("dataSource.cacheResultSetMetadata", "true");
        properties.setProperty("dataSource.cacheServerConfiguration", "true");
        properties.setProperty("dataSource.elideSetAutoCommits", "true");
        properties.setProperty("dataSource.maintainTimeStats", "false");

        HikariConfig config = new HikariConfig(properties);

        // Get the correct class loader
        ClassLoader driverClassLoader = getDriverClassLoader();
        String driverClassName = getDriverClassName();

        // Set driver class name in the correct class loader context
        ClassLoader originalClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(driverClassLoader);

            // 1. Explicitly load and register the driver
            try {
                Class<?> driverClass = Class.forName(driverClassName, true, driverClassLoader);
                Driver driver = (Driver) driverClass.newInstance();
                DriverManager.registerDriver(new DriverWrapper(driver));
            } catch (Exception e) {
                logger.warn("Failed to explicitly register driver {}", driverClassName, e);
            }

            // 2. Set configuration
            config.setDriverClassName(driverClassName);
            config.setJdbcUrl(getUrl());
            config.setUsername(getUser());
            config.setPassword(getPassword());

        } finally {
            Thread.currentThread().setContextClassLoader(originalClassLoader);
        }

        config.setMaximumPoolSize(getMaxConn());
        config.setMinimumIdle(getMinConn());
        config.setAutoCommit(true);
        config.setConnectionTimeout(getMaxWait());
        config.setInitializationFailTimeout(-1);
        config.setTransactionIsolation(IsolationLevel.TRANSACTION_READ_COMMITTED.name());
        config.setConnectionTestQuery(getValidationQuery(getDBType()));
        long idleTimeout =
                CONFIG.getLong(ConfigurationKeys.STORE_DB_HIKARI_IDLE_TIMEOUT, DEFAULT_DB_HIKARI_IDLE_TIMEOUT);
        config.setIdleTimeout(idleTimeout < 0 ? DEFAULT_DB_HIKARI_IDLE_TIMEOUT : idleTimeout);
        long keepaliveTime =
                CONFIG.getLong(ConfigurationKeys.STORE_DB_HIKARI_KEEPALIVE_TIME, DEFAULT_DB_HIKARI_KEEPALIVE_TIME);
        config.setKeepaliveTime(keepaliveTime < 0 ? DEFAULT_DB_HIKARI_KEEPALIVE_TIME : keepaliveTime);
        long maxLifeTime =
                CONFIG.getLong(ConfigurationKeys.STORE_DB_HIKARI_MAX_LIFE_TIME, DEFAULT_DB_HIKARI_MAX_LIFE_TIME);
        config.setMaxLifetime(maxLifeTime < 0 ? DEFAULT_DB_HIKARI_MAX_LIFE_TIME : maxLifeTime);
        long validationTimeout = CONFIG.getLong(
                ConfigurationKeys.STORE_DB_HIKARI_VALIDATION_TIMEOUT, DEFAULT_DB_HIKARI_VALIDATION_TIMEOUT);
        config.setValidationTimeout(validationTimeout < 0 ? DEFAULT_DB_HIKARI_VALIDATION_TIMEOUT : validationTimeout);

        return new HikariDataSource(config);
    }

    /**
     * Driver wrapper to ensure using the correct class loader
     */
    private static class DriverWrapper implements Driver {
        private final Driver delegate;

        public DriverWrapper(Driver delegate) {
            this.delegate = delegate;
        }

        @Override
        public Connection connect(String url, Properties info) throws SQLException {
            return delegate.connect(url, info);
        }

        @Override
        public boolean acceptsURL(String url) throws SQLException {
            return delegate.acceptsURL(url);
        }

        @Override
        public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
            return delegate.getPropertyInfo(url, info);
        }

        @Override
        public int getMajorVersion() {
            return delegate.getMajorVersion();
        }

        @Override
        public int getMinorVersion() {
            return delegate.getMinorVersion();
        }

        @Override
        public boolean jdbcCompliant() {
            return delegate.jdbcCompliant();
        }

        @Override
        public java.util.logging.Logger getParentLogger() throws SQLFeatureNotSupportedException {
            return delegate.getParentLogger();
        }
    }
}
