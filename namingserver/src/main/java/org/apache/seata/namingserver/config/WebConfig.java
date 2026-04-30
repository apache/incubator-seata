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

import jakarta.servlet.Filter;
import org.apache.seata.namingserver.filter.ConsoleRemotingFilter;
import org.apache.seata.namingserver.manager.NamingManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_REQUEST_TIMEOUT;
import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_WRITE_TIMEOUT;

@Configuration
public class WebConfig {

    @Bean
    public RestClient restClient(RestClient.Builder restClientBuilder) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(DEFAULT_REQUEST_TIMEOUT))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofMillis(Math.max(DEFAULT_REQUEST_TIMEOUT, DEFAULT_WRITE_TIMEOUT)));
        return restClientBuilder.requestFactory(requestFactory).build();
    }

    @Bean
    public FilterRegistrationBean<Filter> consoleRemotingFilter(NamingManager namingManager, RestClient restClient) {
        ConsoleRemotingFilter consoleRemotingFilter = new ConsoleRemotingFilter(namingManager, restClient);
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(consoleRemotingFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
