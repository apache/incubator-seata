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
package io.seata.core.event;

import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import io.seata.common.thread.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default event bus implement with Guava EventBus.
 *
 * @author zhengyangyong
 */
public class GuavaEventBus implements EventBus {
    private static final Logger LOGGER = LoggerFactory.getLogger(GuavaEventBus.class);
    private final com.google.common.eventbus.EventBus eventBus;
    private static Set<Object> subscriberSet = ConcurrentHashMap.newKeySet();

    public GuavaEventBus(String identifier) {
        this(identifier, false);
    }

    public GuavaEventBus(String identifier, boolean async) {
        if (!async) {
            this.eventBus = new com.google.common.eventbus.EventBus(identifier);
        } else {
            final ExecutorService eventExecutor = new ThreadPoolExecutor(1, 1, Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(2048), new NamedThreadFactory(identifier, 1, true), (r, executor) -> {

                LOGGER.warn("eventBus executor queue is full, size:{}", executor.getQueue().size());
            });
            this.eventBus = new com.google.common.eventbus.AsyncEventBus(identifier, eventExecutor);
        }
    }

    @Override
    public void register(Object subscriber) {
        if (subscriberSet.add(subscriber)) {
            this.eventBus.register(subscriber);
        }
    }

    @Override
    public void unregister(Object subscriber) {
        if (subscriberSet.remove(subscriber)) {
            this.eventBus.unregister(subscriber);
        }
    }


    @Override
    public void unregisterAll() {
        for (Object subscriber : subscriberSet) {
            unregister(subscriber);
        }
    }

    @Override
    public void post(Event event) {
        this.eventBus.post(event);
    }

    @Override
    public Set<Object> getSubscribers() {
        return subscriberSet;
    }
}
