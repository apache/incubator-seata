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

import org.apache.seata.console.config.WebSecurityConfig;
import org.apache.seata.console.utils.JwtTokenUtils;
import org.apache.seata.mcp.handler.CustomResponseErrorHandler;
import org.apache.seata.mcp.service.MCPRPCService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import javax.annotation.PostConstruct;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an interface for MCP to call servers from RPC through Namingspace
 */
@Service
public class MCPRPCServiceImpl implements MCPRPCService {
    @Autowired
    private Environment env;

    @Autowired
    private JwtTokenUtils jwtTokenUtils;

    private final RestTemplate restTemplate = new RestTemplate();

    private final String NAMING_SPACE_URL = "http://127.0.0.1:%s/mcp";

    private final Logger logger = LoggerFactory.getLogger(MCPRPCServiceImpl.class);

    private String namingSpacePort = "";

    private final static User user = new User();

    private static String token = "";

    private final CustomResponseErrorHandler errorHandler = new CustomResponseErrorHandler();

    @PostConstruct
    public void init() {
        namingSpacePort = env.getProperty("server.port", "8081");
        restTemplate.setErrorHandler(errorHandler);
        user.username = env.getProperty("console.user.username","seata");
        user.password = env.getProperty("console.user.password","seata");
        getToken();
    }

    public void getToken() {
        // AuthenticationManager(default ProviderManager) #authenticate check Authentication
        Authentication authentication = new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword());
        // init token
        String originToken = jwtTokenUtils.createToken(authentication);
        token = WebSecurityConfig.TOKEN_PREFIX + originToken;
    }

    public static class User{
        private String username;
        private String password;
        public String getUsername() {
            return username;
        }
        public void setUsername(String username) {
            this.username = username;
        }
        public String getPassword() {
            return password;
        }
        public void setPassword(String password) {
            this.password = password;
        }
    }

    /**
     * Post Call the TC API based on the path
     */
    @Override
    public String postCallTC(String path, HttpHeaders headers, Object... args) {
        if(headers==null){
            headers = new HttpHeaders();
        }
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, null, null);
        HttpEntity<Object> entity = new HttpEntity<>(args, headers);
        String responseBody = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP POST request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP POST Call TC Failed:{}", e.getMessage());
            return responseBody;
        }
    }

    /**
     * Get Call the TC API based on the path
     */
    @Override
    public String getCallTC(String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers) {
        if(headers==null){
            headers = new HttpHeaders();
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, token);
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP GET request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP GET Call TC Failed: {}", e.getMessage());
            return responseBody;
        }
    }

    /**
     * Delete Call the TC API based on the path
     * @param path "Interface path"
     * @param queryParams "Query Parameters (Objects)"
     * @param pathParams "Path parameters（map）"
     * @param headers "Request header"
     * @return "Query results"
     */
    @Override
    public String deleteCallTC(String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers) {
        if(headers==null){
            headers = new HttpHeaders();
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, token);
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.DELETE, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP DELETE request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP DELETE Call TC Failed: {}", e.getMessage());
            return responseBody;
        }
    }

    @Override
    public String putCallTC(String path, Object queryParams, Map<String, String> pathParams, HttpHeaders headers) {
        if(headers==null){
            headers = new HttpHeaders();
        }
        headers.add(WebSecurityConfig.AUTHORIZATION_HEADER, token);
        Map<String, Object> queryParamsMap = objectToQueryParamMap(queryParams);
        String url = buildUrl(String.format(NAMING_SPACE_URL, namingSpacePort), path, pathParams, queryParamsMap);
        HttpEntity<String> entity = new HttpEntity<>(headers);
        String responseBody = null;
        try {
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.PUT, entity, String.class);

            responseBody = response.getBody();

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.warn("MCP PUT request returned non-success status: {}", response.getStatusCode());
            }
            return responseBody;
        } catch (RestClientException e) {
            logger.error("MCP Put Call TC Failed: {}", e.getMessage());
            return responseBody;
        }
    }

    /**
     * Convert an object to a query parameter, Map
     */
    private Map<String, Object> objectToQueryParamMap(Object obj) {
        if (obj == null) {
            return Collections.emptyMap();
        }

        Map<String, Object> paramMap = new HashMap<>();

        if (obj instanceof Map) {
            ((Map<?, ?>) obj).forEach((key, value) -> {
                if (key != null && value != null) {
                    paramMap.put(key.toString(), value);
                }
            });
            return paramMap;
        }

        Class<?> clazz = obj.getClass();
        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    paramMap.put(field.getName(), value);
                }
            } catch (IllegalAccessException e) {
                logger.warn("Failed to access field {}: {}", field.getName(), e.getMessage());
            }
        }

        return paramMap;
    }

    /**
     * Build the full URL
     */
    private String buildUrl(
            String baseUrl, String path, Map<String, String> pathParams, Map<String, Object> queryParams) {

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl).path(path);

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
