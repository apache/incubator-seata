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

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParameterParser {

    private static final Logger LOGGER = LoggerFactory.getLogger(ParameterParser.class);

    public static ObjectNode convertParamMap(Map<String, List<String>> paramMap) throws JsonProcessingException {
        ObjectMapper objectMapper = new ObjectMapper();
        ObjectNode paramNode = objectMapper.createObjectNode();
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

    public static Object[] getArgValues(ParamMetaData[] paramMetaDatas, Method handleMethod, ObjectNode paramMap) throws JsonProcessingException {
        Class<?>[] parameterTypes = handleMethod.getParameterTypes();
        Parameter[] parameters = handleMethod.getParameters();
        return getParameters(parameterTypes, paramMetaDatas, parameters, paramMap);
    }

    private static Object[] getParameters(Class<?>[] parameterTypes, ParamMetaData[] paramMetaDatas, Parameter[] parameters, ObjectNode paramMap) throws JsonProcessingException {
        int length = parameterTypes.length;
        Object[] ret = new Object[length];
        for (int i = 0; i < length; i++) {
            Class<?> parameterType = parameterTypes[i];
            String parameterName = parameters[i].getName();
            ParamMetaData paramMetaData = paramMetaDatas[i];
            ret[i] = getArgValue(parameterType, parameterName, paramMetaData, paramMap);
            if (!parameterType.isAssignableFrom(ret[i].getClass())) {
                LOGGER.error("[HttpDispatchHandler] not compatible parameter type, expect {}, but {}", parameterType, ret[i].getClass());
                ret[i] = null;
            }
        }

        return ret;
    }


    private static Object getArgValue(Class<?> parameterType, String parameterName, ParamMetaData paramMetaData, ObjectNode paramMap) throws JsonProcessingException {
        ParamMetaData.ParamConvertType paramConvertType = paramMetaData.getParamConvertType();
        ObjectMapper objectMapper = new ObjectMapper();
        if (parameterType.equals(Channel.class)) {
            JsonNode jsonNode = paramMap.get("channel");
            paramMap.putPOJO("channel", null);
            return objectMapper.convertValue(jsonNode, Channel.class);
        } else if (ParamMetaData.ParamConvertType.MODEL_ATTRIBUTE.equals(paramConvertType)) {
            JsonNode param = paramMap.get("param");
            return objectMapper.convertValue(param, parameterType);
        } else if (ParamMetaData.ParamConvertType.REQUEST_BODY.equals(paramConvertType)) {
            JsonNode body = paramMap.get("body");
            return objectMapper.convertValue(body, parameterType);
        } else {
            return paramMap.get(parameterName).asText(null);
        }
    }
}
