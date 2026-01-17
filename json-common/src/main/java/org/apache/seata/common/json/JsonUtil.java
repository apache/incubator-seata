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
import org.apache.seata.config.ConfigurationFactory;

import java.util.Objects;

/**
 * Unified JSON utility class
 */
public final class JsonUtil {

    private static final String CONFIG_JSON_PARSER_NAME = ConfigurationFactory.getInstance()
            .getConfig(
                    ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME,
                    DefaultValues.DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER);

    private static final JsonSerializer DEFAULT_SERIALIZER =
            JsonSerializerFactory.getSerializer(CONFIG_JSON_PARSER_NAME);

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
                : CONFIG_JSON_PARSER_NAME;
        return JsonSerializerFactory.getSerializer(jsonParseName).parseObject(text, clazz);
    }
}
