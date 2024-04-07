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
package org.apache.seata.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.seata.common.thread.NamedThreadFactory;

/**
 * The interface Configuration change listener.
 *
 */
public interface ConfigurationChangeListener {

    /**
     * The constant CORE_LISTENER_THREAD.
     */
    int CORE_LISTENER_THREAD = 1;
    /**
     * The constant MAX_LISTENER_THREAD.
     */
    int MAX_LISTENER_THREAD = 1;
    /**
     * The constant EXECUTOR_SERVICE.
     */
    ExecutorService EXECUTOR_SERVICE =
        new ThreadPoolExecutor(CORE_LISTENER_THREAD, MAX_LISTENER_THREAD, Integer.MAX_VALUE, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(), new NamedThreadFactory("configListenerOperate", MAX_LISTENER_THREAD));

    /**
     * Process.
     *
     * @param event the event
     */
    void onChangeEvent(ConfigurationChangeEvent event);

    /**
     * On process event.
     *
     * @param event the event
     */
    default void onProcessEvent(ConfigurationChangeEvent event) {
        getExecutorService().submit(() -> {
            beforeEvent(event);
            onChangeEvent(event);
            afterEvent(event);
        });
    }

    /**
     * On shut down.
     */
    default void onShutDown() {
        getExecutorService().shutdownNow();
    }

    /**
     * Gets executor service.
     *
     * @return the executor service
     */
    default ExecutorService getExecutorService() {
        return EXECUTOR_SERVICE;
    }

    /**
     * Before event.
     */
    default void beforeEvent(ConfigurationChangeEvent event) {

    }

    /**
     * After event.
     */
    default void afterEvent(ConfigurationChangeEvent event) {

    }
}
