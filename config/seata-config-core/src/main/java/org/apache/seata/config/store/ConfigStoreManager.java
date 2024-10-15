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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.processor.ConfigDataType;
import org.apache.seata.config.processor.ConfigProcessor;

/**
 * The interface Local config store manager.
 *
 */
public interface ConfigStoreManager {
    String get(String namespace, String dataId, String key);

    Map<String, Object> getAll(String namespace, String dataId);

    Boolean put(String namespace, String dataId, String key, Object value);

    Boolean delete(String namespace, String dataId, String key);

    Boolean putAll(String namespace, String dataId, Map<String, Object> configMap);

    Boolean deleteAll(String namespace, String dataId);

    Boolean isEmpty(String namespace, String dataId);

    Map<String, Map<String, Object>> getConfigMap();

    Boolean putConfigMap(Map<String, Map<String, Object>> configMap);

    Boolean clearData();

    List<String> getAllNamespaces();

    List<String> getAllDataIds(String namespace);

    Long getConfigVersion(String namespace, String dataId);

    Boolean putConfigVersion(String namespace, String dataId, Long version);

    Boolean deleteConfigVersion(String namespace, String dataId);
    void destroy();
    void shutdown();

    default void addConfigListener(String group, String dataId, ConfigurationChangeListener listener) {};

    default void removeConfigListener(String group, String dataId, ConfigurationChangeListener listener) {};

    static String convertConfig2Str(Map<String, Object> configs) {
        StringBuilder sb = new StringBuilder();
        if (CollectionUtils.isEmpty(configs)) {
            sb.toString();
        }
        for (Map.Entry<String, Object> entry : configs.entrySet()) {
            sb.append(entry.getKey()).append("=").append(entry.getValue().toString()).append("\n");
        }
        return sb.toString();
    }

    static Map<String, Object> convertConfigStr2Map(String configStr) {
        if (StringUtils.isEmpty(configStr)) {
            return new HashMap<>();
        }
        Map<String, Object> configs = new HashMap<>();
        try {
            Properties properties = ConfigProcessor.processConfig(configStr, ConfigDataType.properties.name());
            properties.forEach((k, v) -> configs.put(k.toString(), v));
            return configs;
        } catch (IOException e) {
            return new HashMap<>();
        }
    }
}
