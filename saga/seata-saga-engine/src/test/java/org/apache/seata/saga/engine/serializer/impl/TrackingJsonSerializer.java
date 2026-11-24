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
import org.apache.seata.common.loader.LoadLevel;

import java.lang.reflect.Type;

@LoadLevel(name = "tracking")
public class TrackingJsonSerializer implements JsonSerializer {

    @Override
    public String toJSONString(Object object) {
        return toJSONString(object, false);
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        return parseObject(text, clazz, false);
    }

    @Override
    public <T> T parseObjectWithType(String text, Type type) {
        return (T) ("parsed:" + text);
    }

    @Override
    public boolean useAutoType(String json) {
        return false;
    }

    @Override
    public String toJSONString(Object object, boolean prettyPrint) {
        return "tracking:" + object + ":" + prettyPrint;
    }

    @Override
    public String toJSONString(Object object, boolean ignoreAutoType, boolean prettyPrint) {
        return "tracking:" + object + ":" + prettyPrint;
    }

    @Override
    public <T> T parseObject(String json, Class<T> type, boolean ignoreAutoType) {
        return (T) ("parsed:" + json);
    }
}
