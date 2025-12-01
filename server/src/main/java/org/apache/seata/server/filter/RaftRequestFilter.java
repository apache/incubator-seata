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
package org.apache.seata.server.filter;

import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http2.Http2Headers;
import org.apache.seata.common.loader.LoadLevel;
import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.HttpRequestFilterException;
import org.apache.seata.core.rpc.netty.http.SimpleHttp2Request;
import org.apache.seata.core.rpc.netty.http.filter.HttpFilterContext;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilter;
import org.apache.seata.core.rpc.netty.http.filter.HttpRequestFilterChain;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.store.StoreConfig;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;

/**
 * Raft Leader Write Filter for HTTP requests
 */
@LoadLevel(name = "RaftRequest", order = 1)
@Component
public class RaftRequestFilter implements HttpRequestFilter, ApplicationListener<ClusterChangeEvent> {

    private static final Map<String, Boolean> GROUP_PREVENT = new ConcurrentHashMap<>();

    @Override
    public void doFilter(HttpFilterContext<?> context, HttpRequestFilterChain chain) throws HttpRequestFilterException {
        String uri = getUri(context);
        if (!isTargetUri(uri)) {
            chain.doFilter(context);
            return;
        }
        String group = getGroup(context);
        if (group != null) {
            SeataClusterContext.bindGroup(group);
        }
        try {
            String method;
            if (context.getRequest() instanceof SimpleHttp2Request) {
                SimpleHttp2Request request = (SimpleHttp2Request) context.getRequest();
                method = request.getMethod().name();
            } else {
                HttpRequest request = (HttpRequest) context.getRequest();
                method = request.method().name();
            }
            if (!"GET".equalsIgnoreCase(method)) {
                if (!isPass(group)) {
                    throw new HttpRequestFilterException(
                            "The current TC is not a leader node, interrupt processing of transactions!");
                }
            }
            chain.doFilter(context);
        } finally {
            SeataClusterContext.unbindGroup();
        }
    }

    private String getGroup(HttpFilterContext<?> context) {
        // Try to get from query params
        Map<String, List<String>> params = context.getParamWrapper().getAllParamsAsMultiMap();
        List<String> unitParams = params.get("unit");
        if (unitParams != null && !unitParams.isEmpty()) {
            return unitParams.get(0);
        }
        // Try to get the group header from HttpRequest or SimpleHttp2Request
        if (context.getRequest() instanceof io.netty.handler.codec.http.HttpRequest) {
            io.netty.handler.codec.http.HttpRequest httpRequest =
                    (io.netty.handler.codec.http.HttpRequest) context.getRequest();
            return httpRequest.headers().get(RAFT_GROUP_HEADER);
        } else if (context.getRequest() instanceof SimpleHttp2Request) {
            Http2Headers http2Headers = ((SimpleHttp2Request) context.getRequest()).getHeaders();
            CharSequence headerValue = http2Headers.get(RAFT_GROUP_HEADER);
            return headerValue != null ? headerValue.toString() : null;
        }
        return null;
    }

    @Override
    public boolean shouldApply() {
        return StoreConfig.getSessionMode() == SessionMode.RAFT;
    }

    @Override
    public void onApplicationEvent(ClusterChangeEvent event) {
        setPrevent(event.getGroup(), event.isLeader());
    }

    public static void setPrevent(String group, boolean prevent) {
        if (StoreConfig.getSessionMode() == SessionMode.RAFT) {
            GROUP_PREVENT.put(group, prevent);
        }
    }

    private boolean isPass(String group) {
        // Non-raft mode always allows requests
        return Optional.ofNullable(GROUP_PREVENT.get(group)).orElse(false);
    }

    private String getUri(HttpFilterContext<?> context) {
        // Extract URI from HttpRequest or path from SimpleHttp2Request
        if (context.getRequest() instanceof io.netty.handler.codec.http.HttpRequest) {
            io.netty.handler.codec.http.HttpRequest httpRequest =
                    (io.netty.handler.codec.http.HttpRequest) context.getRequest();
            return httpRequest.uri();
        } else if (context.getRequest() instanceof SimpleHttp2Request) {
            return ((SimpleHttp2Request) context.getRequest()).getPath();
        }
        return null;
    }

    private boolean isTargetUri(String uri) {
        if (StringUtils.isBlank(uri)) {
            return false;
        }
        return uri.startsWith("/api/v1/console/") || uri.startsWith("/vgroup/v1/");
    }
}
