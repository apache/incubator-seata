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
package org.apache.seata.benchmark.executor;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.apache.seata.rm.datasource.DataSourceProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.ThreadLocalRandom;

/**
 * AT mode transaction executor supporting both empty and real transaction modes
 * - branches == 0: Empty transaction mode (pure Seata protocol overhead testing)
 * - branches > 0: Real mode with MySQL database (via Testcontainers)
 */
public class ATModeExecutor extends AbstractTransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ATModeExecutor.class);

    private MySQLContainer<?> mysqlContainer;
    private HikariDataSource rawDataSource;
    private DataSourceProxy dataSourceProxy;

    public ATModeExecutor(BenchmarkConfig config) {
        super(config);
    }

    private boolean isRealMode() {
        return config.getBranches() > 0;
    }

    @Override
    public void init() {
        if (isRealMode()) {
            LOGGER.info("Initializing AT mode executor (MySQL via Testcontainers)");
            initRealMode();
        } else {
            LOGGER.info("AT mode executor initialized (empty transaction mode)");
        }
    }

    private void initRealMode() {
        try {
            // Start MySQL container
            startMySQLContainer();

            // Create HikariCP connection pool
            createDataSource();

            // Initialize database schema and data
            initDatabase();

            // Wrap with Seata DataSourceProxy for AT mode
            dataSourceProxy = new DataSourceProxy(rawDataSource);

            LOGGER.info("DataSourceProxy initialized, dbType: {}", dataSourceProxy.getDbType());
            LOGGER.info("Real AT mode executor initialized with {} accounts", BenchmarkConstants.ACCOUNT_COUNT);
        } catch (Exception e) {
            // Cleanup resources on initialization failure
            cleanupOnFailure();
            throw e instanceof RuntimeException
                    ? (RuntimeException) e
                    : new RuntimeException("Failed to initialize real AT mode executor", e);
        }
    }

    private void cleanupOnFailure() {
        LOGGER.warn("Cleaning up resources due to initialization failure");
        if (rawDataSource != null && !rawDataSource.isClosed()) {
            try {
                rawDataSource.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close DataSource during cleanup", e);
            }
        }
        if (mysqlContainer != null && mysqlContainer.isRunning()) {
            try {
                mysqlContainer.stop();
            } catch (Exception e) {
                LOGGER.warn("Failed to stop MySQL container during cleanup", e);
            }
        }
    }

    private void startMySQLContainer() {
        LOGGER.info("Starting MySQL container...");
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("benchmark")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

        mysqlContainer.start();

        LOGGER.info("MySQL container started: {}", mysqlContainer.getJdbcUrl());
    }

    private void createDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(mysqlContainer.getJdbcUrl());
        hikariConfig.setUsername(mysqlContainer.getUsername());
        hikariConfig.setPassword(mysqlContainer.getPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(config.getThreads() * 2);
        hikariConfig.setMinimumIdle(config.getThreads());
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);

        rawDataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("HikariCP DataSource created");
    }

    private void initDatabase() {
        try (Connection conn = rawDataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            // Create accounts table
            stmt.execute("CREATE TABLE IF NOT EXISTS accounts ("
                    + "id BIGINT PRIMARY KEY, "
                    + "balance INT NOT NULL, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Create undo_log table for Seata AT mode (MySQL syntax)
            stmt.execute("CREATE TABLE IF NOT EXISTS undo_log ("
                    + "branch_id BIGINT NOT NULL, "
                    + "xid VARCHAR(128) NOT NULL, "
                    + "context VARCHAR(128) NOT NULL, "
                    + "rollback_info LONGBLOB NOT NULL, "
                    + "log_status INT NOT NULL, "
                    + "log_created DATETIME(6) NOT NULL, "
                    + "log_modified DATETIME(6) NOT NULL, "
                    + "UNIQUE KEY ux_undo_log (xid, branch_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            // Insert test data
            stmt.execute("TRUNCATE TABLE accounts");
            try (PreparedStatement pstmt = conn.prepareStatement("INSERT INTO accounts (id, balance) VALUES (?, ?)")) {
                for (int i = 1; i <= BenchmarkConstants.ACCOUNT_COUNT; i++) {
                    pstmt.setLong(1, i);
                    pstmt.setInt(2, BenchmarkConstants.INITIAL_BALANCE);
                    pstmt.addBatch();
                    if (i % 100 == 0) {
                        pstmt.executeBatch();
                    }
                }
                pstmt.executeBatch();
            }

            LOGGER.info(
                    "Database initialized: {} accounts with balance {}",
                    BenchmarkConstants.ACCOUNT_COUNT,
                    BenchmarkConstants.INITIAL_BALANCE);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    @Override
    protected String getTransactionName() {
        return isRealMode() ? "benchmark-real-at-tx" : "benchmark-at-tx";
    }

    @Override
    protected int getBranchCount() {
        return config.getBranches();
    }

    @Override
    protected void executeBusinessLogic() throws Exception {
        if (isRealMode()) {
            executeBranchOperations(config.getBranches());
        }
        // Empty mode: do nothing (pure Seata protocol overhead testing)
    }

    private void executeBranchOperations(int branchCount) throws SQLException {
        // Execute N branch operations (simulating distributed transaction branches)
        for (int i = 0; i < branchCount; i++) {
            executeSingleBranch();
        }
    }

    private void executeSingleBranch() throws SQLException {
        // Use DataSourceProxy connection to enable AT mode
        try (Connection conn = dataSourceProxy.getConnection()) {
            conn.setAutoCommit(false);

            // Select two different accounts for transfer
            long fromAccount = (ThreadLocalRandom.current().nextInt(BenchmarkConstants.ACCOUNT_COUNT) + 1);
            // Ensure toAccount is different by selecting from remaining accounts
            long toAccount = (fromAccount % BenchmarkConstants.ACCOUNT_COUNT) + 1;
            int amount = ThreadLocalRandom.current().nextInt(BenchmarkConstants.MAX_TRANSFER_AMOUNT)
                    + BenchmarkConstants.MIN_TRANSFER_AMOUNT;

            // Debit from source account
            try (PreparedStatement pstmt =
                    conn.prepareStatement("UPDATE accounts SET balance = balance - ? WHERE id = ?")) {
                pstmt.setInt(1, amount);
                pstmt.setLong(2, fromAccount);
                pstmt.executeUpdate();
            }

            // Credit to destination account
            try (PreparedStatement pstmt =
                    conn.prepareStatement("UPDATE accounts SET balance = balance + ? WHERE id = ?")) {
                pstmt.setInt(1, amount);
                pstmt.setLong(2, toAccount);
                pstmt.executeUpdate();
            }

            conn.commit();
        }
    }

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }

    @Override
    public void destroy() {
        if (isRealMode()) {
            destroyRealMode();
        }
        LOGGER.info("AT mode executor destroyed");
    }

    private void destroyRealMode() {
        LOGGER.info("Destroying Real AT mode resources");

        if (rawDataSource != null && !rawDataSource.isClosed()) {
            rawDataSource.close();
            LOGGER.info("DataSource closed");
        }

        if (mysqlContainer != null && mysqlContainer.isRunning()) {
            mysqlContainer.stop();
            LOGGER.info("MySQL container stopped");
        }
    }
}
