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
package org.apache.seata.common.json;

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.Constants;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Objects;

/**
 * Unified JSON utility class
 */
public final class JsonUtil {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonUtil.class);

    private static final String CONFIG_JSON_SERIALIZER_NAME =
            resolveJsonSerializerName(ConfigurationFactory.getInstance());

    private static final JsonSerializer DEFAULT_SERIALIZER =
            JsonSerializerFactory.getSerializer(CONFIG_JSON_SERIALIZER_NAME);

    static String resolveJsonSerializerName(Configuration configuration) {
        String deprecatedSagaSerializerType = configuration.getConfig(ConfigurationKeys.CLIENT_SAGA_JSON_PARSER);
        if (StringUtils.isNotBlank(deprecatedSagaSerializerType)) {
            LOGGER.warn(
                    "The config '{}' is deprecated and will be removed in a future version. "
                            + "It takes precedence over '{}'; remove the deprecated key to let '{}' take effect.",
                    ConfigurationKeys.CLIENT_SAGA_JSON_PARSER,
                    ConfigurationKeys.JSON_SERIALIZER_TYPE,
                    ConfigurationKeys.JSON_SERIALIZER_TYPE);
            return deprecatedSagaSerializerType;
        }

        String serializerType = configuration.getConfig(ConfigurationKeys.JSON_SERIALIZER_TYPE);
        if (StringUtils.isNotBlank(serializerType)) {
            return serializerType;
        }

        return DefaultValues.BUSINESS_ACTION_CONTEXT_JSON_PARSER;
    }

    /**
     * Serialize the given object to JSON string
     *
     * @param object the object to serialize
     * @return the JSON string representation
     * @throws JsonParseException if serialization fails
     */
    public static String toJSONString(Object object) {
        return DEFAULT_SERIALIZER.toJSONString(object);
    }

    /**
     * Serialize the given object to JSON string.
     *
     * @param object the object to serialize
     * @param prettyPrint whether to format the JSON string for readability
     * @return the JSON string representation
     * @throws JsonParseException if serialization fails
     */
    public static String toJSONString(Object object, boolean prettyPrint) {
        return DEFAULT_SERIALIZER.toJSONString(object, prettyPrint);
    }

    /**
     * Serialize the given object to JSON string.
     *
     * @param object the object to serialize
     * @param ignoreAutoType whether to ignore auto type information
     * @param prettyPrint whether to format the JSON string for readability
     * @return the JSON string representation
     * @throws JsonParseException if serialization fails
     */
    public static String toJSONString(Object object, boolean ignoreAutoType, boolean prettyPrint) {
        return DEFAULT_SERIALIZER.toJSONString(object, ignoreAutoType, prettyPrint);
    }

    /**
     * Deserialize the given JSON string to an object of the specified class
     *
     * @param <T>   the type of the object
     * @param text  the JSON string
     * @param clazz the class to deserialize to
     * @return the deserialized object
     * @throws JsonParseException if deserialization fails
     */
    public static <T> T parseObject(String text, Class<T> clazz) {
        if (Objects.isNull(text) || Objects.isNull(clazz)) {
            return null;
        }
        return getJsonSerializer(text).parseObject(text, clazz);
    }

    /**
     * Deserialize the given JSON string to an object of the specified class.
     *
     * @param <T> the type of the object
     * @param text the JSON string
     * @param clazz the class to deserialize to
     * @param ignoreAutoType whether to ignore auto type information
     * @return the deserialized object
     * @throws JsonParseException if deserialization fails
     */
    public static <T> T parseObject(String text, Class<T> clazz, boolean ignoreAutoType) {
        if (Objects.isNull(text) || Objects.isNull(clazz)) {
            return null;
        }
        return getJsonSerializer(text).parseObject(text, clazz, ignoreAutoType);
    }

    /**
     * Deserialize the given JSON string to an object of the specified type.
     *
     * @param <T> the type of the object
     * @param text the JSON string
     * @param type the type to deserialize to
     * @return the deserialized object
     * @throws JsonParseException if deserialization fails
     */
    public static <T> T parseObjectWithType(String text, Type type) {
        if (Objects.isNull(text) || Objects.isNull(type)) {
            return null;
        }
        return getJsonSerializer(text).parseObjectWithType(text, type);
    }

    /**
     * Check whether the given JSON string uses auto type information.
     *
     * @param json the JSON string to check
     * @return true if auto type is used, otherwise false
     */
    public static boolean useAutoType(String json) {
        if (json == null) {
            return false;
        }
        if (json.startsWith(Constants.JACKSON_JSON_TEXT_PREFIX)) {
            return true;
        }
        return DEFAULT_SERIALIZER.useAutoType(json);
    }

    private static JsonSerializer getJsonSerializer(String text) {
        String jsonParserName = text.startsWith(Constants.JACKSON_JSON_TEXT_PREFIX)
                ? Constants.JACKSON_JSON_PARSER_NAME
                : CONFIG_JSON_SERIALIZER_NAME;
        return JsonSerializerFactory.getSerializer(jsonParserName);
    }
}
