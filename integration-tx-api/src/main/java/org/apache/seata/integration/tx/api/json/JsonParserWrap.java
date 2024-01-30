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

import org.apache.seata.common.exception.JsonParseException;


public class JsonParserWrap implements JsonParser {

    private JsonParser jsonParser;

    public JsonParserWrap(JsonParser jsonParser) {
        this.jsonParser = jsonParser;
    }

    @Override
    public String toJSONString(Object object) {
        try {
            return this.jsonParser.toJSONString(object);
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        try {
            return this.jsonParser.parseObject(text, clazz);
        } catch (Exception e) {
            throw new JsonParseException(e);
        }
    }

    @Override
    public String getName() {
        return this.jsonParser.getName();
    }
}
