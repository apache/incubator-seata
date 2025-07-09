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
package org.apache.seata.server.store.db;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.DynamicPortTestConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/**
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import(DynamicPortTestConfig.class)
public class AbstractDataSourceProviderTest {

    private final String dbcpDatasourceType = "dbcp";

    private final String druidDatasourceType = "druid";

    private final String hikariDatasourceType = "hikari";

    private final String mysqlJdbcDriver = "com.mysql.jdbc.Driver";
    private final String mysql8JdbcDriver = "com.mysql.cj.jdbc.Driver";

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        EnhancedServiceLoader.unloadAll();
        ConfigurationFactory.reload();
        System.clearProperty("store.db.driverClassName");
    }

    @AfterEach
    void tearDown() {
        EnhancedServiceLoader.unloadAll();
        ConfigurationFactory.reload();
        System.clearProperty("store.db.driverClassName");
    }

    @Test
    @Order(1)
    public void testDbcpDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType)
                .provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(2)
    public void testLoadMysqlDriver() {
        System.setProperty("loader.path", "/tmp");
        System.setProperty("store.db.driverClassName", mysqlJdbcDriver);
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType)
                .provide();
        Assertions.assertNotNull(dataSource);
        System.setProperty("store.db.driverClassName", mysql8JdbcDriver);
        dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType)
                .provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(3)
    public void testLoadDMDriver() {
        System.setProperty("store.db.driverClassName", "dm.jdbc.driver.DmDriver");
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType)
                .provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(4)
    public void testLoadDriverFailed() {
        System.setProperty("store.db.driverClassName", "dm.jdbc.driver.DmDriver1");
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType)
                    .provide();
        });
    }

    @Test
    @Order(5)
    public void testDruidDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, druidDatasourceType)
                .provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(6)
    public void testHikariDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, hikariDatasourceType)
                .provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(7)
    public void testMySQLDataSourceProvider() throws ClassNotFoundException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Class<?> driverClass = Class.forName(mysqlJdbcDriver, true, classLoader);
        Assertions.assertNotNull(driverClass);
    }

    @Test
    @Order(8)
    public void testHikariDataSourceProviderWithMySQLDriver() {
        // Set MySQL 8 driver
        System.setProperty("store.db.driverClassName", mysql8JdbcDriver);

        try {
            // Use Hikari data source provider
            DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, hikariDatasourceType)
                    .provide();
            Assertions.assertNotNull(dataSource);

            // Verify it's a HikariDataSource type
            Assertions.assertTrue(dataSource instanceof com.zaxxer.hikari.HikariDataSource);

            // The most critical verification: try to get a connection (this will trigger the actual driver loading)
            // Note: This might throw an exception due to database connection failure, but importantly, it should not
            // show "Failed to load driver class" error
            try {
                Connection connection = dataSource.getConnection();
                // If we reach here, it means the driver loaded successfully and connection succeeded
                Assertions.assertNotNull(connection);
                connection.close();
            } catch (SQLException e) {
                // Database connection failure is normal (test environment might not have real MySQL), but error message
                // should not contain driver class loading failure
                String errorMessage = e.getMessage();
                Assertions.assertFalse(
                        errorMessage.contains("Failed to load driver class"),
                        "Driver class should be loaded successfully, but got: " + errorMessage);
                Assertions.assertFalse(
                        errorMessage.contains("HikariConfig class loader"),
                        "Driver classloader issue should be resolved, but got: " + errorMessage);
                // Here we expect connection-related errors, such as connection timeout, connection refused, etc.
                System.out.println("Expected database connection error (driver loaded successfully): " + errorMessage);
            }

        } catch (Exception e) {
            // If it's a driver loading related exception, the test should fail
            if (e.getMessage().contains("Failed to load driver class")
                    || e.getMessage().contains("HikariConfig class loader")) {
                Assertions.fail("HikariCP should load MySQL driver successfully with custom classloader, but got: "
                        + e.getMessage());
            }
            // Other exceptions might be normal (such as configuration issues, etc.)
            System.out.println("Non-driver related exception (might be expected): " + e.getMessage());
        }
    }

    @Test
    @Order(9)
    public void testHikariDataSourceProviderWithMySQLLegacyDriver() {
        // Test with legacy MySQL driver as well
        System.setProperty("store.db.driverClassName", mysqlJdbcDriver);

        try {
            DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, hikariDatasourceType)
                    .provide();
            Assertions.assertNotNull(dataSource);

            // Try to get connection to verify driver loading
            try {
                Connection connection = dataSource.getConnection();
                Assertions.assertNotNull(connection);
                connection.close();
            } catch (SQLException e) {
                String errorMessage = e.getMessage();
                Assertions.assertFalse(
                        errorMessage.contains("Failed to load driver class"),
                        "Legacy MySQL driver should also be loaded successfully, but got: " + errorMessage);
            }

        } catch (Exception e) {
            if (e.getMessage().contains("Failed to load driver class")) {
                Assertions.fail("HikariCP should load legacy MySQL driver successfully, but got: " + e.getMessage());
            }
        }
    }
}
