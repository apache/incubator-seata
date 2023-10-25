/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.seata.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * @author Junduo Dong
 */
public class ConfigValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigValidator.class);

    private static Map<String, ConfigSchema> configSchemaMap = new HashMap<>();

    private static final String SCHEMA_TYPE_NAME = "value-type";

    private static final String SCHEMA_TYPE_ENUM = "enum";

    private static final String SCHEMA_TYPE_INTEGER = "integer";

    private static final String SCHEMA_TYPE_STRING = "string";

    private static final String SCHEMA_TYPE_BOOLEAN = "boolean";

    private static final String SCHEMA_VALUE_MAX_NAME = "value-max";

    private static final String SCHEMA_VALUE_MIN_NAME = "value-min";

    private static final String SCHEMA_VALUE_RANGE = "value-range";

    private static final String SCHEMA_DYNAMIC_CONFIG = "dynamic-config";

    private static final String PREFIX_REGISTRY = "registry.";

    private static final String PREFIX_CONFIG = "config.";

    private static final String PREFIX_SEATA = ".seata";

    static {
        load();
    }

    private static void load() {
        Yaml yaml = new Yaml();
        InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream("config-schema.yml");
        Map<String, Object> rawMap = yaml.loadAs(in, HashMap.class);
        if (rawMap.size() == 0) {
            LOGGER.error("Wrong config-schema.yml");
        }
        flattenSchema("", rawMap);
    }

    private static void flattenSchema(String prefix, Map<String, Object> rawMap) {
        if (rawMap.containsKey(SCHEMA_TYPE_NAME)) {
            boolean dynamicConfig = (Boolean) rawMap.getOrDefault(SCHEMA_DYNAMIC_CONFIG, false);
            if (rawMap.get(SCHEMA_TYPE_NAME).equals(SCHEMA_TYPE_ENUM)) {
                List<String> values = (List<String>) rawMap.get(SCHEMA_VALUE_RANGE);
                configSchemaMap.put(prefix, new EnumConfigSchema(new HashSet<>(values), dynamicConfig));
            } else if (rawMap.get(SCHEMA_TYPE_NAME).equals(SCHEMA_TYPE_INTEGER)) {
                int min = (int) rawMap.getOrDefault(SCHEMA_VALUE_MIN_NAME, 0);
                int max = (int) rawMap.getOrDefault(SCHEMA_VALUE_MAX_NAME, Integer.MAX_VALUE);
                configSchemaMap.put(prefix, new IntegerConfigSchema(min, max, dynamicConfig));
            } else if (rawMap.get(SCHEMA_TYPE_NAME).equals(SCHEMA_TYPE_STRING)) {
                configSchemaMap.put(prefix, new StringConfigSchema(dynamicConfig));
            } else if (rawMap.get(SCHEMA_TYPE_NAME).equals(SCHEMA_TYPE_BOOLEAN)) {
                configSchemaMap.put(prefix, new BooleanConfigSchema(dynamicConfig));
            }
            return;
        }
        for (String key: rawMap.keySet()) {
            String suffix = removeMinus(key);
            String nextPrefix = prefix.equals(PREFIX_SEATA) ? suffix : (prefix + "." + suffix);
            flattenSchema(nextPrefix, (Map<String, Object>) rawMap.get(key));
        }
    }

    /**
     * Remove "-" in string. e.g. "server-port" -> "serverPort"
     * @param s old string
     * @return new string
     */
    private static String removeMinus(String s) {
        String[] strings = s.split("-");
        if (strings.length == 1)  return strings[0];
        StringBuilder builder = new StringBuilder(strings[0]);
        for (int i = 1; i < strings.length; i++) {
            builder.append(Character.toUpperCase(strings[i].charAt(0)));
            builder.append(strings[i].substring(1));
        }
        return builder.toString();
    }

    public static ValidateResult validateRegistryConf(String key, String value) {
        if (!key.startsWith(PREFIX_REGISTRY))
            return new ValidateResult(false, "Invalid registry config item");
        ConfigSchema schema = configSchemaMap.get(key);
        if (schema == null) return new ValidateResult(false, "Invalid config key");
        return schema.validate(value) ? new ValidateResult(true, "")
                : new ValidateResult(false, "Invalid config value");
    }

    public static ValidateResult validateCenterConf(String key, String value) {
        if (!key.startsWith(PREFIX_CONFIG))
            return new ValidateResult(false, "Invalid config-center config Item");
        ConfigSchema schema = configSchemaMap.get(key);
        if (schema == null) return new ValidateResult(false, "Invalid key");
        return schema.validate(value) ? new ValidateResult(true, "")
                : new ValidateResult(false, "Invalid value");
    }

    public static ValidateResult validateConfiguration(String key, String value) {
        if (key.startsWith(PREFIX_REGISTRY) || key.startsWith(PREFIX_CONFIG))
            return new ValidateResult(false, "Invalid configuration item");
        ConfigSchema schema = configSchemaMap.get(key);
        if (schema == null) return new ValidateResult(false, "Invalid key");
        return schema.validate(value) ? new ValidateResult(true, "")
                : new ValidateResult(false, "Invalid value");
    }

    public static boolean canBeConfiguredDynamically(String key) {
        ConfigSchema schema = configSchemaMap.get(key);
        return schema != null && schema.dynamicConfig;
    }

    public static Set<String> availableRegistryConf() {
        return configSchemaMap.keySet().stream()
                .filter(key -> key.startsWith(PREFIX_REGISTRY))
                .collect(Collectors.toSet());
    }

    public static Set<String> availableConfigCenterConf() {
        return configSchemaMap.keySet().stream()
                .filter(key -> key.startsWith(PREFIX_CONFIG))
                .collect(Collectors.toSet());
    }

    public static Set<String> availableConfiguration() {
        return configSchemaMap.keySet().stream().
                filter(key -> !key.startsWith(PREFIX_REGISTRY) && !key.startsWith(PREFIX_CONFIG))
                .collect(Collectors.toSet());
    }

    public static class ValidateResult {

        private boolean valid;

        private String errorMessage;

        public ValidateResult(boolean valid, String errorMessage) {
            this.valid = valid;
            this.errorMessage = errorMessage;
        }

        public boolean getValid() {
            return valid;
        }

        public String getErrorMessage() {
            return errorMessage;
        }
    }

    static class ConfigSchema {
        private String type;
        
        private boolean dynamicConfig;
        
        public ConfigSchema(String type, boolean dynamicConfig) {
            this.type = type;
            this.dynamicConfig = dynamicConfig;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public boolean isDynamicConfig() {
            return dynamicConfig;
        }

        public void setDynamicConfig(boolean dynamicConfig) {
            this.dynamicConfig = dynamicConfig;
        }

        public boolean validate(String value) {
            return false;
        }

        public boolean canDynamicConfig() {
            return dynamicConfig;
        }
    }
    
    static class IntegerConfigSchema extends ConfigSchema {
        private int min;
        
        private int max;

        public IntegerConfigSchema(int min, int max, boolean dynamicConfig) {
            super(SCHEMA_TYPE_INTEGER, dynamicConfig);
            this.min = min;
            this.max = max;
        }

        public int getMax() {
            return max;
        }

        public int getMin() {
            return min;
        }

        public void setMax(int max) {
            this.max = max;
        }

        public void setMin(int min) {
            this.min = min;
        }

        @Override
        public boolean validate(String value) {
            try {
                int v = Integer.parseInt(value);
                return v >= min && v <= max;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }
    
    static class StringConfigSchema extends ConfigSchema {
        public StringConfigSchema(boolean dynamicConfig) {
            super(SCHEMA_TYPE_STRING, dynamicConfig);
        }

        @Override
        public boolean validate(String value) {
            return value != null;
        }
    }
    
    static class EnumConfigSchema extends ConfigSchema {
        private Set<String> values;
        public EnumConfigSchema(Set<String> values, boolean dynamicConfig) {
            super(SCHEMA_TYPE_ENUM, dynamicConfig);
            this.values = values;
        }

        public Set<String> getValues() {
            return values;
        }

        public void setValues(Set<String> values) {
            this.values = values;
        }

        @Override
        public boolean validate(String value) {
            return values.contains(value);
        }
    }

    static class BooleanConfigSchema extends ConfigSchema {
        public BooleanConfigSchema(boolean dynamicConfig) {
            super(SCHEMA_TYPE_BOOLEAN, dynamicConfig);
        }

        @Override
        public boolean validate(String value) {
            return "true".equals(value) || "false".equals(value);
        }
    }
}
