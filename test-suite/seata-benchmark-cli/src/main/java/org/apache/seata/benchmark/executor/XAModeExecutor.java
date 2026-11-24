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
import org.apache.seata.rm.datasource.xa.DataSourceProxyXA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * XA mode transaction executor supporting both empty and real transaction modes.
 *
 * <p>Supports two sub-modes controlled by {@code --branches}:
 * <ul>
 *   <li><b>Empty mode</b> ({@code branches == 0}): starts and commits an empty global transaction.
 *       Measures pure Seata XA protocol overhead with no branch registration.</li>
 *   <li><b>Real mode</b> ({@code branches > 0}): executes {@code branches} XA branch operations
 *       per transaction using MySQL via Testcontainers and {@link DataSourceProxyXA}.</li>
 * </ul>
 */
public class XAModeExecutor extends AbstractTransactionExecutor {

    private static final Logger LOGGER = LoggerFactory.getLogger(XAModeExecutor.class);
    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = ThreadLocal.withInitial(SecureRandom::new);

    private MySQLContainer<?> mysqlContainer;
    private HikariDataSource rawDataSource;
    private DataSourceProxyXA dataSourceProxyXA;

    public XAModeExecutor(BenchmarkConfig config) {
        super(config);
    }

    private boolean isRealMode() {
        return config.getBranches() > 0;
    }

    @Override
    public void init() {
        if (isRealMode()) {
            LOGGER.info("Initializing XA mode executor (MySQL XA via Testcontainers)");
            initRealMode();
        } else {
            LOGGER.info("XA mode executor initialized (empty transaction mode)");
        }
    }

    private void initRealMode() {
        try {
            startMySQLContainer();
            createDataSource();
            initDatabase();
            dataSourceProxyXA = new DataSourceProxyXA(rawDataSource);
            LOGGER.info("XA DataSourceProxy initialized, dbType: {}", dataSourceProxyXA.getDbType());
            LOGGER.info("Real XA mode executor initialized with {} accounts", BenchmarkConstants.ACCOUNT_COUNT);
        } catch (Exception e) {
            cleanupOnFailure();
            throw e instanceof RuntimeException
                    ? (RuntimeException) e
                    : new RuntimeException("Failed to initialize real XA mode executor", e);
        }
    }

    private void cleanupOnFailure() {
        LOGGER.warn("Cleaning up XA mode resources due to initialization failure");
        if (rawDataSource != null && !rawDataSource.isClosed()) {
            try {
                rawDataSource.close();
            } catch (Exception e) {
                LOGGER.warn("Failed to close XA DataSource during cleanup", e);
            }
        }
        if (mysqlContainer != null && mysqlContainer.isRunning()) {
            try {
                mysqlContainer.stop();
            } catch (Exception e) {
                LOGGER.warn("Failed to stop XA MySQL container during cleanup", e);
            }
        }
    }

    private void startMySQLContainer() {
        LOGGER.info("Starting MySQL container for XA mode...");
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("benchmark_xa")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");
        mysqlContainer.start();
        LOGGER.info("XA MySQL container started: {}", mysqlContainer.getJdbcUrl());
    }

    private void createDataSource() {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(mysqlContainer.getJdbcUrl());
        hikariConfig.setUsername(mysqlContainer.getUsername());
        hikariConfig.setPassword(mysqlContainer.getPassword());
        hikariConfig.setDriverClassName("com.mysql.cj.jdbc.Driver");
        hikariConfig.setMaximumPoolSize(config.getThreads() * 2);
        hikariConfig.setMinimumIdle(Math.max(1, config.getThreads()));
        hikariConfig.setConnectionTimeout(30000);
        hikariConfig.setIdleTimeout(600000);
        hikariConfig.setMaxLifetime(1800000);
        rawDataSource = new HikariDataSource(hikariConfig);
        LOGGER.info("XA HikariCP DataSource created");
    }

    private void initDatabase() {
        try (Connection conn = rawDataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS xa_accounts ("
                    + "id BIGINT PRIMARY KEY, "
                    + "balance INT NOT NULL, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.execute("TRUNCATE TABLE xa_accounts");
            try (PreparedStatement pstmt =
                    conn.prepareStatement("INSERT INTO xa_accounts (id, balance) VALUES (?, ?)")) {
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
                    "XA database initialized: {} accounts with balance {}",
                    BenchmarkConstants.ACCOUNT_COUNT,
                    BenchmarkConstants.INITIAL_BALANCE);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize XA database", e);
        }
    }

    @Override
    protected String getTransactionName() {
        return isRealMode() ? "benchmark-real-xa-tx" : "benchmark-xa-tx";
    }

    @Override
    protected int getBranchCount() {
        return config.getBranches();
    }

    @Override
    protected void executeBusinessLogic() throws Exception {
        if (!isRealMode()) {
            return;
        }
        executeBranchOperations(config.getBranches());
    }

    private void executeBranchOperations(int branchCount) throws SQLException {
        for (int i = 0; i < branchCount; i++) {
            executeSingleBranch();
        }
    }

    private void executeSingleBranch() throws SQLException {
        try (Connection conn = dataSourceProxyXA.getConnection()) {
            conn.setAutoCommit(false);

            SecureRandom secureRandom = SECURE_RANDOM.get();
            long fromAccount = secureRandom.nextInt(BenchmarkConstants.ACCOUNT_COUNT) + 1L;
            long toAccount = (fromAccount % BenchmarkConstants.ACCOUNT_COUNT) + 1L;
            int amount = secureRandom.nextInt(BenchmarkConstants.MAX_TRANSFER_AMOUNT)
                    + BenchmarkConstants.MIN_TRANSFER_AMOUNT;

            try (PreparedStatement pstmt =
                    conn.prepareStatement("UPDATE xa_accounts SET balance = balance - ? WHERE id = ?")) {
                pstmt.setInt(1, amount);
                pstmt.setLong(2, fromAccount);
                pstmt.executeUpdate();
            }

            try (PreparedStatement pstmt =
                    conn.prepareStatement("UPDATE xa_accounts SET balance = balance + ? WHERE id = ?")) {
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
        if (rawDataSource != null && !rawDataSource.isClosed()) {
            rawDataSource.close();
            LOGGER.info("XA DataSource closed");
        }
        if (mysqlContainer != null && mysqlContainer.isRunning()) {
            mysqlContainer.stop();
            LOGGER.info("XA MySQL container stopped");
        }
        dataSourceProxyXA = null;
        LOGGER.info("XA mode executor destroyed");
    }
}
