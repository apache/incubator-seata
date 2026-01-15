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
package org.apache.seata.common.json.impl;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.loader.LoadLevel;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;

/**
 * Gson implementation of JsonSerializer
 */
@LoadLevel(name = GsonJsonSerializer.NAME)
public class GsonJsonSerializer implements JsonSerializer {

    private final Gson gson = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
            .create();

    private final Gson gsonPretty = new GsonBuilder()
            .excludeFieldsWithModifiers(Modifier.STATIC, Modifier.TRANSIENT)
            .setPrettyPrinting()
            .create();

    public static final String NAME = "gson";

    @Override
    public String toJSONString(Object object) {
        try {
            return gson.toJson(object);
        } catch (Exception e) {
            throw new JsonParseException("Gson serialize error", e);
        }
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        if (text == null || clazz == null) {
            return null;
        }
        try {
            return gson.fromJson(text, clazz);
        } catch (Exception e) {
            throw new JsonParseException("Gson deserialize error", e);
        }
    }

    @Override
    public <T> T parseObjectWithType(String text, Type type) {
        if (text == null || type == null) {
            return null;
        }
        try {
            return gson.fromJson(text, type);
        } catch (Exception e) {
            throw new JsonParseException("Gson deserialize error", e);
        }
    }

    // advanced methods for Saga
    @Override
    public boolean useAutoType(String json) {
        return false;
    }

    @Override
    public String toJSONString(Object object, boolean prettyPrint) {
        return toJSONString(object, false, prettyPrint);
    }

    @Override
    public String toJSONString(Object object, boolean ignoreAutoType, boolean prettyPrint) {
        try {
            if (prettyPrint) {
                return gsonPretty.toJson(object);
            } else {
                return gson.toJson(object);
            }
        } catch (Exception e) {
            throw new JsonParseException("Gson serialize error", e);
        }
    }

    @Override
    public <T> T parseObject(String text, Class<T> type, boolean ignoreAutoType) {
        if (text == null || type == null) {
            return null;
        }
        try {
            if ("[]".equals(text)) {
                return (T) new java.util.ArrayList<>();
            }
            return gson.fromJson(text, type);
        } catch (Exception e) {
            throw new JsonParseException("Gson deserialize error", e);
        }
    }
}
