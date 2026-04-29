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
package org.apache.seata.namingserver.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.mcp.core.props.NameSpaceDetail;
import org.apache.seata.mcp.exception.ServiceCallException;
import org.apache.seata.mcp.service.ConsoleApiService;
import org.apache.seata.namingserver.manager.NamingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;
import static org.apache.seata.mcp.core.utils.UrlUtils.buildUrl;
import static org.apache.seata.mcp.core.utils.UrlUtils.objectToQueryParamMap;

@ConditionalOnBean(NamingServerLocalMarkerImpl.class)
@Primary
@Service
public class ConsoleLocalServiceImpl implements ConsoleApiService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleLocalServiceImpl.class);

    private final NamingManager namingManager;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public ConsoleLocalServiceImpl(NamingManager namingManager, RestClient restClient, ObjectMapper objectMapper) {
        this.namingManager = namingManager;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        LOGGER.info("ConsoleLocalServiceImpl initialized.");
    }

    public String getResult(
            NameSpaceDetail nameSpaceDetail,
            HttpMethod httpMethod,
            String path,
            Object objectQueryParams,
            Map<String, String> queryParams,
            HttpHeaders headers) {
        String namespace = nameSpaceDetail.getNamespace();
        String cluster = nameSpaceDetail.getCluster();
        String vgroup = nameSpaceDetail.getvGroup();
        if (StringUtils.isNotBlank(namespace) && (StringUtils.isNotBlank(cluster) || StringUtils.isNotBlank(vgroup))) {
            List<NamingServerNode> list = Collections.emptyList();
            if (StringUtils.isNotBlank(vgroup)) {
                list = namingManager.getInstancesByVgroupAndNamespace(
                        namespace, vgroup, HttpMethod.GET.equals(httpMethod));
            } else if (StringUtils.isNotBlank(cluster)) {
                list = namingManager.getInstances(namespace, cluster);
            }
            if (CollectionUtils.isNotEmpty(list)) {
                // Randomly select a node from the list
                NamingServerNode node = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                Node.Endpoint controlEndpoint = node.getControl();
                if (controlEndpoint != null) {
                    // Construct the target URL
                    String baseUrl = "http://" + controlEndpoint.getHost() + ":" + controlEndpoint.getPort();
                    Map<String, Object> queryParamsMap = objectToQueryParamMap(objectQueryParams, objectMapper);
                    String targetUrl = buildUrl(baseUrl, path, queryParams, queryParamsMap);
                    if (node.getRole() == ClusterRole.LEADER) {
                        headers.add(RAFT_GROUP_HEADER, node.getUnit());
                    }
                    HttpEntity<String> entity = new HttpEntity<>(headers);
                    String responseBody;
                    try {
                        ResponseEntity<String> response = executeRequest(targetUrl, httpMethod, entity);

                        responseBody = response.getBody();

                        if (!response.getStatusCode().is2xxSuccessful()) {
                            String errorMsg = String.format(
                                    "MCP request failed with status: %s, response: %s",
                                    response.getStatusCode(), response.getBody());
                            LOGGER.warn(errorMsg);
                            throw new ServiceCallException(errorMsg, response.getStatusCode());
                        }
                        return responseBody;
                    } catch (RestClientException e) {
                        String errorMsg = "MCP Call TC Failed.";
                        LOGGER.error(errorMsg, e);
                        throw new ServiceCallException(errorMsg);
                    }
                }
            }
            throw new IllegalArgumentException("Couldn't find target node url");
        }
        throw new IllegalArgumentException("Invalid NameSpace Detail");
    }

    private ResponseEntity<String> executeRequest(String targetUrl, HttpMethod httpMethod, HttpEntity<String> entity)
            throws RestClientException {
        return restClient
                .method(Objects.requireNonNull(httpMethod))
                .uri(Objects.requireNonNull(targetUrl))
                .headers(headers -> headers.addAll(entity.getHeaders()))
                .exchange((request, response) -> new ResponseEntity<>(
                        readResponseBody(response), response.getHeaders(), response.getStatusCode()));
    }

    private String readResponseBody(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response)
            throws IOException {
        return response.getBody() == null ? "" : StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
    }

    @Override
    public String getCallTC(
            NameSpaceDetail nameSpaceDetail,
            String path,
            Object objectQueryParams,
            Map<String, String> queryParams,
            HttpHeaders headers) {
        return getResult(nameSpaceDetail, HttpMethod.GET, path, objectQueryParams, queryParams, headers);
    }

    @Override
    public String deleteCallTC(
            NameSpaceDetail nameSpaceDetail,
            String path,
            Object objectQueryParams,
            Map<String, String> queryParams,
            HttpHeaders headers) {
        return getResult(nameSpaceDetail, HttpMethod.DELETE, path, objectQueryParams, queryParams, headers);
    }

    @Override
    public String putCallTC(
            NameSpaceDetail nameSpaceDetail,
            String path,
            Object objectQueryParams,
            Map<String, String> queryParams,
            HttpHeaders headers) {
        return getResult(nameSpaceDetail, HttpMethod.PUT, path, objectQueryParams, queryParams, headers);
    }

    @Override
    public String getCallNameSpace(String path) {
        String namespace;
        try {
            namespace = objectMapper.writeValueAsString(namingManager.namespace());
        } catch (JsonProcessingException e) {
            LOGGER.error("Get NameSpace failed: {}", e.getMessage());
            return "Failed to get namespace";
        }
        return namespace;
    }
}
