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
import org.apache.seata.common.rpc.http.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.fasterxml.jackson.databind.SerializationFeature.FAIL_ON_EMPTY_BEANS;

public class ParameterParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterParser.class);

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).configure(FAIL_ON_EMPTY_BEANS, false);

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
        if (parameterType.equals(HttpContext.class)) {
            return httpContext;
        } else if (ParamMetaData.ParamConvertType.MODEL_ATTRIBUTE.equals(paramConvertType)) {
            JsonNode param = paramMap.get("param");
            return OBJECT_MAPPER.convertValue(param, parameterType);
        } else if (ParamMetaData.ParamConvertType.REQUEST_BODY.equals(paramConvertType)) {
            JsonNode body = paramMap.get("body");
            return OBJECT_MAPPER.convertValue(body, parameterType);
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
