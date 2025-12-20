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

import org.apache.seata.console.filter.MCPAuthenticationFilter;
import org.apache.seata.mcp.core.props.MCPProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.authentication.AuthenticationManager;

@Configuration
public class MCPFiltersConfig {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private MCPProperties mcpProperties;

    private static final Logger LOGGER = LoggerFactory.getLogger(MCPFiltersConfig.class);

    @Bean
    public FilterRegistrationBean<MCPAuthenticationFilter> mcpAuthenticationFilterRegistration() {

        MCPAuthenticationFilter mcpAuthenticationFilter = new MCPAuthenticationFilter(authenticationManager);

        FilterRegistrationBean<MCPAuthenticationFilter> registration = new FilterRegistrationBean<>();

        registration.setFilter(mcpAuthenticationFilter);

        for (String endPoint : mcpProperties.getEndpoints()) {
            registration.addUrlPatterns(endPoint);
        }

        registration.setName("mcpJwtAuthenticationTokenFilter");

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);

        if (!mcpProperties.isEnableAuth())
            LOGGER.warn(
                    "The AUTH VERIFICATION of the [MCP server] is not enabled, please ensure that it is enabled as much as possible to avoid security problems");

        registration.setEnabled(mcpProperties.isEnableAuth());

        return registration;
    }
}
