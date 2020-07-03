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
package io.seata.saga.engine.serializer.impl;

import io.seata.saga.engine.serializer.Serializer;
import io.seata.saga.statelang.domain.DomainConstants;
import io.seata.saga.statelang.parser.JsonParser;
import io.seata.saga.statelang.parser.JsonParserFactory;

/**
 * Parameter serializer based on Fastjson
 *
 * @author lorne.cl
 */
public class ParamsSerializer implements Serializer<Object, String> {

    private String jsonParserName = DomainConstants.DEFAULT_JSON_PARSER;

    @Override
    public String serialize(Object params) {
        if (params != null) {
            JsonParser jsonParser = JsonParserFactory.getJsonParser(jsonParserName);
            if (jsonParser == null) {
                throw new RuntimeException("Cannot find JsonParer by name: " + jsonParserName);
            }
            return jsonParser.toJsonString(params, false);
        }
        return null;
    }

    @Override
    public Object deserialize(String json) {
        if (json != null) {
            JsonParser jsonParser = JsonParserFactory.getJsonParser(jsonParserName);
            if (jsonParser == null) {
                throw new RuntimeException("Cannot find JsonParer by name: " + jsonParserName);
            }
            return jsonParser.parse(json, Object.class, false);
        }
        return null;
    }

    public String getJsonParserName() {
        return jsonParserName;
    }

    public void setJsonParserName(String jsonParserName) {
        this.jsonParserName = jsonParserName;
    }
}