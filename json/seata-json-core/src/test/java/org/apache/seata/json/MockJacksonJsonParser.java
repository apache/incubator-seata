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

import org.apache.seata.common.Constants;
import org.apache.seata.common.loader.LoadLevel;

@LoadLevel(name = MockJacksonJsonParser.NAME)
public class MockJacksonJsonParser implements JsonParser {
    public static final String NAME = Constants.JACKSON_JSON_PARSER_NAME;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String toJsonString(Object o, boolean prettyPrint) {
        return "";
    }

    @Override
    public boolean useAutoType(String json) {
        return false;
    }

    @Override
    public String toJSONString(Object o) {
        return "";
    }

    @Override
    public String toJsonString(Object o, boolean ignoreAutoType, boolean prettyPrint) {
        return null;
    }

    @Override
    public <T> T parse(String json, Class<T> type) {
        return null;
    }

    @Override
    public <T> T parse(String json, Class<T> type, boolean ignoreAutoType) {
        return null;
    }
}
