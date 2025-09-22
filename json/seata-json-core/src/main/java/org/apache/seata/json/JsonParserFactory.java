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
package org.apache.seata.json;

import org.apache.seata.common.DefaultValues;
import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.ConfigurationKeys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class JsonParserFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(JsonParserFactory.class);

    /**
     * Gets instance.
     *
     * @return the instance
     */
    public static JsonParser getInstance() {
        return JsonParserFactoryHolder.INSTANCE;
    }

    private static JsonParser buildJsonParser() {
        String jsonParserTypeName = ConfigurationFactory.getInstance()
                .getConfig(ConfigurationKeys.JSON_PARSER_TYPE, DefaultValues.DEFAULT_JSON_PARSER_TYPE);

        LOGGER.info("use json parser type: {}", jsonParserTypeName);

        JsonParserType jsonParserType = JsonParserType.getType(jsonParserTypeName);
        return EnhancedServiceLoader.load(
                JsonParser.class, Objects.requireNonNull(jsonParserType).name());
    }

    private static class JsonParserFactoryHolder {
        private static final JsonParser INSTANCE = buildJsonParser();
    }
}
