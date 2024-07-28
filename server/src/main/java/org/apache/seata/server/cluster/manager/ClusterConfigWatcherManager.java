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

import org.apache.seata.common.thread.NamedThreadFactory;
import org.apache.seata.server.cluster.listener.ClusterConfigChangeEvent;
import org.apache.seata.server.cluster.listener.ClusterConfigChangeListener;
import org.apache.seata.server.cluster.watch.ConfigWatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


/**
 *
 * The type of cluster config watcher manager.
 */
@Component
public class ClusterConfigWatcherManager implements ClusterConfigChangeListener {
    private final Logger LOGGER = LoggerFactory.getLogger(getClass());

    private static final Map<String/*namespace*/, Map<String/*dataId*/, Queue<ConfigWatcher<?>>>> WATCHERS = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Responds to monitors that time out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            for (String namespace : WATCHERS.keySet()) {
                Map<String, Queue<ConfigWatcher<?>>> dataIdWatchersMap = WATCHERS.get(namespace);
                for (String dataId : dataIdWatchersMap.keySet()) {
                    Optional.ofNullable(dataIdWatchersMap.remove(dataId))
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
            }
        }, 1, 1, TimeUnit.SECONDS);
    }
    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterConfigChangeEvent event) {
        String namespace = event.getNamespace();
        String dataId = event.getDataId();
        Map<String, Queue<ConfigWatcher<?>>> dataIdWatchersMap = WATCHERS.get(namespace);
        Optional.ofNullable(dataIdWatchersMap.remove(dataId))
                .ifPresent(watchers -> watchers.parallelStream().forEach(this::notify));
    }

    private void notify(ConfigWatcher<?> watcher) {
        AsyncContext asyncContext = (AsyncContext)watcher.getAsyncContext();
        HttpServletResponse httpServletResponse = (HttpServletResponse)asyncContext.getResponse();
        watcher.setDone(true);
        LOGGER.info("notify cluster config change event to: {}", asyncContext.getRequest().getRemoteAddr());
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("notify cluster config change event to: {}", asyncContext.getRequest().getRemoteAddr());
        }
        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
        asyncContext.complete();
    }

    public void registryWatcher(ConfigWatcher<?> watcher) {
        String namespace = watcher.getNamespace();
        String dataId = watcher.getDataId();
        WATCHERS.computeIfAbsent(namespace, ns -> new ConcurrentHashMap<>())
                .computeIfAbsent(dataId, did -> new ConcurrentLinkedQueue<>()).add(watcher);
    }
}
