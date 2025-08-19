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
package org.apache.seata.mcp.config;

import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.test.context.ContextConfiguration;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;

import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MAX_CONN;
import static org.apache.seata.common.DefaultValues.DEFAULT_DB_MIN_CONN;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ContextConfiguration(initializers = BusinessDataSourcesPropertiesTest.PropertyInitializer.class)
public class BusinessDataSourcesPropertiesTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Autowired
    private BusinessDataSourcesProperties configuration;

    // Use reflection to clear static fields to ensure that tests don't affect each other
    @BeforeEach
    @AfterEach
    public void clearStaticDatasources() throws Exception {
        Field datasourcesField = BusinessDataSourcesProperties.class.getDeclaredField("datasources");
        datasourcesField.setAccessible(true);
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                (Map<String, BusinessDataSourcesProperties.DataSourceProperties>) datasourcesField.get(null);
        datasources.clear();
    }

    // Custom property initializer
    public static class PropertyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext context) {
            TestPropertyValues values = TestPropertyValues.of(
                    // The first data source configuration
                    "seata.datasources.db1.enabled=true",
                    "seata.datasources.db1.dbType=mysql",
                    "seata.datasources.db1.driverClassName=com.mysql.cj.jdbc.Driver",
                    "seata.datasources.db1.url=jdbc:mysql://localhost:3306/db1",
                    "seata.datasources.db1.username=user1",
                    "seata.datasources.db1.password=pass1",
                    "seata.datasources.db1.datasource=druid",
                    "seata.datasources.db1.minConn=5",
                    "seata.datasources.db1.maxConn=20",
                    "seata.datasources.db1.maxWait=3000",

                    // The second data source configuration
                    "seata.datasources.db2.url=jdbc:mysql://localhost:3306/db2",
                    "seata.datasources.db2.username=user2",
                    "seata.datasources.db2.password=pass2",

                    // The Third data source configuration
                    "seata.datasources.db3.enabled=false",
                    "seata.datasources.db3.url=jdbc:mysql://localhost:3306/db3",

                    // Incomplete data sources
                    "seata.datasources.db4.dbType=postgresql");
            values.applyTo(context);
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        public BusinessDataSourcesProperties dataSourcesConfiguration(Environment env) {
            return new BusinessDataSourcesProperties();
        }
    }

    @Test
    public void testDataSourcesInitialization() throws Exception {
        // Initialization is triggered manually
        configuration.afterPropertiesSet();

        // Verify the data source mapping
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                BusinessDataSourcesProperties.getDatasources();

        assertNotNull(datasources);

        assertEquals(3, datasources.size());

        // Verify the resource ID
        Set<String> resourceIds = BusinessDataSourcesProperties.getResourceIds();
        assertEquals(3, resourceIds.size());
        assertTrue(resourceIds.contains("jdbc:mysql://localhost:3306/db1"));
        assertTrue(resourceIds.contains("jdbc:mysql://localhost:3306/db2"));
        // db3 is disabled, but its resource ID is still loaded
        assertTrue(resourceIds.contains("jdbc:mysql://localhost:3306/db3"));
    }

    @Test
    public void testCompleteDataSourceProperties() throws Exception {
        configuration.afterPropertiesSet();
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                BusinessDataSourcesProperties.getDatasources();

        BusinessDataSourcesProperties.DataSourceProperties db1 = datasources.get("jdbc:mysql://localhost:3306/db1");

        assertNotNull(db1);
        assertTrue(db1.isEnabled());
        assertEquals("mysql", db1.getDbType());
        assertEquals("com.mysql.cj.jdbc.Driver", db1.getDriverClassName());
        assertEquals("jdbc:mysql://localhost:3306/db1", db1.getUrl());
        assertEquals("user1", db1.getUsername());
        assertEquals("pass1", db1.getPassword());
        assertEquals("druid", db1.getDatasource());
        assertEquals(5, db1.getMinConn());
        assertEquals(20, db1.getMaxConn());
        assertEquals(3000L, db1.getMaxWait());
    }

    @Test
    public void testPartialDataSourceProperties() throws Exception {
        configuration.afterPropertiesSet();
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                BusinessDataSourcesProperties.getDatasources();

        BusinessDataSourcesProperties.DataSourceProperties db2 = datasources.get("jdbc:mysql://localhost:3306/db2");

        assertNotNull(db2);
        assertTrue(db2.isEnabled());
        assertEquals("mysql", db2.getDbType());
        assertEquals("com.mysql.cj.jdbc.Driver", db2.getDriverClassName());
        assertEquals("user2", db2.getUsername());
        assertEquals("pass2", db2.getPassword());
        assertEquals("druid", db2.getDatasource());
        assertEquals(DEFAULT_DB_MIN_CONN, db2.getMinConn());
        assertEquals(DEFAULT_DB_MAX_CONN, db2.getMaxConn());
        assertEquals(5000L, db2.getMaxWait());
    }

    @Test
    public void testDisabledDataSource() throws Exception {
        configuration.afterPropertiesSet();
        Map<String, BusinessDataSourcesProperties.DataSourceProperties> datasources =
                BusinessDataSourcesProperties.getDatasources();

        BusinessDataSourcesProperties.DataSourceProperties db3 = datasources.get("jdbc:mysql://localhost:3306/db3");

        assertNotNull(db3);
        assertFalse(db3.isEnabled());
    }

    @Test
    public void testDataSourcePropertiesMethods() throws Exception {
        // Test the methods of the DataSourceProperties class
        BusinessDataSourcesProperties.DataSourceProperties props =
                new BusinessDataSourcesProperties.DataSourceProperties();

        assertTrue(props.isEnabled());
        assertEquals("mysql", props.getDbType());
        assertEquals("com.mysql.cj.jdbc.Driver", props.getDriverClassName());
        assertEquals("jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true", props.getUrl());
        assertEquals("mysql", props.getUsername());
        assertEquals("mysql", props.getPassword());
        assertEquals("druid", props.getDatasource());
        assertEquals(DEFAULT_DB_MIN_CONN, props.getMinConn());
        assertEquals(DEFAULT_DB_MAX_CONN, props.getMaxConn());
        assertEquals(5000L, props.getMaxWait());

        props.setEnabled(false);
        props.setDbType("postgresql");
        props.setDriverClassName("org.postgresql.Driver");
        props.setUrl("jdbc:postgresql://localhost:5432/testdb");
        props.setUsername("postgres");
        props.setPassword("postgres");
        props.setDatasource("hikari");
        props.setMinConn(10);
        props.setMaxConn(30);
        props.setMaxWait(2000L);

        assertFalse(props.isEnabled());
        assertEquals("postgresql", props.getDbType());
        assertEquals("org.postgresql.Driver", props.getDriverClassName());
        assertEquals("jdbc:postgresql://localhost:5432/testdb", props.getUrl());
        assertEquals("postgres", props.getUsername());
        assertEquals("postgres", props.getPassword());
        assertEquals("hikari", props.getDatasource());
        assertEquals(10, props.getMinConn());
        assertEquals(30, props.getMaxConn());
        assertEquals(2000L, props.getMaxWait());
    }

    @Test
    public void testGetOriginUrl() throws Exception {
        // Test the method of getting the original URL
        Method field = BusinessDataSourcesProperties.class.getDeclaredMethod("getOriginUrl", String.class);
        field.setAccessible(true);

        String url1 = "jdbc:mysql://localhost:3306/testdb?useSSL=false";
        String url2 = "jdbc:mysql://localhost:3306/testdb";
        String url3 = "jdbc:postgresql://localhost:5432/testdb?currentSchema=public&ssl=true";

        assertEquals("jdbc:mysql://localhost:3306/testdb", field.invoke(configuration, url1));
        assertEquals("jdbc:mysql://localhost:3306/testdb", field.invoke(configuration, url2));
        assertEquals("jdbc:postgresql://localhost:5432/testdb", field.invoke(configuration, url3));
    }
}
