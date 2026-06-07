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
import tools.jackson.databind.DatabindContext;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Jackson 3.x implementation of JsonSerializer
 */
@LoadLevel(name = Jackson3JsonSerializer.NAME)
public class Jackson3JsonSerializer implements JsonSerializer {

    public static final String NAME = "jackson3";

    private static final Pattern AUTOTYPE_PATTERN = Pattern.compile("\"@type\"\\s*:");

    private final ObjectMapper defaultObjectMapper;

    private final ObjectMapper objectMapperWithAutoType;

    public Jackson3JsonSerializer() {
        this.defaultObjectMapper = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        AllowlistTypeValidator validator = new AllowlistTypeValidator();
        this.objectMapperWithAutoType = JsonMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .activateDefaultTypingAsProperty(validator, DefaultTyping.NON_FINAL, "@type")
                .build();
    }

    @Override
    public String toJSONString(Object object) {
        try {
            if (object instanceof List && ((List<?>) object).isEmpty()) {
                return "[]";
            }
            return objectMapperWithAutoType.writeValueAsString(object);
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
        } catch (SecurityException e) {
            throw e;
        } catch (JacksonException e) {
            rethrowIfSecurityException(e);
            throw new JsonParseException("Jackson3 deserialize error", e);
        }
    }

    @Override
    public boolean useAutoType(String json) {
        return json != null && AUTOTYPE_PATTERN.matcher(json).find();
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
            if ("[]".equals(json) && (java.util.Collection.class.isAssignableFrom(type) || type == Object.class)) {
                return (T) new ArrayList<>(0);
            }
            if (ignoreAutoType) {
                return defaultObjectMapper.readValue(json, type);
            } else {
                return objectMapperWithAutoType.readValue(json, type);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (JacksonException e) {
            rethrowIfSecurityException(e);
            throw new JsonParseException("Jackson3 deserialize error", e);
        }
    }

    private static void rethrowIfSecurityException(Throwable e) {
        Throwable cause = e.getCause();
        while (cause != null) {
            if (cause instanceof SecurityException) {
                throw (SecurityException) cause;
            }
            cause = cause.getCause();
        }
    }

    /**
     * Jackson 3.x native PolymorphicTypeValidator that delegates to JsonAllowlistManager
     */
    private static class AllowlistTypeValidator extends PolymorphicTypeValidator.Base {
        private static final long serialVersionUID = 1L;

        @Override
        public Validity validateBaseType(DatabindContext ctxt, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(DatabindContext ctxt, JavaType baseType, String subClassName) {
            // Throws SecurityException if not allowed
            JsonAllowlistManager.getInstance().checkClass(subClassName);
            return Validity.ALLOWED;
        }

        @Override
        public Validity validateSubType(DatabindContext ctxt, JavaType baseType, JavaType subType) {
            JsonAllowlistManager.getInstance().checkClass(subType.getRawClass().getName());
            return Validity.ALLOWED;
        }
    }
}
