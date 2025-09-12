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
package org.apache.seata.console.config;

import org.apache.seata.console.filter.MCPBusinessDataSourceFilter;
import org.apache.seata.console.filter.MCPJwtAuthenticationTokenFilter;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.entity.pojo.BusinessDataSourcesProperties;
import org.apache.seata.mcp.entity.pojo.MCPProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

@Configuration
public class MCPFiltersConfig {

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    @Autowired
    private MCPProperties mcpProperties;

    @Autowired
    private BusinessDataSourcesProperties properties;

    @Bean
    public FilterRegistrationBean<MCPJwtAuthenticationTokenFilter> mcpJwtAuthenticationTokenFilterRegistration() {

        MCPJwtAuthenticationTokenFilter mcpJwtAuthenticationTokenFilter =
                new MCPJwtAuthenticationTokenFilter(jwtTokenUtils);

        FilterRegistrationBean<MCPJwtAuthenticationTokenFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(mcpJwtAuthenticationTokenFilter);

        if (mcpProperties.isSseType()) {
            MCPProperties.SseServerProperties sseServerProperties = mcpProperties.getSseServerProperties();
            registration.addUrlPatterns(sseServerProperties.getSseEndpoint(), sseServerProperties.getMessageEndpoint());
        } else {
            registration.addUrlPatterns(mcpProperties.getStreamableProperties().getMcpEndPoint());
        }

        registration.setName("mcpJwtAuthenticationTokenFilter");

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);

        registration.setEnabled(mcpProperties.isEnableAuth());

        return registration;
    }

    @Bean
    public FilterRegistrationBean<MCPBusinessDataSourceFilter> mcpDataSourceFilterRegistration() {

        MCPBusinessDataSourceFilter mcpBusinessDataSourceFilter = new MCPBusinessDataSourceFilter(properties);

        FilterRegistrationBean<MCPBusinessDataSourceFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(mcpBusinessDataSourceFilter);

        if (mcpProperties.isSseType()) {
            MCPProperties.SseServerProperties sseServerProperties = mcpProperties.getSseServerProperties();
            registration.addUrlPatterns(sseServerProperties.getSseEndpoint(), sseServerProperties.getMessageEndpoint());
        } else {
            registration.addUrlPatterns(mcpProperties.getStreamableProperties().getMcpEndPoint());
        }

        registration.setName("mcpBusinessDataSourceFilter");

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 11);

        registration.setEnabled(true);

        return registration;
    }
}
