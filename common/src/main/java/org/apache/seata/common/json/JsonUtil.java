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

import org.apache.seata.common.exception.JsonParseException;

import java.lang.reflect.Type;

/**
 * Unified JSON utility class
 */
public final class JsonUtil {

    private static volatile JsonUtil fastjsonInstance;
    private static volatile JsonUtil jacksonInstance;

    private final JsonSerializer serializer;

    private JsonUtil(JsonSerializer serializer) {
        this.serializer = serializer;
    }

    /**
     * Get JsonUtil instance with FastJSON implementation
     */
    public static JsonUtil fastjson() {
        if (fastjsonInstance == null) {
            synchronized (JsonUtil.class) {
                if (fastjsonInstance == null) {
                    fastjsonInstance = new JsonUtil(JsonSerializerFactory.getSerializer("fastjson"));
                }
            }
        }
        return fastjsonInstance;
    }

    /**
     * Get JsonUtil instance with Jackson implementation
     */
    public static JsonUtil jackson() {
        if (jacksonInstance == null) {
            synchronized (JsonUtil.class) {
                if (jacksonInstance == null) {
                    jacksonInstance = new JsonUtil(JsonSerializerFactory.getSerializer("jackson"));
                }
            }
        }
        return jacksonInstance;
    }

    /**
     * Get JsonUtil instance with custom implementation
     */
    public static JsonUtil custom(String serializerName) {
        return new JsonUtil(JsonSerializerFactory.getSerializer(serializerName));
    }

    /**
     * Serialize the given object to JSON string
     *
     * @param object the object to serialize
     * @return the JSON string representation
     * @throws JsonParseException if serialization fails
     */
    public String toJSONString(Object object) {
        return serializer.toJSONString(object);
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
    public <T> T parseObject(String text, Class<T> clazz) {
        if (text == null || clazz == null) {
            return null;
        }
        return serializer.parseObject(text, clazz);
    }

    /**
     * Deserialize the given JSON string to an object of the specified type
     * This method supports generic types (e.g., List<String>, Map<String, Object>)
     *
     * @param <T>   the type of the object
     * @param text  the JSON string
     * @param type  the type to deserialize to
     * @return the deserialized object
     * @throws JsonParseException if deserialization fails
     */
    public <T> T parseObject(String text, Type type) {
        if (text == null || type == null) {
            return null;
        }
        return serializer.parseObject(text, type);
    }
}
