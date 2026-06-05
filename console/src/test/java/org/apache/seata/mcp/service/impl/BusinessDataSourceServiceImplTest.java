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
package org.apache.seata.mcp.service.impl;

import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.BusinessQueryResult;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceTestResult;
import org.apache.seata.mcp.service.MysqlMetadataService;
import org.apache.seata.mcp.store.DataSourceFactory;
import org.apache.seata.mcp.store.SqlExecutionTemplate;
import org.apache.seata.mcp.store.SqlSafetyValidator;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessDataSourceServiceImplTest {

    @Test
    void shouldRemovePoolWhenUnregisteringDynamicDataSource() {
        SqlExecutionTemplate sqlExecutionTemplate = mock(SqlExecutionTemplate.class);
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        MysqlMetadataService metadataService = mock(MysqlMetadataService.class);
        BusinessDataSourceServiceImpl service = new BusinessDataSourceServiceImpl(
                sqlExecutionTemplate, properties, metadataService, new SqlSafetyValidator());
        when(properties.unregisterMysqlDataSource("biz")).thenReturn("business-ds://biz");

        try (MockedStatic<DataSourceFactory> dataSourceFactory = mockStatic(DataSourceFactory.class)) {
            String resourceId = service.unregisterMysqlDataSource("biz");

            assertEquals("business-ds://biz", resourceId);
            dataSourceFactory.verify(() -> DataSourceFactory.removeDataSource("business-ds://biz"));
        }
        verify(properties).unregisterMysqlDataSource("biz");
    }

    @Test
    void shouldQueryTableWithinUrlDatabase() {
        SqlExecutionTemplate sqlExecutionTemplate = mock(SqlExecutionTemplate.class);
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        MysqlMetadataService metadataService = mock(MysqlMetadataService.class);
        BusinessQueryResult result = new BusinessQueryResult();
        BusinessDataSourceServiceImpl service = new BusinessDataSourceServiceImpl(
                sqlExecutionTemplate, properties, metadataService, new SqlSafetyValidator());
        when(properties.getDatabaseName("business-ds://biz")).thenReturn("app");
        when(sqlExecutionTemplate.queryWithMaxRows("business-ds://biz", "SELECT `id`, `name` FROM `app`.`users`", 10))
                .thenReturn(result);

        BusinessQueryResult actual = service.queryMysqlTable(
                "business-ds://biz", "users", Arrays.asList("id", "name"), Collections.emptyMap(), 10);

        assertEquals(result, actual);
        verify(sqlExecutionTemplate)
                .queryWithMaxRows("business-ds://biz", "SELECT `id`, `name` FROM `app`.`users`", 10);
    }

    @Test
    void shouldExposeValidationMessageWhenTestingDataSource() {
        SqlExecutionTemplate sqlExecutionTemplate = mock(SqlExecutionTemplate.class);
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        MysqlMetadataService metadataService = mock(MysqlMetadataService.class);
        BusinessDataSourceServiceImpl service = new BusinessDataSourceServiceImpl(
                sqlExecutionTemplate, properties, metadataService, new SqlSafetyValidator());
        MysqlDataSourceRegisterRequest request = new MysqlDataSourceRegisterRequest();
        when(properties.buildDynamicMysqlProperties(request))
                .thenThrow(new IllegalArgumentException("MySQL host is not allowed: 127.0.0.1"));

        MysqlDataSourceTestResult result = service.testMysqlDataSource(request);

        assertEquals("MySQL host is not allowed: 127.0.0.1", result.getMessage());
    }
}
