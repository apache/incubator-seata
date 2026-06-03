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

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.mcp.core.props.BusinessDataSourcesProperties;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class MCPBusinessDataSourceFilter extends OncePerRequestFilter {

    private final BusinessDataSourcesProperties businessDataSourcesProperties;

    private final List<RequestMatcher> mcpEndpointMatchers;

    public MCPBusinessDataSourceFilter(BusinessDataSourcesProperties properties, List<String> mcpEndpoints) {
        this.businessDataSourcesProperties = properties;
        this.mcpEndpointMatchers = mcpEndpoints.stream().map(AntPathRequestMatcher::new).collect(Collectors.toList());
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return mcpEndpointMatchers.stream().noneMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String combinedHeader = request.getHeader("X-DB-Config");
        if (combinedHeader != null && !combinedHeader.isEmpty()) {
            if (!isAuthenticated()) {
                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Authentication is required");
                return;
            }
            String[] jsonConfigs = combinedHeader.split(";");
            for (String jsonDBConfig : jsonConfigs) {
                try {
                    businessDataSourcesProperties.registerDataSourceFromJson(jsonDBConfig.trim());
                } catch (Exception e) {
                    if (!response.isCommitted()) {
                        response.sendError(
                                HttpStatus.BAD_REQUEST.value(),
                                "The business database parameter in the request header is incorrect: "
                                        + e.getMessage());
                    }
                    return;
                }
            }
        }
        filterChain.doFilter(request, response);
    }

    private boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }
}
