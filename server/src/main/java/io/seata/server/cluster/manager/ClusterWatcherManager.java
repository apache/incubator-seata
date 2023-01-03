/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.server.cluster.manager;

import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.PostConstruct;
import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletResponse;
import io.seata.common.thread.NamedThreadFactory;
import io.seata.server.cluster.listener.ClusterChangeEvent;
import io.seata.server.cluster.listener.ClusterChangeListener;
import io.seata.server.cluster.watch.Watcher;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import static io.seata.common.DefaultValues.DEFAULT_SEATA_GROUP;

/**
 * @author jianbin.chen
 */
@Component
public class ClusterWatcherManager implements ClusterChangeListener {

    private static final Map<String, Queue<Watcher<?>>> WATCHERS = new ConcurrentHashMap<>();

    private ScheduledThreadPoolExecutor scheduledThreadPoolExecutor =
        new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("long-polling", 1));

    @PostConstruct
    public void init() {
        // Responds to monitors that time out
        scheduledThreadPoolExecutor.scheduleAtFixedRate(() -> {
            Queue<Watcher<?>> watchers = WATCHERS.remove(DEFAULT_SEATA_GROUP);
            watchers.parallelStream().forEach(watcher -> {
                if (!watcher.isDone()
                    && (System.currentTimeMillis() - watcher.getCreateTime() > watcher.getTimeout())) {
                    HttpServletResponse httpServletResponse = (HttpServletResponse)watcher.getAsyncContext();
                    synchronized (httpServletResponse) {
                        if (!watcher.isDone()
                            && (System.currentTimeMillis() - watcher.getCreateTime() > watcher.getTimeout())) {
                            watcher.setDone(true);
                            httpServletResponse.setStatus(HttpServletResponse.SC_NOT_MODIFIED);
                            ((AsyncContext)watcher.getAsyncContext()).complete();
                        }
                    }
                }
                if (!watcher.isDone()) {
                    // Re-register
                    registryWatcher(watcher);
                }
            });
        }, 1, 1, TimeUnit.SECONDS);
    }

    @Override
    @EventListener
    @Async
    public void onChangeEvent(ClusterChangeEvent event) {
        // Notifications are made of changes in cluster information
        Queue<Watcher<?>> watchers = WATCHERS.remove(DEFAULT_SEATA_GROUP);
        watchers.parallelStream().forEach(watcher -> {
            if (!watcher.isDone() && (System.currentTimeMillis() - watcher.getCreateTime() < watcher.getTimeout())) {
                HttpServletResponse httpServletResponse = (HttpServletResponse)watcher.getAsyncContext();
                synchronized (httpServletResponse) {
                    if (!watcher.isDone()
                        && (System.currentTimeMillis() - watcher.getCreateTime() < watcher.getTimeout())) {
                        watcher.setDone(true);
                        httpServletResponse.setStatus(HttpServletResponse.SC_OK);
                        ((AsyncContext)watcher.getAsyncContext()).complete();
                    }
                }
            }
        });
    }

    public void registryWatcher(Watcher<?> watcher) {
        WATCHERS.computeIfAbsent(DEFAULT_SEATA_GROUP, value -> new ConcurrentLinkedQueue<>()).add(watcher);
    }

}
