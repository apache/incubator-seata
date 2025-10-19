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
package org.apache.seata.rm.datasource.undo.parser;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.parser.DefaultJSONParser;
import com.alibaba.fastjson.parser.JSONToken;
import com.alibaba.fastjson.parser.ParserConfig;
import com.alibaba.fastjson.parser.deserializer.ObjectDeserializer;
import com.alibaba.fastjson.serializer.JSONSerializer;
import com.alibaba.fastjson.serializer.ObjectSerializer;
import com.alibaba.fastjson.serializer.SerializeConfig;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import org.apache.seata.common.Constants;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.rm.datasource.sql.serial.SerialArray;
import org.apache.seata.rm.datasource.undo.BranchUndoLog;
import org.apache.seata.rm.datasource.undo.UndoLogParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.sql.SQLException;

/**
 * The type Json based undo log parser.
 *
 */
@LoadLevel(name = FastjsonUndoLogParser.NAME)
public class FastjsonUndoLogParser implements UndoLogParser, Initialize {

    public static final String NAME = "fastjson";

    private static final Logger LOGGER = LoggerFactory.getLogger(FastjsonUndoLogParser.class);

    private final SimplePropertyPreFilter filter = new SimplePropertyPreFilter();

    @Override
    public void init() {
        filter.getExcludes().add("tableMeta");

        // Register SerialArray serializer and deserializer
        SerializeConfig.getGlobalInstance().put(SerialArray.class, new SerialArraySerializer());
        ParserConfig.getGlobalInstance().putDeserializer(SerialArray.class, new SerialArrayDeserializer());
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public byte[] getDefaultContent() {
        return "{}".getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public byte[] encode(BranchUndoLog branchUndoLog) {
        String json = JSON.toJSONString(
                branchUndoLog, filter, SerializerFeature.WriteClassName, SerializerFeature.WriteDateUseDateFormat);
        return json.getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        String text = new String(bytes, Constants.DEFAULT_CHARSET);
        return JSON.parseObject(text, BranchUndoLog.class);
    }

    /**
     * Custom Fastjson serializer for SerialArray
     */
    private static class SerialArraySerializer implements ObjectSerializer {
        @Override
        public void write(JSONSerializer serializer, Object object, Object fieldName, Type fieldType, int features)
                throws IOException {
            if (object == null) {
                serializer.writeNull();
                return;
            }

            SerialArray serialArray = (SerialArray) object;
            JSONObject json = new JSONObject();

            try {
                json.put("baseType", serialArray.getBaseType());
            } catch (SQLException e) {
                json.put("baseType", null);
            }

            try {
                json.put("baseTypeName", serialArray.getBaseTypeName());
            } catch (SQLException e) {
                json.put("baseTypeName", null);
            }

            Object[] elements = serialArray.getElements();
            if (elements != null) {
                JSONArray jsonArray = new JSONArray();
                for (Object element : elements) {
                    jsonArray.add(element);
                }
                json.put("elements", jsonArray);
            } else {
                json.put("elements", null);
            }

            serializer.write(json);
        }
    }

    /**
     * Custom Fastjson deserializer for SerialArray
     */
    private static class SerialArrayDeserializer implements ObjectDeserializer {
        @Override
        public SerialArray deserialze(DefaultJSONParser parser, Type type, Object fieldName) {
            try {
                JSONObject json = parser.parseObject();
                if (json == null) {
                    return null;
                }

                SerialArray serialArray = new SerialArray();

                Object baseType = json.get("baseType");
                if (baseType instanceof Number) {
                    serialArray.setBaseType(((Number) baseType).intValue());
                }

                Object baseTypeName = json.get("baseTypeName");
                if (baseTypeName instanceof String) {
                    serialArray.setBaseTypeName((String) baseTypeName);
                }

                Object elementsObj = json.get("elements");
                if (elementsObj instanceof JSONArray) {
                    JSONArray elementsArray = (JSONArray) elementsObj;
                    Object[] elements = new Object[elementsArray.size()];
                    for (int i = 0; i < elementsArray.size(); i++) {
                        elements[i] = elementsArray.get(i);
                    }
                    serialArray.setElements(elements);
                }

                return serialArray;
            } catch (Exception e) {
                LOGGER.error("deserialize SerialArray error: {}", e.getMessage(), e);
                return null;
            }
        }

        @Override
        public int getFastMatchToken() {
            return JSONToken.LBRACE;
        }
    }
}
