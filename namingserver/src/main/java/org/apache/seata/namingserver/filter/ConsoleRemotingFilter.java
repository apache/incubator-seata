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
package org.apache.seata.namingserver.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.common.metadata.ClusterRole;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.metadata.namingserver.NamingServerNode;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.manager.NamingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;
import static org.apache.seata.namingserver.contants.NamingConstant.CONSOLE_PATTERN;

public class ConsoleRemotingFilter implements Filter {

    private final NamingManager namingManager;

    private final RestTemplate restTemplate;

    private final Pattern urlPattern = Pattern.compile(CONSOLE_PATTERN);

    private final Logger logger = LoggerFactory.getLogger(ConsoleRemotingFilter.class);

    public ConsoleRemotingFilter(NamingManager namingManager, RestTemplate restTemplate) {
        this.namingManager = namingManager;
        this.restTemplate = restTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            if (urlPattern
                    .matcher(((HttpServletRequest) servletRequest).getRequestURI())
                    .matches()) {
                CachedBodyHttpServletRequest request =
                        new CachedBodyHttpServletRequest((HttpServletRequest) servletRequest);
                HttpServletResponse response = (HttpServletResponse) servletResponse;
                String namespace = request.getHeader("x-seata-namespace");
                String cluster = request.getHeader("x-seata-cluster");
                String vgroup = request.getParameter("vgroup");
                if (StringUtils.isNotBlank(namespace)
                        && (StringUtils.isNotBlank(cluster) || StringUtils.isNotBlank(vgroup))) {
                    List<Node> list = null;
                    if (StringUtils.isNotBlank(vgroup)) {
                        list = namingManager.getInstancesByVgroupAndNamespace(
                                namespace,
                                vgroup,
                                StringUtils.equalsIgnoreCase(request.getMethod(), HttpMethod.GET.name()));
                    } else if (StringUtils.isNotBlank(cluster)) {
                        list = namingManager.getInstances(namespace, cluster);
                    }
                    if (CollectionUtils.isNotEmpty(list)) {
                        // Randomly select a node from the list
                        NamingServerNode node = (NamingServerNode)
                                list.get(ThreadLocalRandom.current().nextInt(list.size()));
                        Node.Endpoint controlEndpoint = node.getControl();
                        if (controlEndpoint != null) {
                            // Construct the target URL
                            String targetUrl = "http://" + controlEndpoint.getHost() + ":" + controlEndpoint.getPort()
                                    + request.getRequestURI()
                                    + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

                            // Copy headers from the original request
                            HttpHeaders headers = new HttpHeaders();
                            if (node.getRole() == ClusterRole.LEADER) {
                                headers.add(RAFT_GROUP_HEADER, node.getUnit());
                            }
                            Collections.list(request.getHeaderNames())
                                    .forEach(headerName -> headers.add(headerName, request.getHeader(headerName)));

                            // Create the HttpEntity with headers and body
                            HttpEntity<byte[]> httpEntity = new HttpEntity<>(request.getCachedBody(), headers);
                            HttpMethod httpMethod;
                            try {
                                httpMethod = HttpMethod.valueOf(request.getMethod());
                            } catch (IllegalArgumentException ex) {
                                logger.error("Unsupported HTTP method: {}", request.getMethod(), ex);
                                response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
                                return;
                            }
                            try {
                                ResponseEntity<byte[]> responseEntity = restTemplate.exchange(
                                        URI.create(targetUrl), httpMethod, httpEntity, byte[].class);
                                responseEntity.getHeaders().forEach((key, value) -> {
                                    value.forEach(v -> response.addHeader(key, v));
                                });
                                response.setStatus(
                                        responseEntity.getStatusCode().value());
                                Optional.ofNullable(responseEntity.getBody()).ifPresent(body -> {
                                    try (ServletOutputStream outputStream = response.getOutputStream()) {
                                        outputStream.write(body);
                                        outputStream.flush();
                                    } catch (IOException e) {
                                        logger.error(e.getMessage(), e);
                                    }
                                });
                            } catch (Exception ex) {
                                logger.error(ex.getMessage(), ex);
                                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                            }
                            return;
                        }
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }
}
