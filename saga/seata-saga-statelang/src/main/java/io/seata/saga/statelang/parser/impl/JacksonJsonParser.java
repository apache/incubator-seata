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
package io.seata.saga.statelang.parser.impl;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import io.seata.common.loader.LoadLevel;
import io.seata.saga.statelang.parser.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * JsonParser implement by Jackson
 *
 * @author lorne.cl
 */
@LoadLevel(name = JacksonJsonParser.NAME)
public class JacksonJsonParser implements JsonParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(JacksonJsonParser.class);

    private ObjectMapper objectMapperWithAutoType = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enableDefaultTypingAsProperty(DefaultTyping.NON_FINAL, "@type")
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
            .setSerializationInclusion(Include.NON_NULL);

    private ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .disableDefaultTyping()
            .enable(MapperFeature.PROPAGATE_TRANSIENT_MARKER)
            .setSerializationInclusion(Include.NON_NULL);

    public static final String NAME = "jackson";

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toJsonString(Object o, boolean prettyPrint) {
        try {
            if (o instanceof List && ((List) o).size() == 0) {
                return "[]";
            }
            if (prettyPrint) {
                return objectMapperWithAutoType.writerWithDefaultPrettyPrinter().writeValueAsString(o);
            }
            else {
                return objectMapperWithAutoType.writeValueAsString(o);
            }
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Parse object to json error", e);
        }
    }

    @Override
    public <T> T parse(String json, Class<T> type, boolean ignoreAutoType) {
        try {
            if (json != null && "[]".equals(json)) {
                return (T) (new ArrayList(0));
            }
            if (ignoreAutoType) {
                return objectMapper.readValue(json, type);
            }
            else {
                return objectMapperWithAutoType.readValue(json, type);
            }
        } catch (IOException e) {
            throw new RuntimeException("Parse json to object error", e);
        }
    }
}