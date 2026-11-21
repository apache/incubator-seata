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

import com.alibaba.druid.pool.DruidDataSource;
import com.zaxxer.hikari.HikariDataSource;
import org.apache.commons.dbcp2.BasicDataSource;
import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Verifies the datasource providers (dbcp/druid/hikari) against an isolated MySQL driver
 * fixture. Provider creation must succeed or fail loudly: a driver that cannot be loaded
 * must never produce a silently passing test, and every test-owned pool must be closed
 * to avoid background connection attempts after the test ends.
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AbstractDataSourceProviderTest extends BaseSpringBootTest {

    private static final String DBCP_DATASOURCE_TYPE = "dbcp";

    private static final String DRUID_DATASOURCE_TYPE = "druid";

    private static final String HIKARI_DATASOURCE_TYPE = "hikari";

    private static final String MYSQL_JDBC_DRIVER = "com.mysql.jdbc.Driver";
    private static final String MYSQL8_JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String MYSQL_DRIVER_CLASS_RESOURCE = "com/mysql/cj/jdbc/Driver.class";

    /**
     * Failure markers that always indicate a driver-loading problem. Such failures must
     * fail the tests instead of being tolerated as expected connection errors.
     */
    private static final List<String> DRIVER_STAGE_FAILURE_MARKERS = Arrays.asList(
            "Failed to load driver class",
            "HikariConfig class loader",
            "cannot find jdbc folder",
            "UnsupportedClassVersionError",
            "ClassNotFoundException",
            "NoClassDefFoundError");

    /**
     * DataSources created by the tests. They are closed after each test so that no pool
     * keeps trying to connect to the configured (absent) MySQL server in the background.
     */
    private final List<DataSource> createdDataSources = new ArrayList<>();

    @BeforeAll
    public static void setUp(ApplicationContext context) throws Exception {
        EnhancedServiceLoader.unloadAll();
        ConfigurationFactory.reload();
        System.clearProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME);
        // Fail fast on connection attempts instead of waiting for the default timeout,
        // because there is no MySQL server in the test environment.
        System.setProperty(ConfigurationKeys.STORE_DB_MAX_WAIT, "2000");
        prepareIsolatedDriverFixture();
    }

    @AfterAll
    public static void cleanUp() {
        System.clearProperty(ConfigurationKeys.STORE_DB_MAX_WAIT);
        System.clearProperty("loader.path");
    }

    @AfterEach
    public void tearDown() {
        closeCreatedDataSources();
        EnhancedServiceLoader.unloadAll();
        ConfigurationFactory.reload();
        System.clearProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME);
    }

    /**
     * Builds an isolated driver fixture: copies the mysql-connector-j jar (a declared test
     * dependency) into a temporary "jdbc" directory and points "loader.path" at it, so the
     * MySQL drivers are loaded through the dedicated driver classloader, exactly like a
     * production seata-server launched with external driver jars.
     */
    private static void prepareIsolatedDriverFixture() throws Exception {
        URL driverClassUrl = ClassLoader.getSystemResource(MYSQL_DRIVER_CLASS_RESOURCE);
        Assertions.assertNotNull(
                driverClassUrl,
                "mysql-connector-j is a declared test dependency, but " + MYSQL_DRIVER_CLASS_RESOURCE
                        + " was not found on the test classpath");
        Assertions.assertEquals(
                "jar",
                driverClassUrl.getProtocol(),
                "the MySQL driver is expected to be loaded from its jar, but got: " + driverClassUrl);
        String jarUrl = driverClassUrl.toString();
        String innerFileUrl = jarUrl.substring("jar:".length(), jarUrl.indexOf("!/"));
        File driverJar = new File(URI.create(innerFileUrl));
        File fixtureDir = new File(System.getProperty("java.io.tmpdir"), "seata-driver-fixture");
        File jdbcDir = new File(fixtureDir, "jdbc");
        Files.createDirectories(jdbcDir.toPath());
        Files.copy(
                driverJar.toPath(),
                new File(jdbcDir, driverJar.getName()).toPath(),
                StandardCopyOption.REPLACE_EXISTING);
        System.setProperty("loader.path", fixtureDir.getAbsolutePath());
    }

    @Test
    @Order(1)
    public void testDbcpDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, DBCP_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(dataSource);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(2)
    public void testLoadMysqlDriver() {
        System.setProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME, MYSQL_JDBC_DRIVER);
        DataSource legacyDataSource = EnhancedServiceLoader.load(DataSourceProvider.class, DBCP_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(legacyDataSource);
        Assertions.assertNotNull(legacyDataSource);

        EnhancedServiceLoader.unloadAll();

        System.setProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME, MYSQL8_JDBC_DRIVER);
        DataSource mysql8DataSource = EnhancedServiceLoader.load(DataSourceProvider.class, DBCP_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(mysql8DataSource);
        Assertions.assertNotNull(mysql8DataSource);
    }

    @Test
    @Order(3)
    public void testLoadDMDriver() {
        System.setProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME, "dm.jdbc.driver.DmDriver");
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, DBCP_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(dataSource);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(4)
    public void testLoadDriverFailed() {
        System.setProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME, "dm.jdbc.driver.DmDriver1");
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            EnhancedServiceLoader.load(DataSourceProvider.class, DBCP_DATASOURCE_TYPE)
                    .provide();
        });
    }

    @Test
    @Order(5)
    public void testDruidDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, DRUID_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(dataSource);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(6)
    public void testHikariDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, HIKARI_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(dataSource);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(7)
    public void testMySQLDataSourceProvider() throws ClassNotFoundException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Class<?> driverClass = Class.forName(MYSQL_JDBC_DRIVER, true, classLoader);
        Assertions.assertNotNull(driverClass);
    }

    /**
     * Provider creation already verifies that the MySQL 8 driver is loadable through the
     * isolated driver fixture classloader. Therefore a getConnection failure can only come
     * from the connection stage (there is no MySQL server in the test environment), and any
     * driver-stage failure must fail the test.
     */
    @Test
    @Order(8)
    public void testHikariDataSourceProviderWithMySQLDriver() {
        System.setProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME, MYSQL8_JDBC_DRIVER);

        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, HIKARI_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(dataSource);
        Assertions.assertTrue(
                dataSource instanceof HikariDataSource,
                "the hikari provider should create a HikariDataSource, but got: "
                        + dataSource.getClass().getName());

        SQLException failure = Assertions.assertThrows(
                SQLException.class,
                dataSource::getConnection,
                "a real connection attempt is expected to fail without a MySQL server, but it succeeded");
        Assertions.assertFalse(
                containsDriverStageFailure(failure),
                "the MySQL 8 driver should be loaded successfully, but got a driver-stage failure: " + failure);
    }

    /**
     * Same expectation as {@link #testHikariDataSourceProviderWithMySQLDriver()} but with the
     * legacy MySQL driver name kept for backward compatibility.
     */
    @Test
    @Order(9)
    public void testHikariDataSourceProviderWithMySQLLegacyDriver() {
        System.setProperty(ConfigurationKeys.STORE_DB_DRIVER_CLASS_NAME, MYSQL_JDBC_DRIVER);

        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, HIKARI_DATASOURCE_TYPE)
                .provide();
        createdDataSources.add(dataSource);
        Assertions.assertTrue(
                dataSource instanceof HikariDataSource,
                "the hikari provider should create a HikariDataSource, but got: "
                        + dataSource.getClass().getName());

        SQLException failure = Assertions.assertThrows(
                SQLException.class,
                dataSource::getConnection,
                "a real connection attempt is expected to fail without a MySQL server, but it succeeded");
        Assertions.assertFalse(
                containsDriverStageFailure(failure),
                "the legacy MySQL driver should be loaded successfully, but got a driver-stage failure: " + failure);
    }

    /**
     * Walks the exception chain and reports whether any failure was caused by driver loading
     * instead of the connection attempt itself.
     */
    private static boolean containsDriverStageFailure(SQLException failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            String message = String.valueOf(current.getMessage());
            for (String marker : DRIVER_STAGE_FAILURE_MARKERS) {
                if (message.contains(marker)) {
                    return true;
                }
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    /**
     * Closes every pool created by the current test so that no background connection
     * attempts survive the test. Closing failures are swallowed on purpose: they must not
     * mask the test result.
     */
    private void closeCreatedDataSources() {
        for (DataSource dataSource : createdDataSources) {
            try {
                if (dataSource instanceof HikariDataSource) {
                    ((HikariDataSource) dataSource).close();
                } else if (dataSource instanceof DruidDataSource) {
                    ((DruidDataSource) dataSource).close();
                } else if (dataSource instanceof BasicDataSource) {
                    ((BasicDataSource) dataSource).close();
                }
            } catch (Exception e) {
                // closing failures must not mask the test result
            }
        }
        createdDataSources.clear();
    }
}
