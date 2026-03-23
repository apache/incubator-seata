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

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.Feature;
import com.alibaba.fastjson.serializer.SerializerFeature;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.JsonAllowlistManager;
import org.apache.seata.common.json.JsonSerializer;
import org.apache.seata.common.loader.LoadLevel;

import java.lang.reflect.Type;

/**
 * FastJSON implementation of JsonSerializer
 */
@LoadLevel(name = FastjsonJsonSerializer.NAME)
public class FastjsonJsonSerializer implements JsonSerializer {

    private static final SerializerFeature[] SERIALIZER_FEATURES = new SerializerFeature[] {
        SerializerFeature.DisableCircularReferenceDetect,
        SerializerFeature.WriteDateUseDateFormat,
        SerializerFeature.WriteClassName
    };

    private static final SerializerFeature[] SERIALIZER_FEATURES_PRETTY = new SerializerFeature[] {
        SerializerFeature.DisableCircularReferenceDetect,
        SerializerFeature.WriteDateUseDateFormat,
        SerializerFeature.WriteClassName,
        SerializerFeature.PrettyFormat
    };

    private static final SerializerFeature[] FEATURES_PRETTY = new SerializerFeature[] {
        SerializerFeature.DisableCircularReferenceDetect,
        SerializerFeature.WriteDateUseDateFormat,
        SerializerFeature.PrettyFormat
    };

    private static final Feature[] READER_FEATURES_SUPPORT_AUTO_TYPE =
            new Feature[] {Feature.SupportAutoType, Feature.OrderedField};

    private static final Feature[] READER_FEATURES_IGNORE_AUTO_TYPE =
            new Feature[] {Feature.IgnoreAutoType, Feature.OrderedField};

    public static final String NAME = "fastjson";

    @Override
    public String toJSONString(Object object) {
        try {
            return JSON.toJSONString(object);
        } catch (Exception e) {
            throw new JsonParseException("FastJSON serialize error", e);
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
            throw new JsonParseException("FastJSON deserialize error", e);
        }
    }

    @Override
    public <T> T parseObjectWithType(String text, Type type) {
        if (text == null || type == null) {
            return null;
        }
        try {
            checkAutoTypeClasses(text);
            return JSON.parseObject(text, type);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonParseException("FastJSON deserialize error", e);
        }
    }

    // advanced methods for Saga
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
            throw new JsonParseException("FastJSON serialize error", e);
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

            if (!ignoreAutoType) {
                checkAutoTypeClasses(text);
            }

            if (ignoreAutoType) {
                return JSON.parseObject(text, type, READER_FEATURES_IGNORE_AUTO_TYPE);
            } else {
                return JSON.parseObject(text, type, READER_FEATURES_SUPPORT_AUTO_TYPE);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            throw new JsonParseException("FastJSON deserialize error", e);
        }
    }

    /**
     * Parse JSON with DisableSpecialKeyDetect (treats @type as a normal key without triggering
     * AutoType resolution) and check all real @type fields against the allowlist.
     */
    private void checkAutoTypeClasses(String json) {
        Object parsed = JSON.parse(json, Feature.DisableSpecialKeyDetect, Feature.OrderedField);
        if (parsed instanceof JSONObject) {
            checkJsonObject((JSONObject) parsed);
        } else if (parsed instanceof JSONArray) {
            checkJsonArray((JSONArray) parsed);
        }
    }

    private void checkJsonObject(JSONObject obj) {
        Object type = obj.get("@type");
        if (type instanceof String) {
            JsonAllowlistManager.getInstance().checkClass((String) type);
        }
        for (Object value : obj.values()) {
            if (value instanceof JSONObject) {
                checkJsonObject((JSONObject) value);
            } else if (value instanceof JSONArray) {
                checkJsonArray((JSONArray) value);
            }
        }
    }

    private void checkJsonArray(JSONArray arr) {
        for (Object item : arr) {
            if (item instanceof JSONObject) {
                checkJsonObject((JSONObject) item);
            } else if (item instanceof JSONArray) {
                checkJsonArray((JSONArray) item);
            }
        }
    }
}
