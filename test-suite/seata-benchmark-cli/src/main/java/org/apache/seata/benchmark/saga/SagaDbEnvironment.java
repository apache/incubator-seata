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
package org.apache.seata.benchmark.saga;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.seata.benchmark.config.BenchmarkConfig;
import org.apache.seata.benchmark.constant.BenchmarkConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database-backed environment for Saga benchmark business actions.
 */
public class SagaDbEnvironment {

    private static final Logger LOGGER = LoggerFactory.getLogger(SagaDbEnvironment.class);

    private final BenchmarkConfig config;
    private MySQLContainer<?> mysqlContainer;
    private HikariDataSource dataSource;

    public SagaDbEnvironment(BenchmarkConfig config) {
        this.config = config;
    }

    public void init() {
        try {
            startMySQLContainer();
            createDataSource();
            initDatabase();
            LOGGER.info(
                    "Saga DB workload initialized with {} accounts and {} products",
                    BenchmarkConstants.ACCOUNT_COUNT,
                    BenchmarkConstants.SAGA_PRODUCT_COUNT);
        } catch (Exception e) {
            destroy();
            throw e instanceof RuntimeException
                    ? (RuntimeException) e
                    : new RuntimeException("Failed to initialize Saga DB workload", e);
        }
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void destroy() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Saga DB DataSource closed");
        }
        if (mysqlContainer != null && mysqlContainer.isRunning()) {
            mysqlContainer.stop();
            LOGGER.info("Saga DB MySQL container stopped");
        }
    }

    private void startMySQLContainer() {
        LOGGER.info("Starting MySQL container for Saga DB workload...");
        mysqlContainer = new MySQLContainer<>(DockerImageName.parse("mysql:8.0"))
                .withDatabaseName("benchmark_saga")
                .withUsername("test")
                .withPassword("test")
                .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");
        mysqlContainer.start();
        LOGGER.info("Saga DB MySQL container started: {}", mysqlContainer.getJdbcUrl());
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
        dataSource = new HikariDataSource(hikariConfig);
    }

    private void initDatabase() throws SQLException {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS benchmark_inventory ("
                    + "product_id VARCHAR(64) PRIMARY KEY, "
                    + "available_qty INT NOT NULL, "
                    + "reserved_qty INT NOT NULL DEFAULT 0, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            stmt.execute("CREATE TABLE IF NOT EXISTS benchmark_account ("
                    + "account_id VARCHAR(64) PRIMARY KEY, "
                    + "balance DECIMAL(18,2) NOT NULL, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            stmt.execute("CREATE TABLE IF NOT EXISTS benchmark_order ("
                    + "order_id VARCHAR(64) PRIMARY KEY, "
                    + "user_id VARCHAR(64) NOT NULL, "
                    + "product_id VARCHAR(64) NOT NULL, "
                    + "quantity INT NOT NULL, "
                    + "amount DECIMAL(18,2) NOT NULL, "
                    + "status VARCHAR(32) NOT NULL, "
                    + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                    + "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            stmt.execute("TRUNCATE TABLE benchmark_order");
            stmt.execute("TRUNCATE TABLE benchmark_inventory");
            stmt.execute("TRUNCATE TABLE benchmark_account");

            seedInventory(conn);
            seedAccounts(conn);
        }
    }

    private void seedInventory(Connection conn) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(
                "INSERT INTO benchmark_inventory (product_id, available_qty, reserved_qty) VALUES (?, ?, 0)")) {
            for (int i = 0; i < BenchmarkConstants.SAGA_PRODUCT_COUNT; i++) {
                pstmt.setString(1, "product-" + i);
                pstmt.setInt(2, BenchmarkConstants.SAGA_INITIAL_INVENTORY);
                pstmt.addBatch();
                if ((i + 1) % 100 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
        }
    }

    private void seedAccounts(Connection conn) throws SQLException {
        try (PreparedStatement pstmt =
                conn.prepareStatement("INSERT INTO benchmark_account (account_id, balance) VALUES (?, ?)")) {
            for (int i = 0; i < BenchmarkConstants.ACCOUNT_COUNT; i++) {
                pstmt.setString(1, "account-" + i);
                pstmt.setBigDecimal(2, BigDecimal.valueOf(BenchmarkConstants.SAGA_INITIAL_BALANCE));
                pstmt.addBatch();
                if ((i + 1) % 100 == 0) {
                    pstmt.executeBatch();
                }
            }
            pstmt.executeBatch();
        }
    }
}
