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
import com.alibaba.fastjson.serializer.SerializeWriter;
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
import java.math.BigDecimal;
import java.sql.SQLException;

import static java.sql.Types.BIGINT;
import static java.sql.Types.DECIMAL;
import static java.sql.Types.DOUBLE;
import static java.sql.Types.FLOAT;
import static java.sql.Types.INTEGER;
import static java.sql.Types.NUMERIC;
import static java.sql.Types.REAL;
import static java.sql.Types.SMALLINT;
import static java.sql.Types.TINYINT;

/**
 * The type Json based undo log parser.
 *
 */
@LoadLevel(name = FastjsonUndoLogParser.NAME)
public class FastjsonUndoLogParser implements UndoLogParser, Initialize {

    public static final String NAME = "fastjson";

    private static final Logger LOGGER = LoggerFactory.getLogger(FastjsonUndoLogParser.class);

    private final SimplePropertyPreFilter filter = new SimplePropertyPreFilter();
    final SerializeConfig serializeConfig = new SerializeConfig();
    final ParserConfig parserConfig = new ParserConfig();

    @Override
    public void init() {
        filter.getExcludes().add("tableMeta");

        // Register SerialArray serializer and deserializer
        serializeConfig.put(SerialArray.class, new SerialArraySerializer());
        parserConfig.putDeserializer(SerialArray.class, new SerialArrayDeserializer());
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
                branchUndoLog,
                serializeConfig,
                filter,
                SerializerFeature.WriteClassName,
                SerializerFeature.WriteDateUseDateFormat);
        return json.getBytes(Constants.DEFAULT_CHARSET);
    }

    @Override
    public BranchUndoLog decode(byte[] bytes) {
        String text = new String(bytes, Constants.DEFAULT_CHARSET);
        return JSON.parseObject(text, BranchUndoLog.class, parserConfig);
    }

    /**
     * Custom Fastjson serializer for SerialArray
     * Manually construct JSON structure while letting serializer handle elements properly
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
            SerializeWriter out = serializer.getWriter();

            out.write('{');

            // Write the correct @type information to ensure the deserializer is called
            out.writeFieldName("@type");
            out.writeString(serialArray.getClass().getName());
            out.write(',');

            // Write baseType
            out.writeFieldName("baseType");
            try {
                out.writeInt(serialArray.getBaseType());
            } catch (SQLException e) {
                out.writeNull();
            }
            out.write(',');

            // Write baseTypeName
            out.writeFieldName("baseTypeName");
            try {
                String baseTypeName = serialArray.getBaseTypeName();
                if (baseTypeName != null) {
                    out.writeString(baseTypeName);
                } else {
                    out.writeNull();
                }
            } catch (SQLException e) {
                out.writeNull();
            }
            out.write(',');

            // Writing elements - using a serializer to ensure correct JSON formatting and type handling
            out.writeFieldName("elements");
            serializer.write(serialArray.getElements());

            out.write('}');
        }
    }

    /**
     * Custom Fastjson deserializer for SerialArray
     * Enhanced with comprehensive type mapping based on SQL baseType
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

                // Remove the @type field if it exists (Fastjson automatically adds this)
                json.remove("@type");

                // Extract baseType for type conversion
                int baseType = 0;
                Object baseTypeObj = json.get("baseType");
                if (baseTypeObj instanceof Number) {
                    baseType = ((Number) baseTypeObj).intValue();
                    serialArray.setBaseType(baseType);
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
                        Object element = elementsArray.get(i);
                        elements[i] = convertElementByBaseType(element, baseType);
                    }
                    serialArray.setElements(elements);
                }

                return serialArray;
            } catch (Exception e) {
                LOGGER.error("deserialize SerialArray error: {}", e.getMessage(), e);
                return null;
            }
        }

        /**
         * Convert element to appropriate Java type based on SQL baseType
         */
        private Object convertElementByBaseType(Object element, int baseType) {
            if (element == null) {
                return null;
            }

            // If not a number, return as-is (String, Boolean, etc.)
            if (!(element instanceof Number)) {
                return element;
            }

            Number numElement = (Number) element;

            // Convert based on SQL type constants
            switch (baseType) {
                case TINYINT:
                    return numElement.byteValue();
                case SMALLINT:
                    return numElement.shortValue();
                case INTEGER:
                    return numElement.intValue();
                case BIGINT:
                    return numElement.longValue();
                case REAL:
                case FLOAT:
                    return numElement.floatValue();
                case DOUBLE:
                    return numElement.doubleValue();
                case DECIMAL:
                case NUMERIC:
                    return new BigDecimal(numElement.toString());
                default:
                    return element;
            }
        }

        @Override
        public int getFastMatchToken() {
            return JSONToken.LBRACE;
        }
    }
}
