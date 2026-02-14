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
package org.apache.seata.saga.engine.serializer.impl;

import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.json.JsonSerializerFactory;
import org.apache.seata.saga.engine.serializer.Serializer;
import org.apache.seata.saga.statelang.domain.DomainConstants;

/**
 * Parameter serializer based on Fastjson
 *
 */
public class ParamsSerializer implements Serializer<Object, String> {

    private String jsonParserName = DomainConstants.DEFAULT_JSON_PARSER;

    @Override
    public String serialize(Object params) {
        if (params != null) {
            JsonSerializer jsonSerializer = JsonSerializerFactory.getSerializer(jsonParserName);
            if (jsonSerializer == null) {
                throw new RuntimeException("Cannot find JsonSerializer by name: " + jsonParserName);
            }
            return jsonSerializer.toJSONString(params, false);
        }
        return null;
    }

    @Override
    public Object deserialize(String json) {
        if (json != null) {
            JsonSerializer jsonSerializer = JsonSerializerFactory.getSerializer(jsonParserName);
            if (jsonSerializer == null) {
                throw new RuntimeException("Cannot find JsonSerializer by name: " + jsonParserName);
            }
            return jsonSerializer.parseObject(json, Object.class, false);
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
