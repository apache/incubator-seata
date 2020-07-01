/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.saga.statelang.parser;

import io.seata.common.loader.EnhancedServiceLoader;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * JsonParserFactory
 *
 * @author lorne.cl
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
        JsonParser parser = INSTANCES.get(name);
        if (parser == null) {
            synchronized (JsonParserFactory.class) {
                parser = INSTANCES.get(name);
                if (parser == null) {
                    parser = EnhancedServiceLoader.load(JsonParser.class, name, Thread.currentThread().getContextClassLoader());
                    INSTANCES.putIfAbsent(name, parser);
                }
            }
        }
        return parser;
    }
}