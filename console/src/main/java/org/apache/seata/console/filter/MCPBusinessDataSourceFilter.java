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

import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MCPBusinessDataSourceFilter extends OncePerRequestFilter {

    private final BusinessDataSourcesProperties businessDataSourcesProperties;

    private final Set<String> processedConfigs = ConcurrentHashMap.newKeySet();

    public MCPBusinessDataSourceFilter(BusinessDataSourcesProperties properties) {
        this.businessDataSourcesProperties = properties;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull FilterChain filterChain)
            throws ServletException, IOException {
        String combinedHeader = request.getHeader("X-DB-Config");
        if (combinedHeader != null && !combinedHeader.isEmpty()) {
            String[] jsonConfigs = combinedHeader.split(";");
            for (String jsonDBConfig : jsonConfigs) {
                if (processedConfigs.contains(jsonDBConfig.trim())) {
                    continue;
                }
                try {
                    businessDataSourcesProperties.registerDataSourceFromJson(jsonDBConfig.trim());
                    processedConfigs.add(jsonDBConfig.trim());
                } catch (Exception e) {
                    if (!response.isCommitted()) {
                        response.sendError(
                                HttpStatus.BAD_REQUEST.value(),
                                "The business database parameter in the request header is incorrect: "
                                        + e.getMessage());
                        return;
                    }
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
