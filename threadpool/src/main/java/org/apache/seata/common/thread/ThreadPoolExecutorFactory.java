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
package org.apache.seata.common.thread;

import org.apache.seata.common.loader.EnhancedServiceLoader;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Central factory used by Seata managed thread pools.
 */
public final class ThreadPoolExecutorFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(ThreadPoolExecutorFactory.class);

    private ThreadPoolExecutorFactory() {}

    public static ThreadFactory newThreadFactory(String threadPrefix, int totalSize) {
        return newThreadFactory(threadPrefix, totalSize, true);
    }

    public static ThreadFactory newThreadFactory(String threadPrefix, int totalSize, boolean daemon) {
        Objects.requireNonNull(threadPrefix, "threadPrefix must not be null");
        return new NamedThreadFactory(threadPrefix, totalSize, daemon);
    }

    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue) {
        return newThreadPoolExecutor(threadPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, true);
    }

    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            boolean daemon) {
        return newThreadPoolExecutor(
                threadPrefix,
                corePoolSize,
                maximumPoolSize,
                keepAliveTime,
                unit,
                workQueue,
                daemon,
                new ThreadPoolExecutor.AbortPolicy());
    }

    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            RejectedExecutionHandler rejectedHandler) {
        return newThreadPoolExecutor(
                threadPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, true, rejectedHandler);
    }

    public static ThreadPoolExecutor newThreadPoolExecutor(
            String threadPrefix,
            int corePoolSize,
            int maximumPoolSize,
            long keepAliveTime,
            TimeUnit unit,
            BlockingQueue<Runnable> workQueue,
            boolean daemon,
            RejectedExecutionHandler rejectedHandler) {
        validateThreadPoolArguments(threadPrefix, corePoolSize, maximumPoolSize, keepAliveTime, unit);
        return resolveThreadPoolProvider()
                .newThreadPoolExecutor(
                        threadPrefix,
                        corePoolSize,
                        maximumPoolSize,
                        keepAliveTime,
                        unit,
                        Objects.requireNonNull(workQueue, "workQueue must not be null"),
                        daemon,
                        Objects.requireNonNull(rejectedHandler, "rejectedHandler must not be null"));
    }

    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(String threadPrefix, int corePoolSize) {
        return newScheduledThreadPoolExecutor(threadPrefix, corePoolSize, true);
    }

    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(
            String threadPrefix, int corePoolSize, boolean daemon) {
        return newScheduledThreadPoolExecutor(threadPrefix, corePoolSize, daemon, new ThreadPoolExecutor.AbortPolicy());
    }

    public static ScheduledThreadPoolExecutor newScheduledThreadPoolExecutor(
            String threadPrefix, int corePoolSize, boolean daemon, RejectedExecutionHandler rejectedHandler) {
        validateScheduledThreadPoolArguments(threadPrefix, corePoolSize);
        return resolveThreadPoolProvider()
                .newScheduledThreadPoolExecutor(
                        threadPrefix,
                        corePoolSize,
                        daemon,
                        Objects.requireNonNull(rejectedHandler, "rejectedHandler must not be null"));
    }

    private static void validateThreadPoolArguments(
            String threadPrefix, int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) {
        Objects.requireNonNull(threadPrefix, "threadPrefix must not be null");
        Objects.requireNonNull(unit, "timeUnit must not be null");
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize must not be negative");
        }
        if (maximumPoolSize <= 0) {
            throw new IllegalArgumentException("maximumPoolSize must be greater than zero");
        }
        if (maximumPoolSize < corePoolSize) {
            throw new IllegalArgumentException("maximumPoolSize must be greater than or equal to corePoolSize");
        }
        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("keepAliveTime must not be negative");
        }
    }

    private static void validateScheduledThreadPoolArguments(String threadPrefix, int corePoolSize) {
        Objects.requireNonNull(threadPrefix, "threadPrefix must not be null");
        if (corePoolSize < 0) {
            throw new IllegalArgumentException("corePoolSize must not be negative");
        }
    }

    private static ThreadPoolProvider resolveThreadPoolProvider() {
        ThreadPoolType threadPoolType = ThreadPoolRuntimeEnvironment.resolveThreadPoolType();
        if (threadPoolType == ThreadPoolType.VIRTUAL && ThreadPoolProviderHolder.VIRTUAL_THREAD_POOL_PROVIDER != null) {
            return ThreadPoolProviderHolder.VIRTUAL_THREAD_POOL_PROVIDER;
        }
        if (threadPoolType == ThreadPoolType.VIRTUAL) {
            // loadOptional() already warned that the provider is absent at initialisation time.
            // This second warning is intentionally kept to surface the per-request fallback
            // decision so that operators can correlate the missing-provider startup message
            // with the actual thread-pool mode that is in effect.
            LOGGER.warn(
                    "Virtual thread pool was selected but the virtual-thread SPI provider (seata-threadpool-virtual) "
                            + "is not present on the classpath. Falling back to platform threads.");
        }
        return ThreadPoolProviderHolder.PLATFORM_THREAD_POOL_PROVIDER;
    }

    private static final class ThreadPoolProviderHolder {
        private static final ThreadPoolProvider PLATFORM_THREAD_POOL_PROVIDER =
                EnhancedServiceLoader.load(ThreadPoolProvider.class, ThreadPoolType.PLATFORM.getCode());
        private static final ThreadPoolProvider VIRTUAL_THREAD_POOL_PROVIDER =
                loadOptional(ThreadPoolType.VIRTUAL.getCode());

        private ThreadPoolProviderHolder() {}
    }

    private static ThreadPoolProvider loadOptional(String activateName) {
        try {
            return EnhancedServiceLoader.load(ThreadPoolProvider.class, activateName);
        } catch (EnhancedServiceNotFoundException ignored) {
            LOGGER.warn(
                    "Virtual thread pool SPI provider '{}' is not available on the classpath. "
                            + "Virtual threads cannot be used.",
                    activateName);
            return null;
        }
    }
}
