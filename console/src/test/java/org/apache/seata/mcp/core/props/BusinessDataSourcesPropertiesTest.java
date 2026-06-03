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
package org.apache.seata.mcp.core.props;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.core.secret.EnvSecretResolver;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessDataSourcesPropertiesTest {

    @BeforeEach
    void setUp() {
        BusinessDataSourcesProperties.clear();
    }

    @AfterEach
    void tearDown() {
        BusinessDataSourcesProperties.clear();
    }

    @Test
    void shouldRejectDynamicRegistrationWhenDisabledByDefault() {
        BusinessDataSourcesProperties properties =
                newProperties(new MockEnvironment().withProperty("MYSQL_PASS", "pwd"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request("biz", "localhost")));

        assertEquals("Dynamic business data source registration is disabled", exception.getMessage());
    }

    @Test
    void shouldResolvePasswordSecretRefAndRegisterMysqlResourceId() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv().withProperty("MYSQL_PASS", "pwd"));

        String resourceId = properties.registerMysqlDataSource(request("biz", "localhost"));

        assertEquals("business-ds://biz", resourceId);
        BusinessDataSourcesProperties.DataSourceProperties props =
                BusinessDataSourcesProperties.getDatasources().get(resourceId);
        assertEquals("pwd", props.getPassword());
        assertEquals("MYSQL_PASS", props.getPasswordSecretRef());
        assertEquals("app", props.getDatabaseName());
        assertEquals(
                "business-ds://biz",
                BusinessDataSourcesProperties.getDataSourcesNamesAndResourceIds()
                        .get("biz"));
        assertEquals(1, properties.getMysqlDataSourceInfos().size());
    }

    @Test
    void shouldRequirePasswordSecretRefForDynamicRegistration() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv());
        MysqlDataSourceRegisterRequest request = request("biz", "localhost");
        request.setPasswordSecretRef("");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request));

        assertEquals("passwordSecretRef cannot be empty", exception.getMessage());
    }

    @Test
    void shouldRejectNonMysqlJdbcUrl() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv().withProperty("MYSQL_PASS", "pwd"));
        MysqlDataSourceRegisterRequest request = request("biz", "localhost");
        request.setUrl("jdbc:postgresql://localhost:5432/app");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request));

        assertEquals("Only jdbc:mysql:// URL is supported", exception.getMessage());
    }

    @Test
    void shouldRejectMysqlJdbcUrlWithoutDatabaseName() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv().withProperty("MYSQL_PASS", "pwd"));
        MysqlDataSourceRegisterRequest request = request("biz", "localhost");
        request.setUrl("jdbc:mysql://localhost:3306");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request));

        assertEquals("MySQL JDBC URL must include a database name", exception.getMessage());
    }

    @Test
    void shouldRejectSystemDatabaseInMysqlJdbcUrl() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv().withProperty("MYSQL_PASS", "pwd"));
        MysqlDataSourceRegisterRequest request = request("biz", "localhost");
        request.setUrl("jdbc:mysql://localhost:3306/information_schema");

        IllegalArgumentException exception =
                assertThrows(IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request));

        assertEquals("MySQL JDBC URL database is not allowed: information_schema", exception.getMessage());
    }

    @Test
    void shouldApplyHostAllowlist() {
        MockEnvironment env = enabledEnv()
                .withProperty("MYSQL_PASS", "pwd")
                .withProperty("seata.businessDataSources.dynamic-registration.allowed-hosts", "db.example.com");
        BusinessDataSourcesProperties properties = newProperties(env);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request("biz", "localhost")));

        assertEquals("MySQL host is not allowed: localhost", exception.getMessage());
        assertEquals("business-ds://biz2", properties.registerMysqlDataSource(request("biz2", "db.example.com")));
    }

    @Test
    void shouldRejectDuplicateDataSourceName() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv().withProperty("MYSQL_PASS", "pwd"));

        properties.registerMysqlDataSource(request("biz", "localhost"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> properties.registerMysqlDataSource(request("biz", "localhost")));

        assertEquals("The data source name has already been registered: biz", exception.getMessage());
    }

    @Test
    void shouldUnregisterDynamicDataSourceAndCleanConfig() {
        BusinessDataSourcesProperties properties = newProperties(enabledEnv().withProperty("MYSQL_PASS", "pwd"));
        properties.registerMysqlDataSource(request("biz", "localhost"));

        String resourceId = properties.unregisterMysqlDataSource("biz");

        assertEquals("business-ds://biz", resourceId);
        assertFalse(BusinessDataSourcesProperties.getDatasources().containsKey(resourceId));
        assertFalse(BusinessDataSourcesProperties.getDataSourcesNamesAndResourceIds()
                .containsKey("biz"));
        assertFalse(BusinessDataSourcesProperties.getDynamicResourceIds().contains(resourceId));
    }

    private BusinessDataSourcesProperties newProperties(MockEnvironment env) {
        return new BusinessDataSourcesProperties(env, new ObjectMapper(), new EnvSecretResolver(env));
    }

    private MockEnvironment enabledEnv() {
        return new MockEnvironment().withProperty("seata.businessDataSources.dynamic-registration.enabled", "true");
    }

    private MysqlDataSourceRegisterRequest request(String name, String host) {
        MysqlDataSourceRegisterRequest request = new MysqlDataSourceRegisterRequest();
        request.setName(name);
        request.setUrl("jdbc:mysql://" + host + ":3306/app");
        request.setUsername("readonly");
        request.setPasswordSecretRef("MYSQL_PASS");
        request.setMinConn(1);
        request.setMaxConn(2);
        return request;
    }
}
