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

import java.lang.reflect.Type;

/**
 * The json serializer interface.
 */
public interface JsonSerializer {

    /**
     * Serializes the specified object into its JSON string representation.
     *
     * @param object the object to serialize
     * @return the JSON string representation of the object
     */
    String toJSONString(Object object);

    /**
     * Deserializes the specified JSON string into an object of the given class type.
     *
     * @param text the JSON string to parse
     * @param clazz the class of T
     * @param <T> the type of the desired object
     * @return the deserialized object of type T
     */
    <T> T parseObject(String text, Class<T> clazz);

    /**
     * Deserializes the specified JSON string into an object of the given type.
     *
     * @param text the JSON string to parse
     * @param type the type to deserialize into
     * @param <T> the type of the desired object
     * @return the deserialized object of type T
     */
    <T> T parseObjectWithType(String text, Type type);

    /**
     * Checks whether the given JSON string uses auto type features (such as type information for polymorphic deserialization).
     *
     * @param json the JSON string to check
     * @return true if auto type is used in the JSON, false otherwise
     */
    boolean useAutoType(String json);

    /**
     * Serializes the specified object into its JSON string representation.
     *
     * @param o the object to serialize
     * @param prettyPrint whether to format the JSON string for readability
     * @return the JSON string representation of the object
     */
    String toJSONString(Object o, boolean prettyPrint);

    /**
     * Serializes the specified object into its JSON string representation, with options to ignore auto type and pretty print.
     *
     * @param o the object to serialize
     * @param ignoreAutoType whether to ignore auto type information during serialization
     * @param prettyPrint whether to format the JSON string for readability
     * @return the JSON string representation of the object
     */
    String toJSONString(Object o, boolean ignoreAutoType, boolean prettyPrint);

    /**
     * Deserializes the specified JSON string into an object of the given class type, with an option to ignore auto type information.
     *
     * @param json the JSON string to parse
     * @param type the class of T
     * @param ignoreAutoType whether to ignore auto type information during deserialization
     * @param <T> the type of the desired object
     * @return the deserialized object of type T
     */
    <T> T parseObject(String json, Class<T> type, boolean ignoreAutoType);
}
