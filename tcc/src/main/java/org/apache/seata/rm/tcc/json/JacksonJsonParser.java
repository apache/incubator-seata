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
package org.apache.seata.rm.tcc.json;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.seata.common.Constants;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.integration.tx.api.json.JsonParser;


@LoadLevel(name = Constants.JACKSON_JSON_PARSER_NAME)
public class JacksonJsonParser implements JsonParser {

    private final ObjectMapper mapper = new ObjectMapper();

    public JacksonJsonParser() {
        this.mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        this.mapper.activateDefaultTyping(this.mapper.getPolymorphicTypeValidator(),
            ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY);
        this.mapper.setConfig(this.mapper.getSerializationConfig().with(MapperFeature.PROPAGATE_TRANSIENT_MARKER));
        this.mapper.setConfig(this.mapper.getDeserializationConfig().with(MapperFeature.PROPAGATE_TRANSIENT_MARKER));
        this.mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public String toJSONString(Object object) throws IOException {
        return this.mapper.writeValueAsString(object);
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) throws IOException {
        return this.mapper.readValue(text, clazz);
    }

    @Override
    public String getName() {
        return Constants.JACKSON_JSON_PARSER_NAME;
    }
}
