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

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.seata.common.Constants;
import org.apache.seata.config.dto.ConfigurationItem;
import org.apache.seata.config.dto.ConfigurationItemMeta;
import org.apache.seata.config.store.rocksdb.RocksDBConfigStoreManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.yaml.snakeyaml.Yaml;


/**
 * The configuration items processor
 *
 */
public class ConfigurationProcessor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RocksDBConfigStoreManager.class);
    private static final String ENCRYPT_STRING = "******";
    private static final String NOT_SEATA_CONFIG = "Not Seata configuration";
    private static final Map<String, ConfigurationItemMeta> CONFIGURATION_ITEMS_META_MAP = new HashMap<>();
    private static final String META_FILE_NAME = Constants.CONFIGURATION_META_FILE_NAME;
    private static final String CONFIG_META_KEY = "configuration-meta";
    private static final String META_KEY_KEY = "key";
    private static final String META_DESC_KEY = "desc";
    private static final String META_DEFAULT_VALUE_KEY = "defaultValue";
    private static final String META_ENCRYPT_KEY = "isEncrypt";

    static {
        loadConfigurationItemMeta();
    }

    /**
     * load Configuration items meta from local yaml file.
     */
    @SuppressWarnings("unchecked")
    private static void loadConfigurationItemMeta() {
        try (InputStream inputStream = new ClassPathResource(META_FILE_NAME).getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> map = yaml.load(inputStream);
            List<Map<String, Object>> configItemMetaList = (List<Map<String, Object>>) map.get(CONFIG_META_KEY);
            for (Map<String, Object> metaMap : configItemMetaList) {
                ConfigurationItemMeta itemMeta = mapToConfigItemMeta(metaMap);
                CONFIGURATION_ITEMS_META_MAP.put(itemMeta.getKey(), itemMeta);
            }
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration meta file", e);
        }
    }

    private static ConfigurationItemMeta mapToConfigItemMeta(Map<String, Object> configItemMap) {
        String key = (String) configItemMap.get(META_KEY_KEY);
        String desc = (String) configItemMap.get(META_DESC_KEY);
        Object defaultValue = configItemMap.get(META_DEFAULT_VALUE_KEY);
        Boolean isEncrypt = (Boolean) configItemMap.get(META_ENCRYPT_KEY);
        return new ConfigurationItemMeta(key, desc, defaultValue, isEncrypt);
    }
    /**
     * process configuration items map (fill description and default value ,or encrypt sensitive data).
     */
    public static Map<String, ConfigurationItem> processConfigMap(Map<String, Object> configMap) {
        return configMap.entrySet().stream()
                .map(entry -> {
                    String key = entry.getKey();
                    Object value = entry.getValue();
                    return new HashMap.SimpleEntry<>(key, processConfigItem(key, value));
                })
                .collect(HashMap::new, (map, entry) -> map.put(entry.getKey(), entry.getValue()), HashMap::putAll);
    }

    /**
     * Process ConfigurationItem
     */
    public static ConfigurationItem processConfigItem(String key, Object value) {
        ConfigurationItemMeta meta = CONFIGURATION_ITEMS_META_MAP.get(key);
        ConfigurationItem item = new ConfigurationItem();
        item.setKey(key);
        item.setDescription(meta == null ? NOT_SEATA_CONFIG : meta.getDescription());
        item.setDefaultValue(meta == null ? null : meta.getDefaultValue());
        if (meta != null && meta.getEncrypt()) {
            item.setValue(ENCRYPT_STRING);
        } else {
            item.setValue(value);
        }
        return item;
    }
}
