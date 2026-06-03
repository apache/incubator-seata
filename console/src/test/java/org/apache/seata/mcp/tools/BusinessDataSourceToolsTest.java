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
package org.apache.seata.mcp.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.mcp.entity.dto.MysqlDataSourceRegisterRequest;
import org.apache.seata.mcp.entity.vo.MysqlDataSourceInfo;
import org.apache.seata.mcp.service.BusinessDataSourceService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springaicommunity.mcp.annotation.McpTool;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BusinessDataSourceToolsTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldRejectDynamicRegistrationForNonAdminAuthority() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceTools tools = new BusinessDataSourceTools(service, new ObjectMapper());
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "pwd", "ROLE_USER");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThrows(AccessDeniedException.class, () -> tools.registerDataSource(new MysqlDataSourceRegisterRequest()));
    }

    @Test
    void shouldAllowDynamicRegistrationForAdminAuthority() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceTools tools = new BusinessDataSourceTools(service, new ObjectMapper());
        MysqlDataSourceRegisterRequest request = new MysqlDataSourceRegisterRequest();
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("admin", "pwd", "ROLE_ADMIN");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(service.registerMysqlDataSource(request)).thenReturn("business-ds://biz");

        tools.registerDataSource(request);

        verify(service).registerMysqlDataSource(request);
    }

    @Test
    void shouldExposeToolNamesWithoutMysqlPrefix() {
        Set<String> toolNames = Arrays.stream(BusinessDataSourceTools.class.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(McpTool.class))
                .map(this::toolName)
                .collect(Collectors.toSet());

        assertEquals(
                new HashSet<>(Arrays.asList(
                        "registerDataSource",
                        "unregisterDataSource",
                        "testDataSource",
                        "getDataSources",
                        "getTableNames",
                        "getTableSchema",
                        "queryTable",
                        "explainSql",
                        "runSql")),
                toolNames);
    }

    @Test
    void shouldSerializeDataSourceResourceWithoutSensitiveFields() {
        BusinessDataSourceService service = mock(BusinessDataSourceService.class);
        BusinessDataSourceTools tools = new BusinessDataSourceTools(service, new ObjectMapper());
        MysqlDataSourceInfo info = new MysqlDataSourceInfo();
        info.setName("biz");
        info.setResourceId("business-ds://biz");
        info.setDatasource("druid");
        when(service.getMysqlDataSources()).thenReturn(Collections.singletonList(info));

        String json = tools.mysqlDataSourcesResource();

        assertFalse(json.contains("jdbc:mysql://"));
        assertFalse(json.contains("username"));
        assertFalse(json.contains("password"));
    }

    private String toolName(Method method) {
        McpTool annotation = method.getAnnotation(McpTool.class);
        return annotation.name().isEmpty() ? method.getName() : annotation.name();
    }
}
