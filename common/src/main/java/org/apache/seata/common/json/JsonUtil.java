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

    private final JsonSerializer serializer;

    /**
     * Create a JsonUtil instance with the specified JsonSerializer implementation
     *
     * @param serializer the JsonSerializer implementation to use
     * @throws IllegalArgumentException if serializer is null
     */
    public JsonUtil(JsonSerializer serializer) {
        if (serializer == null) {
            throw new IllegalArgumentException("JsonSerializer cannot be null");
        }
        this.serializer = serializer;
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

    /**
     * Get the underlying JsonSerializer instance
     *
     * @return the JsonSerializer instance
     */
    public JsonSerializer getSerializer() {
        return serializer;
    }
}
