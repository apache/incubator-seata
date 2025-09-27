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
package org.apache.seata.mcp.store;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.util.IsolationLevel;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.mcp.store.db.AbstractMCPDataSourceProvider;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * The hikari datasource provider
 */
@LoadLevel(name = "hikari")
public class HikariDataSourceProvider extends AbstractMCPDataSourceProvider {

    @Override
    public DataSource doGenerate() {
        Properties properties = new Properties();
        properties.setProperty("dataSource.cachePrepStmts", "true"); // Enable prepared statement caching
        properties.setProperty("dataSource.prepStmtCacheSize", "250"); // Number of prepared statements to cache
        properties.setProperty("dataSource.prepStmtCacheSqlLimit", "2048"); // Maximum SQL statement length to cache
        properties.setProperty("dataSource.useServerPrepStmts", "true"); // Use server-side prepared statements
        properties.setProperty("dataSource.useLocalSessionState", "true"); // Track transaction state locally
        properties.setProperty(
                "dataSource.rewriteBatchedStatements", "true"); // Rewrite batched statements for efficiency
        properties.setProperty("dataSource.cacheResultSetMetadata", "true"); // Cache result set metadata
        properties.setProperty("dataSource.cacheServerConfiguration", "true"); // Cache server configuration
        properties.setProperty("dataSource.elideSetAutoCommits", "true"); // Don't send autocommit if unchanged
        properties.setProperty("dataSource.maintainTimeStats", "false"); // Disable time statistics tracking

        HikariConfig config = new HikariConfig(properties);

        config.setDriverClassName(getDriverClassName());
        config.setJdbcUrl(getUrl());
        config.setUsername(getUser());
        config.setPassword(getPassword());

        config.setMaximumPoolSize(getMaxConn()); // Maximum size of connection pool
        config.setMinimumIdle(getMinConn()); // Minimum number of idle connections
        config.setIdleTimeout(300000); // Maximum idle time (5 minutes)

        config.setConnectionTimeout(getMaxWait()); // Maximum wait time for connection
        config.setInitializationFailTimeout(-1); // No timeout for pool initialization

        config.setConnectionTestQuery(getValidationQuery(getDBType())); // Query to validate connections
        config.setValidationTimeout(5000); // Validation timeout (5 seconds)
        config.setMaxLifetime(1800000); // Maximum connection lifetime (30 minutes)
        config.setKeepaliveTime(60000); // Keepalive interval (60 seconds)

        config.setAutoCommit(true); // Auto-commit connections by default
        config.setTransactionIsolation(IsolationLevel.TRANSACTION_READ_COMMITTED.name());

        properties.setProperty("connectionRetryAttempts", "3"); // Attempt connection 3 times
        properties.setProperty("connectionRetryDelay", "1000"); // Wait 1 second between retries
        properties.setProperty("connectionTimeoutMs", "5000"); // Connection timeout 5 seconds

        return new HikariDataSource(config);
    }
}
