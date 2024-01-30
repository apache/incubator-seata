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
package org.apache.seata.spring.boot.autoconfigure.properties.registry;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_EUREKA_PREFIX;


@Component
@ConfigurationProperties(prefix = REGISTRY_EUREKA_PREFIX)
public class RegistryEurekaProperties {
    private String serviceUrl = "http://localhost:8761/eureka";
    private String application = "default";
    private String weight = "1";

    public String getServiceUrl() {
        return serviceUrl;
    }

    public RegistryEurekaProperties setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
        return this;
    }

    public String getApplication() {
        return application;
    }

    public RegistryEurekaProperties setApplication(String application) {
        this.application = application;
        return this;
    }

    public String getWeight() {
        return weight;
    }

    public RegistryEurekaProperties setWeight(String weight) {
        this.weight = weight;
        return this;
    }
}
