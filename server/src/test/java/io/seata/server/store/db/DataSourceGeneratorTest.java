/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.store.db;

import com.alibaba.druid.pool.DruidDataSource;
import io.seata.common.loader.EnhancedServiceLoader;
import io.seata.core.store.db.DataSourceGenerator;
import org.apache.commons.dbcp2.BasicDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;

public class DataSourceGeneratorTest {
    @Test
    public void testDbcpGenerateDataSource() {
        DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class, "dbcp");
        BasicDataSource dataSource = (BasicDataSource) dataSourceGenerator.generateDataSource();
        ClassLoader driverClassLoader = dataSource.getDriverClassLoader();
        Assertions.assertNotNull(driverClassLoader);
    }

    @Test
    public void testDruidGenerateDataSource() {
        DataSourceGenerator dataSourceGenerator = EnhancedServiceLoader.load(DataSourceGenerator.class, "druid");
        DruidDataSource dataSource = (DruidDataSource) dataSourceGenerator.generateDataSource();
        ClassLoader driverClassLoader = dataSource.getDriverClassLoader();
        Assertions.assertNotNull(driverClassLoader);
        try (Connection conn = dataSource.getConnection()) {
            Assertions.assertNotNull(conn);
        } catch (SQLException ignore) {
        } finally {
            Driver driver = dataSource.getDriver();
            Assertions.assertNotNull(driver);
            Assertions.assertEquals(driverClassLoader, driver.getClass().getClassLoader());
        }
    }
}
