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

import com.zaxxer.hikari.HikariDataSource;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.DBType;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.BaseSpringBootTest;
import org.apache.seata.server.store.HikariDataSourceProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.context.ApplicationContext;

import javax.sql.DataSource;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DataSourceProviderTest extends BaseSpringBootTest {

    private final List<AutoCloseable> dataSources = new ArrayList<>();
    private String originalDriver;
    private String originalMinConn;

    @BeforeEach
    void preserveConfiguration() {
        originalDriver = System.getProperty("store.db.driverClassName");
        originalMinConn = System.getProperty("store.db.minConn");
        System.clearProperty("store.db.driverClassName");
        System.setProperty("store.db.minConn", "0");
        ConfigurationFactory.reload();
    }

    private DataSource loadDataSource(String type) {
        DataSource result =
                EnhancedServiceLoader.load(DataSourceProvider.class, type).provide();
        AutoCloseable closeable = (AutoCloseable) result;
        if (!dataSources.contains(closeable)) {
            dataSources.add(closeable);
        }
        return result;
    }

    private static void restoreProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private final String dbcpDatasourceType = "dbcp";

    private final String druidDatasourceType = "druid";

    private final String hikariDatasourceType = "hikari";

    private final String mysqlJdbcDriver = "com.mysql.jdbc.Driver";

    @BeforeAll
    public static void setUp(ApplicationContext context) {
        EnhancedServiceLoader.unloadAll();
        ConfigurationFactory.reload();
    }

    @AfterEach
    public void tearDown() throws Exception {
        try {
            for (AutoCloseable dataSource : dataSources) {
                dataSource.close();
            }
        } finally {
            restoreProperty("store.db.driverClassName", originalDriver);
            restoreProperty("store.db.minConn", originalMinConn);
            EnhancedServiceLoader.unloadAll();
            ConfigurationFactory.reload();
        }
    }

    @Test
    @Order(1)
    public void testDbcpDataSourceProvider() {
        DataSource dataSource = loadDataSource(dbcpDatasourceType);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(2)
    public void testLoadMysqlDriver() {

        System.setProperty("store.db.driverClassName", mysqlJdbcDriver);
        DataSource dataSource = loadDataSource(dbcpDatasourceType);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(3)
    public void testLoadDMDriver() {
        System.setProperty("store.db.driverClassName", "dm.jdbc.driver.DmDriver");
        DataSource dataSource = loadDataSource(dbcpDatasourceType);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(4)
    public void testLoadDriverFailed() {
        System.setProperty("store.db.driverClassName", "dm.jdbc.driver.DmDriver1");
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            loadDataSource(dbcpDatasourceType);
        });
    }

    @Test
    @Order(5)
    public void testDruidDataSourceProvider() {
        DataSource dataSource = loadDataSource(druidDatasourceType);
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(6)
    public void testHikariDataSourceProvider() {
        DataSource dataSource = loadDataSource(hikariDatasourceType);
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
    public void testHikariDataSourceProviderWithIsolatedDriver() throws Exception {
        ClassLoader original = Thread.currentThread().getContextClassLoader();
        List<Driver> originalDrivers = Collections.list(DriverManager.getDrivers());
        URL driverJar =
                org.h2.Driver.class.getProtectionDomain().getCodeSource().getLocation();
        try (URLClassLoader isolated = new URLClassLoader(new URL[] {driverJar}, Driver.class.getClassLoader())) {
            Assertions.assertSame(
                    isolated, Class.forName("org.h2.Driver", true, isolated).getClassLoader());
            HikariDataSourceProvider provider = new HikariDataSourceProvider() {
                @Override
                protected ClassLoader getDriverClassLoader() {
                    return isolated;
                }

                @Override
                protected String getDriverClassName() {
                    return "org.h2.Driver";
                }

                @Override
                protected String getUrl() {
                    return "jdbc:h2:mem:isolated_driver_test";
                }

                @Override
                protected String getUser() {
                    return "sa";
                }

                @Override
                protected String getPassword() {
                    return "";
                }

                @Override
                protected DBType getDBType() {
                    return DBType.H2;
                }

                @Override
                protected int getMinConn() {
                    return 0;
                }
            };
            try (HikariDataSource dataSource = (HikariDataSource) provider.generate();
                    Connection connection = dataSource.getConnection();
                    Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("SELECT 1")) {
                Assertions.assertEquals("org.h2.Driver", dataSource.getDriverClassName());
                Assertions.assertTrue(result.next());
                Assertions.assertEquals(1, result.getInt(1));
                Assertions.assertSame(original, Thread.currentThread().getContextClassLoader());
            }
        } finally {
            for (Driver driver : Collections.list(DriverManager.getDrivers())) {
                if (!originalDrivers.contains(driver)) {
                    DriverManager.deregisterDriver(driver);
                }
            }
            Assertions.assertSame(original, Thread.currentThread().getContextClassLoader());
        }
    }

    @Test
    @Order(9)
    public void testHikariDataSourceProviderWithMySQLLegacyDriver() {
        System.setProperty("store.db.driverClassName", mysqlJdbcDriver);
        HikariDataSourceProvider provider = new HikariDataSourceProvider() {
            @Override
            protected int getMinConn() {
                return 0;
            }
        };
        try (HikariDataSource dataSource = (HikariDataSource) provider.generate()) {
            Assertions.assertEquals(mysqlJdbcDriver, dataSource.getDriverClassName());
        }
    }
}
