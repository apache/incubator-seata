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
import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import org.apache.seata.namingserver.filter.ConsoleRemotingFilter;
import org.apache.seata.namingserver.manager.NamingManager;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.http.client.OkHttp3ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.concurrent.TimeUnit;

import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_CONNECTION_MAX_PER_ROUTE;
import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_CONNECTION_MAX_TOTAL;
import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_REQUEST_TIMEOUT;
import static org.apache.seata.namingserver.contants.NamingConstant.DEFAULT_WRITE_TIMEOUT;

@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(DEFAULT_CONNECTION_MAX_TOTAL);
        dispatcher.setMaxRequestsPerHost(DEFAULT_CONNECTION_MAX_PER_ROUTE);

        OkHttpClient client = new OkHttpClient.Builder()
                .dispatcher(dispatcher)
                .connectTimeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .readTimeout(DEFAULT_REQUEST_TIMEOUT, TimeUnit.MILLISECONDS)
                .writeTimeout(DEFAULT_WRITE_TIMEOUT, TimeUnit.MILLISECONDS)
                .build();

        // Create and return a RestTemplate with the custom request factory
        return new RestTemplate(new OkHttp3ClientHttpRequestFactory(client));
    }

    @Bean
    public FilterRegistrationBean<Filter> consoleRemotingFilter(
            NamingManager namingManager, RestTemplate restTemplate) {
        ConsoleRemotingFilter consoleRemotingFilter = new ConsoleRemotingFilter(namingManager, restTemplate);
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>();
        registration.setFilter(consoleRemotingFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }
}
