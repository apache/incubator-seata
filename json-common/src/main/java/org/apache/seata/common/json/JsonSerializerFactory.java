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

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class JsonSerializerFactory {

    private static final String DEFAULT_SERIALIZER = "jackson";

    private static final Map<String, JsonSerializer> INSTANCES = new ConcurrentHashMap<>();

    private JsonSerializerFactory() {}

    /**
     * Get JsonSerializer instance by name.
     *
     * @param name the serializer name (e.g., "fastjson", "jackson", "gson")
     * @return the JsonSerializer instance
     */
    public static JsonSerializer getSerializer(String name) {
        final String serializerName = Optional.ofNullable(name).orElse(DEFAULT_SERIALIZER);
        return CollectionUtils.computeIfAbsent(
                INSTANCES, serializerName, key -> EnhancedServiceLoader.load(JsonSerializer.class, key));
    }
}
