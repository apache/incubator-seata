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
package org.apache.seata.integration.tx.api.json;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.util.CollectionUtils;


public class JsonParserFactory {

    private static final Map<String, JsonParserWrap> JSON_PARSER_INSTANCES = new ConcurrentHashMap<>();

    public static JsonParserWrap getInstance(String jsonParserName) {
        final String name =
            Optional.ofNullable(jsonParserName).orElse(DefaultValues.DEFAULT_TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER);
        return CollectionUtils.computeIfAbsent(JSON_PARSER_INSTANCES, name,
            key -> new JsonParserWrap(EnhancedServiceLoader.load(JsonParser.class, name)));
    }
}
