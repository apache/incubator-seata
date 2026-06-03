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
package org.apache.seata.console.filter;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class MCPBusinessDataSourceFilterTest {

    private static final String DB_CONFIG =
            "{\"dbName\":\"biz\",\"dbType\":\"h2\",\"url\":\"jdbc:h2:mem:biz\",\"username\":\"sa\",\"password\":\"pwd\"}";

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldOnlyProcessMcpEndpoints() throws Exception {
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        MCPBusinessDataSourceFilter filter = new MCPBusinessDataSourceFilter(properties, List.of("/mcp/**"));
        MockHttpServletRequest request = request("POST", "/api/v1/console/users");
        request.addHeader("X-DB-Config", DB_CONFIG);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain(statusServlet(HttpServletResponse.SC_NO_CONTENT)));

        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
        verify(properties, never()).registerDataSourceFromJson(DB_CONFIG);
    }

    @Test
    void shouldRejectUnauthenticatedMcpDataSourceRegistration() throws Exception {
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        MCPBusinessDataSourceFilter filter = new MCPBusinessDataSourceFilter(properties, List.of("/mcp/**"));
        MockHttpServletRequest request = request("POST", "/mcp/message");
        request.addHeader("X-DB-Config", DB_CONFIG);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain(statusServlet(HttpServletResponse.SC_NO_CONTENT)));

        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
        verify(properties, never()).registerDataSourceFromJson(DB_CONFIG);
    }

    @Test
    void shouldRegisterDataSourceForAuthenticatedMcpRequest() throws Exception {
        BusinessDataSourcesProperties properties = mock(BusinessDataSourcesProperties.class);
        MCPBusinessDataSourceFilter filter = new MCPBusinessDataSourceFilter(properties, List.of("/mcp/**"));
        MockHttpServletRequest request = request("POST", "/mcp/message");
        request.addHeader("X-DB-Config", DB_CONFIG);
        MockHttpServletResponse response = new MockHttpServletResponse();
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken("user", "token", Collections.emptyList()));

        filter.doFilter(request, response, new MockFilterChain(statusServlet(HttpServletResponse.SC_NO_CONTENT)));

        assertEquals(HttpServletResponse.SC_NO_CONTENT, response.getStatus());
        verify(properties).registerDataSourceFromJson(DB_CONFIG);
    }

    private Servlet statusServlet(int status) {
        return new Servlet() {
            @Override
            public void init(ServletConfig config) {}

            @Override
            public ServletConfig getServletConfig() {
                return null;
            }

            @Override
            public void service(ServletRequest request, ServletResponse response) {
                ((HttpServletResponse) response).setStatus(status);
            }

            @Override
            public String getServletInfo() {
                return null;
            }

            @Override
            public void destroy() {}
        };
    }

    private MockHttpServletRequest request(String method, String servletPath) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, servletPath);
        request.setServletPath(servletPath);
        return request;
    }
}
