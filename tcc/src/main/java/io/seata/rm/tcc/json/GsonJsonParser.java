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
package io.seata.rm.tcc.json;

import java.lang.reflect.Modifier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import io.seata.common.Constants;
import io.seata.common.loader.LoadLevel;
import io.seata.integration.tx.api.json.JsonParser;

/**
 */
@LoadLevel(name = Constants.GSON_JSON_PARSER_NAME)
public class GsonJsonParser implements JsonParser {

    private final Gson gson =
        new GsonBuilder().excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT).create();

    @Override
    public String toJSONString(Object object) {
        return gson.toJson(object);
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        return gson.fromJson(text, clazz);
    }

    @Override
    public String getName() {
        return Constants.GSON_JSON_PARSER_NAME;
    }
}
