/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.tcc.serializer;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.seata.common.util.StringUtils;

/**
 * Used to serialize and deserialize BusinessActionContext
 *
 * @author zouwei
 */
public class BusinessActionContextSerializer {

    private static final JacksonSerializer JACKSON_SERIALIZER = new JacksonSerializer();

    /**
     * serialize Object value to json string by jackson
     * 
     * @param value
     * @return
     */
    public static String toJsonString(Object value) {
        return JACKSON_SERIALIZER.toJsonString(value);
    }

    /**
     * deserialize json string by fastjson or jackson
     * 
     * @param json json string
     * @param clazz target class
     * @param <T>
     * @return
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        T result;
        if (!json.startsWith("{\"@class\":")) {
            result = JSON.parseObject(json, clazz);
        } else {
            result = JACKSON_SERIALIZER.parseObject(json, clazz);
        }
        return result;
    }

    private static class JacksonSerializer {

        private static final Logger LOGGER = LoggerFactory.getLogger(JacksonSerializer.class);

        private final ObjectMapper mapper = new ObjectMapper();

        private JacksonSerializer() {
            this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            this.mapper.activateDefaultTyping(this.mapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
            this.mapper.enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER);
        }

        /**
         * serialize Object value to json string by jackson
         * 
         * @param value
         * @return
         */
        public String toJsonString(Object value) {
            if (Objects.isNull(value)) {
                return null;
            }
            try {
                return value instanceof String ? (String)value : this.mapper.writeValueAsString(value);
            } catch (JsonProcessingException e) {
                LOGGER.error("json toJsonString exception, {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        /**
         * deserialize json string by jackson
         * 
         * @param json json string
         * @param clazz target calss
         * @param <T>
         * @return
         */
        public <T> T parseObject(String json, Class<T> clazz) {
            if (StringUtils.isBlank(json) || Objects.isNull(clazz)) {
                return null;
            }
            try {
                return clazz.equals(String.class) ? (T)json : this.mapper.readValue(json, clazz);
            } catch (JsonProcessingException e) {
                LOGGER.error("json parseObject exception, {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }
}
