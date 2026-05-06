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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for loom-backed thread pool providers.
 */
@EnabledIfSystemProperty(named = "java.specification.version", matches = "(21|2[2-9]|[3-9][0-9])")
public class VirtualThreadPoolProviderTest {

    @AfterEach
    public void tearDown() {
        ThreadPoolRuntimeEnvironment.reset();
    }

    @Test
    public void testVirtualThreadPoolExecutorKeepsConfiguredBounds() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "virtual");
        ThreadPoolRuntimeEnvironment.setJdkFeatureSupplier(() -> 21);
        LinkedBlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<>();

        ThreadPoolExecutor executor =
                ThreadPoolExecutorFactory.newThreadPoolExecutor("virtualPool", 1, 2, 60, TimeUnit.SECONDS, workQueue);
        try {
            Thread thread = executor.getThreadFactory().newThread(() -> {});

            assertThat(executor).isInstanceOf(VirtualThreadPoolExecutor.class);
            assertThat(executor.getMaximumPoolSize()).isEqualTo(2);
            assertThat(executor.getKeepAliveTime(TimeUnit.SECONDS)).isEqualTo(60);
            assertThat(executor.getQueue()).isSameAs(workQueue);
            assertThat(thread.isVirtual()).isTrue();
            assertThat(thread.isDaemon()).isTrue();
            assertThat(thread.getName()).startsWith("virtualPool-");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testVirtualScheduledThreadPoolExecutorUsesVirtualThreads() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "virtual");
        ThreadPoolRuntimeEnvironment.setJdkFeatureSupplier(() -> 21);

        ScheduledThreadPoolExecutor executor =
                ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("virtualSchedule", 1, true);
        try {
            Thread thread = executor.getThreadFactory().newThread(() -> {});

            assertThat(executor).isInstanceOf(VirtualScheduledThreadPoolExecutor.class);
            assertThat(thread.isVirtual()).isTrue();
            assertThat(thread.isDaemon()).isTrue();
            assertThat(thread.getName()).startsWith("virtualSchedule-");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testVirtualThreadPoolsRejectNonDaemonRequests() {
        ThreadPoolRuntimeEnvironment.setThreadPoolTypeSupplier(() -> "virtual");
        ThreadPoolRuntimeEnvironment.setJdkFeatureSupplier(() -> 21);

        assertThatThrownBy(() -> ThreadPoolExecutorFactory.newThreadPoolExecutor(
                        "virtualReject", 1, 1, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(), false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("always daemon");

        assertThatThrownBy(() ->
                        ThreadPoolExecutorFactory.newScheduledThreadPoolExecutor("virtualRejectSchedule", 1, false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("always daemon");
    }
}
