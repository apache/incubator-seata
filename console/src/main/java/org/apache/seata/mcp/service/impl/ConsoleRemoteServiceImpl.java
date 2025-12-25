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
package org.apache.seata.mcp.service.impl;

import org.apache.seata.common.NamingServerLocalMarker;
import org.apache.seata.common.exception.AuthenticationFailedException;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.console.config.WebSecurityConfig;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.service.ConsoleApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ConditionalOnMissingBean(NamingServerLocalMarker.class)
@Service
public class ConsoleRemoteServiceImpl implements ConsoleApiService {

    private final JwtTokenUtils jwtTokenUtils;

    private final RestTemplate restTemplate;

    public ConsoleRemoteServiceImpl(JwtTokenUtils jwtTokenUtils, RestTemplate restTemplate) {
        this.jwtTokenUtils = jwtTokenUtils;
        this.restTemplate = restTemplate;
    }

    @Value("${seata.console.naming-space-url:http://127.0.0.1:%s}")
    private String NAMING_SPACE_URL;

    private final Logger logger = LoggerFactory.getLogger(ConsoleRemoteServiceImpl.class);

    @Value("${server.port:8081}")
    private String namingSpacePort;

    public String getToken() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            throw new AuthenticationFailedException("No right to be identified");
        }
        String originJwt = (String) auth.getCredentials();
        if (!jwtTokenUtils.validateToken(originJwt)) {
            throw new AuthenticationFailedException("Invalid token, please log back in to get a new token");
        }
        return WebSecurityConfig.TOKEN_PREFIX + originJwt;
    }

    public void setNamespaceHeaderAndPathParam(
            NameSpaceDetail nameSpaceDetail, HttpHeaders headers, Map<String, String> pathParams) {
        headers.add("x-seata-namespace", nameSpaceDetail.getNamespace());
        if (StringUtils.isNotBlank(nameSpaceDetail.getvGroup())) {
            if (pathParams != null) {
                pathParams.put("vGroup", nameSpaceDetail.getvGroup());
            }
            return;
        }
        if (nameSpaceDetail.getCluster() != null) {
            headers.add("x-seata-cluster", nameSpaceDetail.getCluster());
        }
    }

    @Override
    public String getCallNameSpace(
            String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, getToken());
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP GET request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP GET Call NameSpace Failed", e);
            return "MCP GET Call NameSpace Failed: " + e.getMessage();
        }
    }

    @Override
    public String getCallTC(
            NameSpaceDetail nameSpaceDetail,
            String path,
            Object queryParams,
            Map<String, String> pathParams,
            HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        if (nameSpaceDetail == null || !nameSpaceDetail.isValid()) {
            return "If you have not specified the namespace of the TC/Server, specify the namespace first";
        } else {
            setNamespaceHeaderAndPathParam(nameSpaceDetail, headers, pathParams);
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, getToken());
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP GET request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP GET Call TC Failed", e);
            return "MCP GET Call TC Failed: " + e.getMessage();
        }
    }

    @Override
    public String deleteCallTC(
            NameSpaceDetail nameSpaceDetail,
            String path,
            Object queryParams,
            Map<String, String> pathParams,
            HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        if (nameSpaceDetail == null || !nameSpaceDetail.isValid()) {
            return "If you have not specified the namespace of the TC/Server, specify the namespace first";
        } else {
            setNamespaceHeaderAndPathParam(nameSpaceDetail, headers, pathParams);
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, getToken());
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP DELETE request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP DELETE Call TC Failed", e);
            return "MCP DELETE Call TC Failed: " + e.getMessage();
        }
    }

    @Override
    public String putCallTC(
            NameSpaceDetail nameSpaceDetail,
            String path,
            Object queryParams,
            Map<String, String> pathParams,
            HttpHeaders headers) {
        if (headers == null) {
            headers = new HttpHeaders();
        }
        if (nameSpaceDetail == null || !nameSpaceDetail.isValid()) {
            return "If you have not specified the namespace of the TC/Server, specify the namespace first";
        } else {
            setNamespaceHeaderAndPathParam(nameSpaceDetail, headers, pathParams);
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, getToken());
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP PUT request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP Put Call TC Failed", e);
            return "MCP Put Call TC Failed: " + e.getMessage();
        }
    }

    private Map<String, Object> objectToQueryParamMap(Object obj) {
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

        Map<String, Object> paramMap = new HashMap<>();
        Class<?> clazz = obj.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value != null) {
                        paramMap.putIfAbsent(field.getName(), value);
                    }
                } catch (IllegalAccessException e) {
                    logger.warn("Failed to access field {}: {}", field.getName(), e.getMessage());
                }
            }
            clazz = clazz.getSuperclass();
        }
        return paramMap;
    }

    private String buildUrl(
            String baseUrl, String path, Map<String, String> pathParams, Map<String, Object> queryParams) {

        UriComponentsBuilder builder =
                UriComponentsBuilder.fromUriString(baseUrl).path(path);

        if (pathParams != null && !pathParams.isEmpty()) {
            for (Map.Entry<String, String> entry : pathParams.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
        }

        if (queryParams != null && !queryParams.isEmpty()) {
            for (Map.Entry<String, Object> entry : queryParams.entrySet()) {
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
}
