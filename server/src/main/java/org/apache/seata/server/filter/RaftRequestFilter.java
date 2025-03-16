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

import org.apache.seata.common.store.SessionMode;
import org.apache.seata.common.util.StringUtils;
import org.apache.seata.core.exception.TransactionException;
import org.apache.seata.core.exception.TransactionExceptionCode;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.raft.context.SeataClusterContext;
import org.apache.seata.server.console.exception.ConsoleException;
import org.apache.seata.server.store.StoreConfig;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static org.apache.seata.common.Constants.RAFT_GROUP_HEADER;

/**
 * Raft Leader Write Filter
 */
@Component
@Conditional(RaftCondition.class)
public class RaftRequestFilter implements Filter, ApplicationListener<ClusterChangeEvent> {

    private static final Map<String, Boolean> GROUP_PREVENT = new ConcurrentHashMap<>();

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {}

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
        throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest)servletRequest;
        String group = httpRequest.getParameter("unit");
        if (StringUtils.isBlank(group)) {
            group = httpRequest.getHeader(RAFT_GROUP_HEADER);
        }
        if (group != null) {
            SeataClusterContext.bindGroup(group);
        }
        try {
            String method = httpRequest.getMethod();
            if (!HttpMethod.GET.name().equalsIgnoreCase(method)) {
                if (!isPass(group)) {
                    throw new ConsoleException(
                        new TransactionException(TransactionExceptionCode.NotRaftLeader,
                            " The current TC is not a leader node, interrupt processing of transactions!"),
                        " The current TC is not a leader node, interrupt processing of transactions!");
                }
            }
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            SeataClusterContext.unbindGroup();
        }
    }

    @Override
    public void onApplicationEvent(ClusterChangeEvent event) {
        setPrevent(event.getGroup(), event.isLeader());
    }

    @Override
    public void destroy() {}

    public static void setPrevent(String group, boolean prevent) {
        if (StoreConfig.getSessionMode() == SessionMode.RAFT) {
            GROUP_PREVENT.put(group, prevent);
        }
    }

    private boolean isPass(String group) {
        // Non-raft mode always allows requests
        return Optional.ofNullable(GROUP_PREVENT.get(group)).orElse(false);
    }
}
