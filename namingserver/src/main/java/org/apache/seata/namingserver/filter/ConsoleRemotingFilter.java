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

import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;
import javax.servlet.AsyncContext;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.seata.common.metadata.Node;
import org.apache.seata.common.util.CollectionUtils;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.namingserver.manager.NamingManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.web.client.AsyncRestTemplate;


import static org.apache.seata.namingserver.contants.NamingConstant.CONSOLE_PATTERN;

public class ConsoleRemotingFilter implements Filter {

    private final NamingManager namingManager;

    private final AsyncRestTemplate asyncRestTemplate;

    private final Pattern urlPattern = Pattern.compile(CONSOLE_PATTERN);

    private final Logger logger = LoggerFactory.getLogger(ConsoleRemotingFilter.class);

    public ConsoleRemotingFilter(NamingManager namingManager, AsyncRestTemplate asyncRestTemplate) {
        this.namingManager = namingManager;
        this.asyncRestTemplate = asyncRestTemplate;
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        if (servletRequest instanceof HttpServletRequest) {
            if (urlPattern.matcher(((HttpServletRequest)servletRequest).getRequestURI()).matches()) {
                CachedBodyHttpServletRequest request =
                    new CachedBodyHttpServletRequest((HttpServletRequest)servletRequest);
                HttpServletResponse response = (HttpServletResponse)servletResponse;
                String namespace = request.getHeader("x-seata-namespace");
                String cluster = request.getHeader("x-seata-cluster");
                String vgroup = request.getParameter("vgroup");
                if (StringUtils.isNotBlank(namespace)
                    && (StringUtils.isNotBlank(cluster) || StringUtils.isNotBlank(vgroup))) {
                    List<Node> list = null;
                    if (StringUtils.isNotBlank(vgroup)) {
                        list = namingManager.getInstancesByVgroupAndNamespace(namespace, vgroup);
                    } else if (StringUtils.isNotBlank(cluster)) {
                        list = namingManager.getInstances(namespace, cluster);
                    }
                    if (CollectionUtils.isNotEmpty(list)) {
                        // Randomly select a node from the list
                        Node node = list.get(ThreadLocalRandom.current().nextInt(list.size()));
                        Node.Endpoint controlEndpoint = node.getControl();

                        if (controlEndpoint != null) {
                            // Construct the target URL
                            String targetUrl = "http://" + controlEndpoint.getHost() + ":" + controlEndpoint.getPort()
                                + request.getRequestURI()
                                + (request.getQueryString() != null ? "?" + request.getQueryString() : "");

                            // Copy headers from the original request
                            HttpHeaders headers = new HttpHeaders();
                            Collections.list(request.getHeaderNames())
                                .forEach(headerName -> headers.add(headerName, request.getHeader(headerName)));

                            // Create the HttpEntity with headers and body
                            HttpEntity<byte[]> httpEntity = new HttpEntity<>(request.getCachedBody(), headers);

                            // Forward the request
                            AsyncContext asyncContext = servletRequest.startAsync();
                            asyncContext.setTimeout(5000L);
                            ListenableFuture<ResponseEntity<byte[]>> responseEntityFuture = asyncRestTemplate.exchange(
                                URI.create(targetUrl), Objects.requireNonNull(HttpMethod.resolve(request.getMethod())),
                                httpEntity, byte[].class);
                            responseEntityFuture.addCallback(new ListenableFutureCallback<ResponseEntity<byte[]>>() {
                                @Override
                                public void onFailure(Throwable ex) {
                                    try {
                                        logger.error(ex.getMessage(), ex);
                                        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                                    } finally {
                                        asyncContext.complete();
                                    }
                                }

                                @Override
                                public void onSuccess(ResponseEntity<byte[]> responseEntity) {
                                    // Copy response headers and status code
                                    responseEntity.getHeaders().forEach((key, value) -> {
                                        value.forEach(v -> response.addHeader(key, v));
                                    });
                                    response.setStatus(responseEntity.getStatusCodeValue());
                                    // Write response body
                                    Optional.ofNullable(responseEntity.getBody()).ifPresent(body -> {
                                        try (ServletOutputStream outputStream = response.getOutputStream()) {
                                            outputStream.write(body);
                                            outputStream.flush();
                                        } catch (IOException e) {
                                            logger.error(e.getMessage(), e);
                                        }
                                    });
                                    asyncContext.complete();
                                }
                            });
                            return;
                        }
                    }
                }
            }
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }

}
