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
package org.apache.seata.mcp.store;

import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ExtendWith(MockitoExtension.class)
@TestPropertySource(
        properties = {
            "seata.businessDataSources.db1.enabled=true",
            "seata.businessDataSources.db1.db-type=mysql",
            "seata.businessDataSources.db1.driverClassName=com.mysql.cj.jdbc.Driver",
            "seata.businessDataSources.db1.url=jdbc:mysql://localhost:3306/seata1?useSSL=false",
            "seata.businessDataSources.db1.username=root",
            "seata.businessDataSources.db1.password=seata",
            "seata.businessDataSources.db1.datasource=druid",
            "seata.businessDataSources.db2.enabled=true",
            "seata.businessDataSources.db2.db-type=mysql",
            "seata.businessDataSources.db2.driverClassName=com.mysql.cj.jdbc.Driver",
            "seata.businessDataSources.db2.url=jdbc:mysql://localhost:3306/seata2?useSSL=false",
            "seata.businessDataSources.db2.username=root",
            "seata.businessDataSources.db2.password=seata",
            "seata.businessDataSources.db2.datasource=hikari",
            "seata.businessDataSources.db3.enabled=true",
            "seata.businessDataSources.db3.db-type=mysql",
            "seata.businessDataSources.db3.driverClassName=com.mysql.cj.jdbc.Driver",
            "seata.businessDataSources.db3.url=jdbc:mysql://localhost:3306/seata3?useSSL=false",
            "seata.businessDataSources.db3.username=root",
            "seata.businessDataSources.db3.password=seata",
            "seata.businessDataSources.db3.datasource=dbcp",
        })
public class DataSourceFactoryTest {

    @Autowired
    private Environment env;

    @Autowired
    private BusinessDataSourcesProperties businessDataSourcesProperties;

    @Test
    public void testGetAllSupportedDataSources() {
        for (String resourceId : BusinessDataSourcesProperties.getResourceIds()) {
            DataSource ds = DataSourceFactory.getDataSource(resourceId);
            assertNotNull(ds, "DataSource for " + resourceId + " should not be null");
            String type = BusinessDataSourcesProperties.getDatasources()
                    .get(resourceId)
                    .getDatasource();
            System.out.println("DataSource type: " + type + ", url: " + resourceId);
        }
    }

    @Configuration
    static class init {
        @Bean
        public BusinessDataSourcesProperties getDataSourcesConfiguration() {
            return new BusinessDataSourcesProperties();
        }
    }

    @Test
    public void testGetDruidDataSource() {
        String druidUrl = env.getProperty("seata.businessDataSources.db1.url");
        assertNotNull(druidUrl);
        String resourceId = druidUrl.split("\\?")[0];
        String type =
                BusinessDataSourcesProperties.getDatasources().get(resourceId).getDatasource();
        assertEquals("druid", type);
        DataSource ds = DataSourceFactory.getDataSource(resourceId);
        assertNotNull(ds);
    }

    @Test
    public void testGetHikariDataSource() {
        String hikari = env.getProperty("seata.businessDataSources.db2.url");
        assertNotNull(hikari);
        String resourceId = hikari.split("\\?")[0];
        String type =
                BusinessDataSourcesProperties.getDatasources().get(resourceId).getDatasource();
        assertEquals("hikari", type);
        DataSource ds = DataSourceFactory.getDataSource(resourceId);
        assertNotNull(ds);
    }

    @Test
    public void testGetDbcpDataSource() {
        String dbcp = env.getProperty("seata.businessDataSources.db3.url");
        assertNotNull(dbcp);
        String resourceId = dbcp.split("\\?")[0];
        String type =
                BusinessDataSourcesProperties.getDatasources().get(resourceId).getDatasource();
        assertEquals("dbcp", type);
        DataSource ds = DataSourceFactory.getDataSource(resourceId);
        assertNotNull(ds);
    }
}
