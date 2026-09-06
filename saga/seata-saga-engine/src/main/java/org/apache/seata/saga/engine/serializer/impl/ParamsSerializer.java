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

import org.apache.seata.common.json.JsonUtil;
import org.apache.seata.saga.engine.serializer.Serializer;
import org.apache.seata.saga.statelang.domain.DomainConstants;

/**
 * Parameter serializer backed by the globally configured {@link JsonUtil} provider.
 *
 */
public class ParamsSerializer implements Serializer<Object, String> {

    private String jsonParserName = DomainConstants.DEFAULT_JSON_PARSER;

    @Override
    public String serialize(Object params) {
        if (params != null) {
            return JsonUtil.toJSONString(params, false);
        }
        return null;
    }

    @Override
    public Object deserialize(String json) {
        if (json != null) {
            return JsonUtil.parseObject(json, Object.class, false);
        }
        return null;
    }

    /**
     * @deprecated JSON serialization is configured globally through {@code json.serializerType}.
     */
    @Deprecated
    public String getJsonParserName() {
        return jsonParserName;
    }

    /**
     * @deprecated JSON serialization is configured globally through {@code json.serializerType}.
     */
    @Deprecated
    public void setJsonParserName(String jsonParserName) {
        this.jsonParserName = jsonParserName;
    }
}
