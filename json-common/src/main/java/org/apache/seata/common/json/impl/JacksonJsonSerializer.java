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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.loader.LoadLevel;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Jackson implementation of JsonSerializer
 */
@LoadLevel(name = JacksonJsonSerializer.NAME)
public class JacksonJsonSerializer implements JsonSerializer {
    public static final String NAME = "jackson";

    private final ObjectMapper defaultObjectMapper;

    private final ObjectMapper objectMapperWithAutoType;

    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonJsonSerializer() {
        this.defaultObjectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disableDefaultTyping()
                .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);

        this.objectMapperWithAutoType = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enableDefaultTypingAsProperty(DefaultTyping.NON_FINAL, "@type")
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
            return mapper.readValue(text, clazz);
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
        } catch (IOException e) {
            throw new JsonParseException("Jackson deserialize error", e);
        }
    }

    // advanced methods for Saga
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
            if ("[]".equals(json)) {
                return (T) new ArrayList<>(0);
            }
            if (ignoreAutoType) {
                return defaultObjectMapper.readValue(json, type);
            } else {
                return objectMapperWithAutoType.readValue(json, type);
            }
        } catch (IOException e) {
            throw new JsonParseException("Jackson deserialize error", e);
        }
    }
}
