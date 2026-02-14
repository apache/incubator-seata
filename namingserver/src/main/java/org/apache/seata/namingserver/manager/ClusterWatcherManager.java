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
package org.apache.seata.namingserver.manager;

import jakarta.annotation.PostConstruct;
import jakarta.servlet.AsyncContext;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.seata.namingserver.listener.ClusterChangeEvent;
import org.apache.seata.namingserver.listener.ClusterChangeListener;
import org.apache.seata.namingserver.listener.ClusterChangePushEvent;
import org.apache.seata.namingserver.listener.Watcher;
import org.apache.seata.namingserver.metrics.NamingServerMetricsManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Component
public class ClusterWatcherManager implements ClusterChangeListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private static final Map<String /* vgroup */, Queue<Watcher<?>>> WATCHERS = new ConcurrentHashMap<>();

    private static final Map<String /* vgroup */, Long> GROUP_UPDATE_TERM = new ConcurrentHashMap<>();

    private final ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
            new ScheduledThreadPoolExecutor(1, new CustomizableThreadFactory("long-polling"));

    @Autowired
    private NamingServerMetricsManager metricsManager;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @PostConstruct
    public void init() {
        // Register metrics data supplier
        metricsManager.setWatchersSupplier(() -> WATCHERS);

        // Responds to monitors that time out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(
                () -> {
                    for (String group : WATCHERS.keySet()) {
                        Optional.ofNullable(WATCHERS.remove(group))
                                .ifPresent(watchers -> watchers.parallelStream().forEach(watcher -> {
                                    if (System.currentTimeMillis() >= watcher.getTimeout()) {
                                        notify(watcher, HttpStatus.NOT_MODIFIED.value());
                                    }
                                    if (!watcher.isDone()) {
                                        // Re-register
                                        registryWatcher(watcher);
                                    }
                                }));
                    }
                },
                1,
                1,
                TimeUnit.SECONDS);
    }

    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        if (event.getTerm() > 0 || event.getTerm() == -1) {
            GROUP_UPDATE_TERM.put(event.getGroup(), event.getTerm());
            // Notifications are made of changes in cluster information

            Optional.ofNullable(WATCHERS.remove(event.getGroup())).ifPresent(watchers -> {
                watchers.parallelStream().forEach(this::notify);
                // Publish event for metrics tracking
                if (!watchers.isEmpty()) {
                    eventPublisher.publishEvent(new ClusterChangePushEvent(
                            this, event.getNamespace(), event.getClusterName(), event.getGroup()));
                    // Refresh watcher count metrics after notification
                    metricsManager.refreshWatcherCountMetrics();
                }
            });
        }
    }

    private void notify(Watcher<?> watcher) {
        notify(watcher, HttpServletResponse.SC_OK);
    }

    private void notify(Watcher<?> watcher, int statusCode) {
        AsyncContext asyncContext = (AsyncContext) watcher.getAsyncContext();
        HttpServletResponse httpServletResponse = (HttpServletResponse) asyncContext.getResponse();
        watcher.setDone(true);
        if (logger.isDebugEnabled()) {
            logger.debug(
                    "notify cluster change event to: {}",
                    asyncContext.getRequest().getRemoteAddr());
        }
        httpServletResponse.setStatus(statusCode);
        asyncContext.complete();
    }

    public void registryWatcher(Watcher<?> watcher) {
        String group = watcher.getGroup();
        Long term = GROUP_UPDATE_TERM.get(group);
        if (term == null || watcher.getTerm() >= term) {
            WATCHERS.computeIfAbsent(group, value -> new ConcurrentLinkedQueue<>())
                    .add(watcher);

            // Immediately refresh watcher count metrics
            metricsManager.refreshWatcherCountMetrics();
        } else {
            notify(watcher);
        }
    }

    public List<String> getWatcherIpList(String vGroup) {
        Set<String> watcherIpSet = new HashSet<>();
        Queue<Watcher<?>> watcherQueue = WATCHERS.get(vGroup);
        for (Watcher<?> watcher : watcherQueue) {
            watcherIpSet.add(watcher.getClientEndpoint());
        }
        return new ArrayList<>(watcherIpSet);
    }

    public List<String> getWatchVGroupList() {
        return new ArrayList<>(WATCHERS.keySet());
    }

    public long getTermByvGroup(String vGroup) {
        return GROUP_UPDATE_TERM.getOrDefault(vGroup, 0L);
    }
}
