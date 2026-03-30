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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.seata.spring.boot.autoconfigure.StarterConstants.REGISTRY_METADATA_PREFIX;

@Component
@ConfigurationProperties(prefix = REGISTRY_METADATA_PREFIX)
public class RegistryMetadataProperties {
    private String external;

    /**
     * Dynamic metadata configuration map
     * This allows loading all metadata properties dynamically
     */
    private Map<String, String> metadata = new ConcurrentHashMap<>();

    public String getExternal() {
        return external;
    }

    public RegistryMetadataProperties setExternal(String external) {
        this.external = external;
        return this;
    }

    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
    }

    public RegistryMetadataProperties setMetadata(Map<String, String> metadata) {
        if (metadata != null) {
            this.metadata = new ConcurrentHashMap<>(metadata);
        } else {
            this.metadata = new ConcurrentHashMap<>();
        }
        return this;
    }

    /**
     * Set metadata value by key
     * @param key metadata key
     * @param value metadata value
     */
    public void setMetadataValue(String key, String value) {
        metadata.put(key, value);
    }

    /**
     * Get environment
     * @return environment value
     */
    public String getEnv() {
        return metadata.get("env");
    }

    /**
     * Set environment
     * @param env environment value
     */
    public void setEnv(String env) {
        metadata.put("env", env);
    }

    /**
     * Get region
     * @return region value
     */
    public String getRegion() {
        return metadata.get("region");
    }

    /**
     * Set region
     * @param region region value
     */
    public void setRegion(String region) {
        metadata.put("region", region);
    }

    /**
     * Get version
     * @return version value
     */
    public String getVersion() {
        return metadata.get("version");
    }

    /**
     * Set version
     * @param version version value
     */
    public void setVersion(String version) {
        metadata.put("version", version);
    }

    /**
     * Get weight
     * @return weight value
     */
    public String getWeight() {
        return metadata.get("weight");
    }

    /**
     * Set weight
     * @param weight weight value
     */
    public void setWeight(String weight) {
        metadata.put("weight", weight);
    }
}
