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
package org.apache.seata.core.rpc.netty.http;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.seata.common.rpc.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

/**
 * A utility class for parsing HTTP request parameters and converting them into Java objects.
 * Supports various parameter types including request params, request body, model attributes, etc.
 */
public class ParameterParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterParser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(FAIL_ON_EMPTY_BEANS, false);

    private static final String DEFAULT_NONE = "\n\t\t\n\t\t\n\ue000\ue001\ue002\n\t\t\t\t\n";


    public static ObjectNode convertParamMap(Map<String, List<String>> paramMap) {
        ObjectNode paramNode = OBJECT_MAPPER.createObjectNode();
        for (Map.Entry<String, List<String>> entry : paramMap.entrySet()) {
            List<String> list = entry.getValue();
            if (list == null || list.isEmpty()) {
                continue;
            }
            if (list.size() == 1) {
                paramNode.put(entry.getKey(), list.get(0));
            } else {
                ArrayNode arrayNode = paramNode.putArray(entry.getKey());
                for (String s : list) {
                    arrayNode.add(s);
                }
            }
        }
        return paramNode;
    }

    public static Object[] getArgValues(ParamMetaData[] paramMetaDatas, Method handleMethod, ObjectNode paramMap,
        HttpContext httpContext) throws JsonProcessingException {
        Class<?>[] parameterTypes = handleMethod.getParameterTypes();
        Parameter[] parameters = handleMethod.getParameters();
        return getParameters(parameterTypes, paramMetaDatas, parameters, paramMap, httpContext);
    }

    private static Object[] getParameters(Class<?>[] parameterTypes, ParamMetaData[] paramMetaDatas,
        Parameter[] parameters, ObjectNode paramMap, HttpContext httpContext) throws JsonProcessingException {
        int length = parameterTypes.length;
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String parameterName = parameters[i].getName();
            ParamMetaData paramMetaData = paramMetaDatas[i];
            Object value = getArgValue(parameterType, parameterName, paramMetaData, paramMap, httpContext);
            if (value != null && !parameterType.isAssignableFrom(value.getClass())) {
                LOGGER.error("[HttpDispatchHandler] not compatible parameter type, expect {}, but {}", parameterType,
                    ret[i].getClass());
                ret[i] = null;
            } else {
                ret[i] = value;
            }
        }

        return ret;
    }

    private static Object getArgValue(Class<?> parameterType, String parameterName, ParamMetaData paramMetaData,
        ObjectNode paramMap, HttpContext httpContext) {
        ParamMetaData.ParamConvertType paramConvertType = paramMetaData.getParamConvertType();
        if (HttpContext.class.equals(parameterType)) {
            return httpContext;
        } else if (ParamMetaData.ParamConvertType.MODEL_ATTRIBUTE.equals(paramConvertType)) {
            JsonNode param = paramMap.get("param");
            return OBJECT_MAPPER.convertValue(param, parameterType);
        } else if (ParamMetaData.ParamConvertType.REQUEST_BODY.equals(paramConvertType)) {
            JsonNode body = paramMap.get("body");
            return OBJECT_MAPPER.convertValue(body, parameterType);
        } else if (ParamMetaData.ParamConvertType.REQUEST_PARAM.equals(paramConvertType)) {
            String paramName = paramMetaData.getParamName();
            JsonNode jsonNode = Optional.ofNullable(paramMap.get("param"))
                    .map(body -> body.get(paramName))
                    .orElse(null);

            // Step 1: If body exists and contains paramName, use its value first
            if (jsonNode != null && !jsonNode.isNull()) {
                return OBJECT_MAPPER.convertValue(jsonNode, parameterType);
            }

            // Step 2: If the parameter is missing but a defaultValue is set, use the defaultValue
            String defaultValue = paramMetaData.getDefaultValue();
            if (defaultValue != null && !defaultValue.equals(DEFAULT_NONE)) {
                return OBJECT_MAPPER.convertValue(defaultValue, parameterType);
            }

            // Step 3: If the parameter is required but no value or defaultValue is provided, throw an exception
            if (paramMetaData.isRequired()) {
                throw new IllegalArgumentException("Required request parameter '" + paramName + "' is missing");
            }
            return null;
        } else {
            JsonNode paramNode = paramMap.get("param");
            if (paramNode != null) {
                JsonNode jsonNode = paramNode.get(parameterName);
                if (jsonNode != null) {
                    String value = jsonNode.asText(null);
                    return value != null ? OBJECT_MAPPER.convertValue(value, parameterType) : null;
                }
            }
            return null;
        }
    }
}
