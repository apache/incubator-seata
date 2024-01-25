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
package org.apache.seata.saga.statelang.parser;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JsonParserFactory
 *
 */
public class JsonParserFactory {

    private JsonParserFactory() {
    }

    private static final ConcurrentMap<String, JsonParser> INSTANCES = new ConcurrentHashMap<>();

    /**
     * Gets JsonParser by name
     *
     * @param name parser name
     * @return the JsonParser
     */
    public static JsonParser getJsonParser(String name) {
        return CollectionUtils.computeIfAbsent(INSTANCES, name,
            key -> EnhancedServiceLoader.load(JsonParser.class, name, Thread.currentThread().getContextClassLoader()));
    }
}
