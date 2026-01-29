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

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.JSONWriter;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.JsonAllowlistManager;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.loader.LoadLevel;

import java.lang.reflect.Type;

/**
 * Fastjson2 implementation of JsonSerializer
 */
@LoadLevel(name = Fastjson2JsonSerializer.NAME)
public class Fastjson2JsonSerializer implements JsonSerializer {

    public static final String NAME = "fastjson2";

    private static final JSONWriter.Feature[] SERIALIZER_FEATURES =
            new JSONWriter.Feature[] {JSONWriter.Feature.WriteClassName};

    private static final JSONWriter.Feature[] SERIALIZER_FEATURES_PRETTY =
            new JSONWriter.Feature[] {JSONWriter.Feature.WriteClassName, JSONWriter.Feature.PrettyFormat};

    private static final JSONWriter.Feature[] FEATURES_PRETTY =
            new JSONWriter.Feature[] {JSONWriter.Feature.PrettyFormat};

    @Override
    public String toJSONString(Object object) {
        try {
            return JSON.toJSONString(object);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson2 serialize error", e);
        }
    }

    @Override
    public <T> T parseObject(String text, Class<T> clazz) {
        if (text == null || clazz == null) {
            return null;
        }
        try {
            return JSON.parseObject(text, clazz);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson2 deserialize error", e);
        }
    }

    @Override
    public <T> T parseObjectWithType(String text, Type type) {
        if (text == null || type == null) {
            return null;
        }
        try {
            return JSON.parseObject(text, type);
        } catch (Exception e) {
            throw new JsonParseException("Fastjson2 deserialize error", e);
        }
    }

    @Override
    public boolean useAutoType(String json) {
        return json != null && json.contains("\"@type\"");
    }

    @Override
    public String toJSONString(Object object, boolean prettyPrint) {
        return toJSONString(object, false, prettyPrint);
    }

    @Override
    public String toJSONString(Object object, boolean ignoreAutoType, boolean prettyPrint) {
        try {
            if (prettyPrint) {
                if (ignoreAutoType) {
                    return JSON.toJSONString(object, FEATURES_PRETTY);
                } else {
                    return JSON.toJSONString(object, SERIALIZER_FEATURES_PRETTY);
                }
            } else {
                if (ignoreAutoType) {
                    return JSON.toJSONString(object);
                } else {
                    return JSON.toJSONString(object, SERIALIZER_FEATURES);
                }
            }
        } catch (Exception e) {
            throw new JsonParseException("Fastjson2 serialize error", e);
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

            // Check allowlist when AutoType is enabled
            if (!ignoreAutoType && useAutoType(text)) {
                checkAutoTypeClasses(text);
            }

            if (ignoreAutoType) {
                return JSON.parseObject(text, type);
            } else {
                return JSON.parseObject(text, type, JSONReader.Feature.SupportAutoType);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonParseException("Fastjson2 deserialize error", e);
        }
    }

    /**
     * Check all @type classes in JSON against allowlist
     */
    private void checkAutoTypeClasses(String json) {
        int index = 0;
        while ((index = json.indexOf("\"@type\"", index)) >= 0) {
            String className = extractTypeValue(json, index);
            if (className != null) {
                JsonAllowlistManager.getInstance().checkClass(className);
            }
            index++;
        }
    }

    /**
     * Extract @type value from JSON string
     */
    private String extractTypeValue(String json, int typeIndex) {
        int colonIndex = json.indexOf(':', typeIndex);
        if (colonIndex < 0) {
            return null;
        }
        int startQuote = json.indexOf('"', colonIndex);
        if (startQuote < 0) {
            return null;
        }
        int endQuote = json.indexOf('"', startQuote + 1);
        if (endQuote < 0) {
            return null;
        }
        return json.substring(startQuote + 1, endQuote);
    }
}
