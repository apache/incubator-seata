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
package org.apache.seata.mcp.core.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UrlUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(UrlUtils.class);

    public static String buildUrl(
            String baseUrl, String path, Map<String, String> queryStringParams, Map<String, Object> queryObjectParams) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(baseUrl).path(path);

        if (queryStringParams != null && !queryStringParams.isEmpty()) {
            for (Map.Entry<String, String> entry : queryStringParams.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }

        if (queryObjectParams != null && !queryObjectParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : queryObjectParams.entrySet()) {
                if (entry.getValue() instanceof Iterable) {
                    for (Object value : (Iterable<?>) entry.getValue()) {
                        builder.queryParam(entry.getKey(), value);
                    }
                } else if (entry.getValue() != null
                        && entry.getValue().getClass().isArray()) {
                    Object[] array = (Object[]) entry.getValue();
                    for (Object value : array) {
                        builder.queryParam(entry.getKey(), value);
                    }
                } else {
                    builder.queryParam(entry.getKey(), entry.getValue());
                }
            }
        }

        return builder.build().toUriString();
    }

    public static Map<String, Object> objectToQueryParamMap(Object obj, ObjectMapper objectMapper) {
        if (obj == null) {
            return Collections.emptyMap();
        }
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
            Map<String, Object> result = new HashMap<>(map.size());
            map.forEach((k, v) -> {
                if (k != null && v != null) {
                    result.put(k.toString(), v);
                }
            });
            return result;
        }

        try {
            return objectMapper.convertValue(obj, new TypeReference<Map<String, Object>>() {});
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Failed to convert object to map: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }
}
