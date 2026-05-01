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
        String serializerType = configuration.getConfig(ConfigurationKeys.JSON_SERIALIZER_TYPE);
        if (StringUtils.isNotBlank(serializerType)) {
            return serializerType;
        }

        String deprecatedSerializerType =
                configuration.getConfig(ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME);
        if (StringUtils.isNotBlank(deprecatedSerializerType)) {
            LOGGER.warn(
                    "The config '{}' is deprecated since 2.7.0 and will be removed in a future version. Please use '{}' instead.",
                    ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME,
                    ConfigurationKeys.JSON_SERIALIZER_TYPE);
            return deprecatedSerializerType;
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
        String jsonParseName = text.startsWith(Constants.JACKSON_JSON_TEXT_PREFIX)
                ? Constants.JACKSON_JSON_PARSER_NAME
                : CONFIG_JSON_SERIALIZER_NAME;
        return JsonSerializerFactory.getSerializer(jsonParseName).parseObject(text, clazz);
    }
}
