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
package org.apache.seata.server.config;

import org.apache.seata.server.common.HttpClient;
import org.apache.seata.server.metrics.ConnectionPoolService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class ServerConfig {
    @Bean
    @ConditionalOnProperty(name = "seata.enableConnectionPoolMetrics", havingValue = "false")
    public ServerProperties emptyServerProperties() {
        return new ServerProperties();
    }

    @Bean
    @ConditionalOnProperty(name = "seata.enableConnectionPoolMetrics", havingValue = "true")
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "seata.enableConnectionPoolMetrics", havingValue = "true")
    public HttpClient httpClient(RestTemplate restTemplate) {
        return new HttpClient(restTemplate);
    }

    @Bean
    @ConditionalOnProperty(name = "seata.enableConnectionPoolMetrics", havingValue = "true")
    public ConnectionPoolService connectionPoolService(HttpClient httpClient) {
        return new ConnectionPoolService(httpClient);
    }
}
