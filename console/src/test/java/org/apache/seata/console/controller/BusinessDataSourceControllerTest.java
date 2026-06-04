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
package org.apache.seata.console.controller;

import org.apache.seata.common.result.SingleResult;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceTestResult;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessDataSourceControllerTest {

    @Test
    void shouldListDataSourcesWithoutSensitiveFields() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceController controller = new BusinessDataSourceController(service);
        MysqlDataSourceInfo info = new MysqlDataSourceInfo();
        info.setName("shopDemo");
        info.setResourceId("business-ds://shopDemo");
        info.setDatabaseName("mcp_shop_demo");
        when(service.getMysqlDataSources()).thenReturn(Collections.singletonList(info));

        SingleResult<List<MysqlDataSourceInfo>> result = controller.listDataSources();

        assertTrue(result.isSuccess());
        assertEquals("shopDemo", result.getData().get(0).getName());
    }

    @Test
    void shouldRegisterDataSourceThroughConsoleApi() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceController controller = new BusinessDataSourceController(service);
        MysqlDataSourceRegisterRequest request = new MysqlDataSourceRegisterRequest();
        request.setPassword("pwd");
        when(service.registerMysqlDataSource(request)).thenReturn("business-ds://shopDemo");

        SingleResult<String> result = controller.registerDataSource(request);

        assertTrue(result.isSuccess());
        assertEquals("business-ds://shopDemo", result.getData());
        verify(service).registerMysqlDataSource(request);
    }

    @Test
    void shouldReturnFailureWhenRegisterFails() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceController controller = new BusinessDataSourceController(service);
        MysqlDataSourceRegisterRequest request = new MysqlDataSourceRegisterRequest();
        when(service.registerMysqlDataSource(request)).thenThrow(new IllegalArgumentException("bad datasource"));

        SingleResult<String> result = controller.registerDataSource(request);

        assertFalse(result.isSuccess());
        assertEquals("bad datasource", result.getMessage());
    }

    @Test
    void shouldTestDataSourceThroughConsoleApi() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceController controller = new BusinessDataSourceController(service);
        MysqlDataSourceRegisterRequest request = new MysqlDataSourceRegisterRequest();
        MysqlDataSourceTestResult testResult = new MysqlDataSourceTestResult();
        testResult.setSuccess(true);
        when(service.testMysqlDataSource(request)).thenReturn(testResult);

        SingleResult<MysqlDataSourceTestResult> result = controller.testDataSource(request);

        assertTrue(result.isSuccess());
        assertTrue(result.getData().isSuccess());
    }

    @Test
    void shouldUnregisterDataSourceThroughConsoleApi() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceController controller = new BusinessDataSourceController(service);
        when(service.unregisterMysqlDataSource("shopDemo")).thenReturn("business-ds://shopDemo");

        SingleResult<String> result = controller.unregisterDataSource("shopDemo");

        assertTrue(result.isSuccess());
        assertEquals("business-ds://shopDemo", result.getData());
        verify(service).unregisterMysqlDataSource("shopDemo");
    }
}
