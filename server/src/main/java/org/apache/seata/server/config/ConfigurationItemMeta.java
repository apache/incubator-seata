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

/**
 * The configuration items meta
 *
 */
public class ConfigurationItemMeta {
    private final String key;
    private final String description;
    private final Object defaultValue;
    private final Boolean isEncrypt;

    public ConfigurationItemMeta(String key, String description, Object defaultValue, Boolean isEncrypt) {
        this.key = key;
        this.description = description;
        this.defaultValue = defaultValue;
        this.isEncrypt = isEncrypt;
    }

    public String getKey() {
        return key;
    }

    public String getDescription() {
        return description;
    }

    public Object getDefaultValue() {
        return defaultValue;
    }
    public Boolean getEncrypt() {
        return isEncrypt;
    }
}
