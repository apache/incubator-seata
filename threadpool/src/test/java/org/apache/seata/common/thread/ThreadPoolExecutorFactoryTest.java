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

import org.apache.seata.common.DefaultValues;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ThreadPoolExecutorFactory}.
 */
public class ThreadPoolExecutorFactoryTest {

    @AfterEach
    public void tearDown() {
        ThreadPoolRuntimeEnvironment.reset();
    }

    @Test
    public void testNewThreadFactoryCreatesThreadsWithExpectedName() {
        ThreadFactory threadFactory = ThreadPoolExecutorFactory.newThreadFactory("factoryTest", 2, true);

        Thread thread = threadFactory.newThread(() -> {});

        assertThat(thread.getName()).startsWith("factoryTest");
        assertThat(thread.isDaemon()).isTrue();
    }

    @Test
    public void testNewThreadPoolExecutor() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "platform");
        ThreadPoolExecutor executor = ThreadPoolExecutorFactory.newThreadPoolExecutor(
                "poolTest", 1, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        try {
            Thread thread = executor.getThreadFactory().newThread(() -> {});

            assertThat(executor).isInstanceOf(PlatformThreadPoolExecutor.class);
            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
            assertThat(thread.getName()).startsWith("poolTest");
            assertThat(thread.isDaemon()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testNewScheduledThreadPoolExecutor() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "platform");
        ScheduledThreadPoolExecutor executor =
                ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("scheduleTest", 1, true);
        try {
            Thread thread = executor.getThreadFactory().newThread(() -> {});

            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(thread.getName()).startsWith("scheduleTest");
            assertThat(thread.isDaemon()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testNewThreadPoolExecutorAllowsZeroCorePoolSize() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "platform");
        ThreadPoolExecutor executor = ThreadPoolExecutorFactory.newThreadPoolExecutor(
                "zeroCorePool", 0, 1, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        try {
            assertThat(executor.getCorePoolSize()).isZero();
            assertThat(executor.getMaximumPoolSize()).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testNewScheduledThreadPoolExecutorAllowsZeroCorePoolSize() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "platform");
        ScheduledThreadPoolExecutor executor =
                ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("zeroSchedule", 0, true);
        try {
            assertThat(executor.getCorePoolSize()).isZero();
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testVirtualThreadPoolFallsBackToPlatformWithoutLoomProvider() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "virtual");
        ThreadPoolRuntimeEnvironment.setJdkFeatureSupplier(() -> 21);

        ThreadPoolExecutor executor = ThreadPoolExecutorFactory.newThreadPoolExecutor(
                "virtualFallback", 1, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        try {
            assertThat(executor).isInstanceOf(PlatformThreadPoolExecutor.class);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testAutoThreadPoolFallsBackToPlatformBeforeLoomAvailable() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "auto");
        ThreadPoolRuntimeEnvironment.setJdkFeatureSupplier(() -> 25);

        ThreadPoolExecutor executor = ThreadPoolExecutorFactory.newThreadPoolExecutor(
                "autoFallback", 1, 2, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        try {
            assertThat(executor).isInstanceOf(PlatformThreadPoolExecutor.class);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testVirtualScheduledThreadPoolFallsBackToPlatformWithoutLoomProvider() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "virtual");
        ThreadPoolRuntimeEnvironment.setJdkFeatureSupplier(() -> 21);

        ScheduledThreadPoolExecutor executor =
                ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("virtualScheduleFallback", 1, true);
        try {
            Thread thread = executor.getThreadFactory().newThread(() -> {});

            assertThat(executor.getCorePoolSize()).isEqualTo(1);
            assertThat(thread.getName()).startsWith("virtualScheduleFallback");
            assertThat(thread.isDaemon()).isTrue();
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @CsvSource({"1.8, 8", "1.7, 7", "9, 9", "11, 11", "17, 17", "21, 21", "25, 25"})
    public void testJavaFeatureVersionParsing(String specVersion, int expectedFeature) {
        String previousVersion = System.getProperty("java.specification.version");
        try {
            System.setProperty("java.specification.version", specVersion);
            assertThat(ThreadPoolRuntimeEnvironment.javaFeatureVersion()).isEqualTo(expectedFeature);
        } finally {
            if (previousVersion != null) {
                System.setProperty("java.specification.version", previousVersion);
            } else {
                System.clearProperty("java.specification.version");
            }
        }
    }

    @Test
    public void testThreadPoolTypeAutoCodeIsStable() {
        assertThat(ThreadPoolType.AUTO.getCode()).isEqualTo("auto");
        assertThat(ThreadPoolType.from(DefaultValues.DEFAULT_TRANSPORT_THREADPOOL))
                .isEqualTo(ThreadPoolType.AUTO);
    }
}
