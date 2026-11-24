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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.JsonAllowlistManager;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.loader.LoadLevel;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Jackson implementation of JsonSerializer
 */
@LoadLevel(name = JacksonJsonSerializer.NAME)
public class JacksonJsonSerializer implements JsonSerializer {
    public static final String NAME = "jackson";

    private static final Pattern AUTOTYPE_PATTERN = Pattern.compile("\"@type\"\\s*:");

    private final ObjectMapper defaultObjectMapper;

    private final ObjectMapper objectMapperWithAutoType;

    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonJsonSerializer() {
        this.defaultObjectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disableDefaultTyping()
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        AllowlistTypeValidator validator = new AllowlistTypeValidator();
        ObjectMapper.DefaultTypeResolverBuilder typer =
                new ObjectMapper.DefaultTypeResolverBuilder(DefaultTyping.NON_FINAL, validator);
        typer.init(JsonTypeInfo.Id.CLASS, null);
        typer.inclusion(JsonTypeInfo.As.PROPERTY);
        typer.typeProperty("@type");

        this.objectMapperWithAutoType = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .setDefaultTyping(typer)
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.activateDefaultTyping(
                this.mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY);
        this.mapper.setConfig(this.mapper.getSerializationConfig().with(MapperFeature.PROPAGATE_TRANSIENT_MARKER));
        this.mapper.setConfig(this.mapper.getDeserializationConfig().with(MapperFeature.PROPAGATE_TRANSIENT_MARKER));
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String toJSONString(Object object) {
        try {
            return mapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            throw new JsonParseException("Jackson serialize error", e);
        }
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        if (text == null || clazz == null) {
            return null;
        }
        try {
            return defaultObjectMapper.readValue(text, clazz);
        } catch (IOException e) {
            throw new JsonParseException("Jackson deserialize error", e);
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
        } catch (IOException e) {
            rethrowIfSecurityException(e);
            throw new JsonParseException("Jackson deserialize error", e);
        }
    }

    // advanced methods for Saga
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
            if (prettyPrint) {
                if (ignoreAutoType) {
                    return defaultObjectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(o);
                } else {
                    return objectMapperWithAutoType
                            .writerWithDefaultPrettyPrinter()
                            .writeValueAsString(o);
                }
            } else {
                if (ignoreAutoType) {
                    return defaultObjectMapper.writeValueAsString(o);
                } else {
                    return objectMapperWithAutoType.writeValueAsString(o);
                }
            }
        } catch (JsonProcessingException e) {
            throw new JsonParseException("Jackson serialize error", e);
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
        } catch (IOException e) {
            rethrowIfSecurityException(e);
            throw new JsonParseException("Jackson deserialize error", e);
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

    private static class AllowlistTypeValidator extends PolymorphicTypeValidator.Base {
        private static final long serialVersionUID = 1L;

        @Override
        public Validity validateBaseType(MapperConfig<?> config, JavaType baseType) {
            return Validity.INDETERMINATE;
        }

        @Override
        public Validity validateSubClassName(MapperConfig<?> config, JavaType baseType, String subClassName) {
            // Throws SecurityException if not allowed
            JsonAllowlistManager.getInstance().checkClass(subClassName);
            return Validity.ALLOWED;
        }

        @Override
        public Validity validateSubType(MapperConfig<?> config, JavaType baseType, JavaType subType) {
            JsonAllowlistManager.getInstance().checkClass(subType.getRawClass().getName());
            return Validity.ALLOWED;
        }
    }
}
