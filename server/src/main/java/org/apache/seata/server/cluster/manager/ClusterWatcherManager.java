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
package org.apache.seata.server.cluster.manager;

import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.server.cluster.listener.ClusterChangeEvent;
import org.apache.seata.server.cluster.listener.ClusterChangeListener;
import org.apache.seata.server.cluster.watch.Watcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 */
@Component
public class ClusterWatcherManager implements ClusterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Map<String, Queue<Watcher<?>>> WATCHERS = new ConcurrentHashMap<>();

    private static final Map<String, Long> GROUP_UPDATE_TIME = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Responds to monitors that time out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            for (String group : WATCHERS.keySet()) {
                Optional.ofNullable(WATCHERS.remove(group))
                    .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                        if (System.currentTimeMillis() >= watcher.getTimeout()) {
                            HttpServletResponse httpServletResponse =
                                (HttpServletResponse)((AsyncContext)watcher.getAsyncContext()).getResponse();
                            watcher.setDone(true);
                            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                            ((AsyncContext)watcher.getAsyncContext()).complete();
                        }
                        if (!watcher.isDone()) {
                            // Re-register
                            registryWatcher(watcher);
                        }
                    }));
            }
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        if (event.getTerm() > 0) {
            GROUP_UPDATE_TIME.put(event.getGroup(), event.getTerm());
            // Notifications are made of changes in cluster information
            Optional.ofNullable(WATCHERS.remove(event.getGroup()))
                .ifPresent(watchers -> watchers.parallelStream().forEach(this::notify));
        }
    }

    private void notify(Watcher<?> watcher) {
        AsyncContext asyncContext = (AsyncContext)watcher.getAsyncContext();
        HttpServletResponse httpServletResponse = (HttpServletResponse)asyncContext.getResponse();
        watcher.setDone(true);
        if (logger.isDebugEnabled()) {
            logger.debug("notify cluster change event to: {}", asyncContext.getRequest().getRemoteAddr());
        }
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        asyncContext.complete();
    }

    public void registryWatcher(Watcher<?> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TIME.get(group);
        if (term == null || watcher.getTerm() >= term) {
            WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>()).add(watcher);
        } else {
            notify(watcher);
        }
    }

}
