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
package org.apache.seata.namingserver.config;

import javax.servlet.Filter;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.seata.namingserver.filter.ConsoleRemotingFilter;
import org.apache.seata.namingserver.manager.NamingManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.HttpComponentsAsyncClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;


import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_CONNECTION_MAX_PER_ROUTE;
import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_CONNECTION_MAX_TOTAL;
import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_REQUEST_TIMEOUT;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        // Create a connection manager with custom settings
        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager();
        connectionManager.setMaxTotal(DEFAULT_CONNECTION_MAX_TOTAL); // Maximum total connections
        connectionManager.setDefaultMaxPerRoute(DEFAULT_CONNECTION_MAX_PER_ROUTE); // Maximum connections per route
        // Create an HttpClient with the connection manager
        CloseableHttpClient httpClient = HttpClients.custom().setConnectionManager(connectionManager).build();
        // Create a request factory with the HttpClient
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(DEFAULT_REQUEST_TIMEOUT); // Connection timeout in milliseconds
        requestFactory.setReadTimeout(DEFAULT_REQUEST_TIMEOUT); // Read timeout in milliseconds
        // Create and return a RestTemplate with the custom request factory
        return new RestTemplate(requestFactory);
    }

    @Bean
    public AsyncRestTemplate asyncRestTemplate(RestTemplate restTemplate) {
        HttpComponentsAsyncClientHttpRequestFactory asyncClientHttpRequestFactory =
            new HttpComponentsAsyncClientHttpRequestFactory();
        asyncClientHttpRequestFactory.setConnectionRequestTimeout(DEFAULT_REQUEST_TIMEOUT); // Connection request timeout in milliseconds
        asyncClientHttpRequestFactory.setConnectTimeout(DEFAULT_REQUEST_TIMEOUT); // Connection timeout in milliseconds
        asyncClientHttpRequestFactory.setReadTimeout(DEFAULT_REQUEST_TIMEOUT); // Read timeout in milliseconds
        return new AsyncRestTemplate(asyncClientHttpRequestFactory, restTemplate);
    }

    @Bean
    public FilterRegistrationBean<Filter> consoleRemotingFilter(NamingManager namingManager,
        AsyncRestTemplate asyncRestTemplate) {
        ConsoleRemotingFilter consoleRemotingFilter = new ConsoleRemotingFilter(namingManager, asyncRestTemplate);
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(consoleRemotingFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

}
