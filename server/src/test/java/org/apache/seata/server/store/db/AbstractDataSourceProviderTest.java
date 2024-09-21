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
import org.apache.seata.core.store.db.DataSourceProvider;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 */
@SpringBootTest
public class AbstractDataSourceProviderTest {

    private final String dbcpDatasourceType = "dbcp";

    private final String druidDatasourceType = "druid";

    private final String hikariDatasourceType = "hikari";

    private final String mysqlJdbcDriver = "com.mysql.jdbc.Driver";

    @Test
    public void testDbcpDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, dbcpDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    public void testDruidDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, druidDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    public void testHikariDataSourceProvider() {
        DataSource dataSource = EnhancedServiceLoader.load(DataSourceProvider.class, hikariDatasourceType).provide();
        Assertions.assertNotNull(dataSource);
    }

    @Test
    public void testMySQLDataSourceProvider() throws ClassNotFoundException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        Class<?> driverClass = Class.forName(mysqlJdbcDriver, true, classLoader);
        Assertions.assertNotNull(driverClass);
    }
}
