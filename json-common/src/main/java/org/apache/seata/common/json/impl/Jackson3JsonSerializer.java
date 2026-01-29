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
package org.apache.seata.common.json.impl;

import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.JsonAllowlistManager;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.loader.LoadLevel;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson 3.x implementation of JsonSerializer
 */
@LoadLevel(name = Jackson3JsonSerializer.NAME)
public class Jackson3JsonSerializer implements JsonSerializer {

    public static final String NAME = "jackson3";

    private final ObjectMapper defaultObjectMapper;

    private final ObjectMapper objectMapperWithAutoType;

    public Jackson3JsonSerializer() {
        this.defaultObjectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        this.objectMapperWithAutoType = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .activateDefaultTyping(null)
                .build();
    }

    @Override
    public String toJSONString(Object object) {
        try {
            return defaultObjectMapper.writeValueAsString(object);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson3 serialize error", e);
        }
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        if (text == null || clazz == null) {
            return null;
        }
        try {
            return defaultObjectMapper.readValue(text, clazz);
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson3 deserialize error", e);
        }
    }

    @Override
    public <T> T parseObjectWithType(String text, Type type) {
        if (text == null || type == null) {
            return null;
        }
        try {
            return objectMapperWithAutoType.readValue(text, objectMapperWithAutoType.constructType(type));
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson3 deserialize error", e);
        }
    }

    @Override
    public boolean useAutoType(String json) {
        return json != null && json.contains("\"@type\"");
    }

    @Override
    public String toJSONString(Object o, boolean prettyPrint) {
        return toJSONString(o, false, prettyPrint);
    }

    @Override
    public String toJSONString(Object o, boolean ignoreAutoType, boolean prettyPrint) {
        try {
            if (o instanceof List && ((List<?>) o).isEmpty()) {
                return "[]";
            }
            ObjectMapper mapper = ignoreAutoType ? defaultObjectMapper : objectMapperWithAutoType;
            if (prettyPrint) {
                return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            } else {
                return mapper.writeValueAsString(o);
            }
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson3 serialize error", e);
        }
    }

    @Override
    public <T> T parseObject(String json, Class<T> type, boolean ignoreAutoType) {
        if (json == null || type == null) {
            return null;
        }
        try {
            if ("[]".equals(json)) {
                return (T) new ArrayList<>(0);
            }

            // Check allowlist when AutoType is enabled
            if (!ignoreAutoType && useAutoType(json)) {
                checkAutoTypeClasses(json);
            }

            if (ignoreAutoType) {
                return defaultObjectMapper.readValue(json, type);
            } else {
                return objectMapperWithAutoType.readValue(json, type);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (JacksonException e) {
            throw new JsonParseException("Jackson3 deserialize error", e);
        }
    }

    /**
     * Check all @type classes in JSON against allowlist
     */
    private void checkAutoTypeClasses(String json) {
        int index = 0;
        while ((index = json.indexOf("\"@type\"", index)) >= 0) {
            String className = extractTypeValue(json, index);
            if (className != null) {
                JsonAllowlistManager.getInstance().checkClass(className);
            }
            index++;
        }
    }

    /**
     * Extract @type value from JSON string
     */
    private String extractTypeValue(String json, int typeIndex) {
        int colonIndex = json.indexOf(':', typeIndex);
        if (colonIndex < 0) {
            return null;
        }
        int startQuote = json.indexOf('"', colonIndex);
        if (startQuote < 0) {
            return null;
        }
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        return json.substring(startQuote + 1, endQuote);
    }
}
