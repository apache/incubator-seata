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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import java.lang.reflect.Field;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessDataSourcesPropertiesTest {

    @BeforeEach
    void setUp() throws Exception {
        clearStaticState();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearStaticState();
    }

    @Test
    void shouldRegisterDynamicDataSourceAndRejectSameNameWithDifferentUrl() throws Exception {
        BusinessDataSourcesProperties properties =
                new BusinessDataSourcesProperties(new MockEnvironment(), new ObjectMapper());
        String config = config("biz", "jdbc:h2:mem:biz");

        properties.registerDataSourceFromJson(config);
        properties.registerDataSourceFromJson(config);

        assertEquals(
                "jdbc:h2:mem:biz",
                BusinessDataSourcesProperties.getDataSourcesNamesAndResourceIds()
                        .get("biz"));
        assertEquals(1, BusinessDataSourcesProperties.getDatasources().size());
        assertThrows(
                IllegalArgumentException.class,
                () -> properties.registerDataSourceFromJson(config("biz", "jdbc:h2:mem:other")));
    }

    @Test
    void shouldLimitDynamicDataSourceCount() throws Exception {
        MockEnvironment env = new MockEnvironment().withProperty("seata.businessDataSources.max-dynamic-size", "1");
        BusinessDataSourcesProperties properties = new BusinessDataSourcesProperties(env, new ObjectMapper());

        properties.registerDataSourceFromJson(config("biz1", "jdbc:h2:mem:biz1"));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> properties.registerDataSourceFromJson(config("biz2", "jdbc:h2:mem:biz2")));
        assertEquals("The number of dynamic business data sources exceeds the limit: 1", exception.getMessage());
    }

    private String config(String name, String url) {
        return "{\"dbName\":\"" + name
                + "\",\"dbType\":\"h2\",\"url\":\"" + url
                + "\",\"username\":\"sa\",\"password\":\"pwd\",\"minConn\":1,\"maxConn\":2}";
    }

    @SuppressWarnings("unchecked")
    private void clearStaticState() throws Exception {
        BusinessDataSourcesProperties.getDatasources().clear();
        BusinessDataSourcesProperties.getDataSourcesNamesAndResourceIds().clear();
        Field field = BusinessDataSourcesProperties.class.getDeclaredField("dynamicResourceIds");
        field.setAccessible(true);
        ((Set<String>) field.get(null)).clear();
    }
}
