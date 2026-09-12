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
package org.apache.seata.spring.boot.autoconfigure.properties;

import org.apache.seata.common.json.JsonAllowlistManager;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.JSON_PREFIX;

/**
 * Seata JSON configuration properties
 */
@Component
@ConfigurationProperties(prefix = JSON_PREFIX)
public class SeataJsonProperties {

    /**
     * JSON serializer implementation to use for Seata JSON serialization/deserialization.
     * Supported values include fastjson, fastjson2, jackson, jackson3, and gson.
     * The {@code jackson3} option requires JDK 17 or later; on lower JDK versions,
     * Seata falls back to {@code jackson}.
     * This property is bound from the Spring Boot configuration key {@code seata.json.serializer-type}
     * (derived from {@code JSON_PREFIX}).
     */
    private String serializerType;

    /**
     * JSON deserialization allowlist, comma-separated
     * Entries ending with '.' are prefix matches, otherwise exact matches
     * Example: com.company.model.,com.company.dto.,com.company.SomeClass
     */
    private String allowlist;

    public String getSerializerType() {
        return serializerType;
    }

    public SeataJsonProperties setSerializerType(String serializerType) {
        this.serializerType = serializerType;
        return this;
    }

    public String getAllowlist() {
        return allowlist;
    }

    public SeataJsonProperties setAllowlist(String allowlist) {
        this.allowlist = allowlist;
        return this;
    }

    @PostConstruct
    public void init() {
        JsonAllowlistManager.getInstance().loadUserAllowlist(allowlist);
    }
}
