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
package org.apache.seata.config.store;

import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigType;
import org.apache.seata.config.processor.ConfigDataType;
import org.apache.seata.config.processor.ConfigProcessor;
import org.apache.seata.config.store.rocksdb.RocksDBConfigStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The type Abstract config store manager.
 *
 */
public abstract class AbstractConfigStoreManager implements ConfigStoreManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractConfigStoreManager.class);
    @Override
    public String get(String group, String key) {
        return null;
    }

    @Override
    public Map<String, Object> getAll(String group) {
        return new HashMap<>();
    }

    @Override
    public Boolean put(String group, String key, Object value) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean delete(String group, String key) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean putAll(String group, Map<String, Object> configMap) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean deleteAll(String group) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean isEmpty(String group) {
        return Boolean.TRUE;
    }

    @Override
    public Map<String, Object> getConfigMap() {
        return new HashMap<>();
    }

    @Override
    public Boolean putConfigMap(Map<String, Object> configMap) {
        return Boolean.FALSE;
    }

    @Override
    public Boolean clearData() {
        return Boolean.FALSE;
    }

    @Override
    public void destroy() {}

    public abstract void shutdown();

    protected static String convertConfig2Str(Map<String, Object> configs) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue().toString()).append("\n");
        }
        return sb.toString();
    }

    protected static Map<String, Object> convertConfigStr2Map(String configStr) {
        if (StringUtils.isEmpty(configStr)) {
            return new HashMap<>();
        }
        Map<String, Object> configs = new HashMap<>();
        try {
            Properties properties = ConfigProcessor.processConfig(configStr, ConfigDataType.properties.name());
            properties.forEach((k, v) -> configs.put(k.toString(), v));
            return configs;
        } catch (IOException e) {
            LOGGER.error("convert config properties error", e);
            return new HashMap<>();
        }
    }

}
