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

import javax.sql.DataSource;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.store.db.DataSourceProvider;
import org.apache.seata.server.lock.LockerManagerFactory;
import org.apache.seata.server.session.SessionHolder;
import org.junit.After;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

/**
 */
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
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
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(2)
    public void testLoadMysqlDriver() {
        System.setProperty("loader.path", "/tmp");
        System.setProperty("store.db.driverClassName", mysqlJdbcDriver);
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
        System.setProperty("store.db.driverClassName", mysql8JdbcDriver);
        dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(3)
    public void testLoadDMDriver() {
        System.setProperty("store.db.driverClassName", "dm.jdbc.driver.DmDriver");
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(4)
    public void testLoadDriverFailed() {
        System.setProperty("store.db.driverClassName", "dm.jdbc.driver.DmDriver1");
        Assertions.assertThrows(EnhancedServiceNotFoundException.class, () -> {
            EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        });
    }

    @Test
    @Order(5)
    public void testDruidDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, druidDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(6)
    public void testHikariDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, hikariDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    @Order(7)
    public void testMySQLDataSourceProvider() throws ClassNotFoundException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Class<?> driverClass = Class.forName(mysqlJdbcDriver, true, classLoader);
        Assertions.assertNotNull(driverClass);
    }

}
