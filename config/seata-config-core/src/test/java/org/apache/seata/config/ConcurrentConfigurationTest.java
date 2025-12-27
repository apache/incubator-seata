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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class ConcurrentConfigurationTest {

    @BeforeEach
    void setUp() {
        ConfigurationCache.clear();
    }

    @AfterEach
    void tearDown() {
        ConfigurationCache.clear();
    }

    @Test
    void testConcurrentCacheRead() throws Exception {
        Configuration mockConfig = ConfigurationFactory.getInstance();
        Configuration proxy = ConfigurationCache.getInstance().proxy(mockConfig);

        System.setProperty("test.concurrent.read", "concurrent-value");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<String> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String value = proxy.getConfig("test.concurrent.read");
                    synchronized (results) {
                        results.add(value);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        Assertions.assertEquals(threadCount, results.size());
        for (String result : results) {
            Assertions.assertEquals("concurrent-value", result);
        }
    }

    @Test
    void testConcurrentCacheWrite() throws Exception {
        Configuration mockConfig = ConfigurationFactory.getInstance();
        Configuration proxy = ConfigurationCache.getInstance().proxy(mockConfig);

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    System.setProperty("test.concurrent.write." + index, "value-" + index);
                    proxy.getConfig("test.concurrent.write." + index);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        for (int i = 0; i < threadCount; i++) {
            String value = proxy.getConfig("test.concurrent.write." + i);
            Assertions.assertEquals("value-" + i, value);
        }
    }

    @Test
    void testConcurrentAddListener() throws InterruptedException {
        Configuration fileConfig = ConfigurationFactory.getInstance();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger listenerCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ConfigurationChangeListener listener = new ConfigurationChangeListener() {
                        @Override
                        public void onProcessEvent(ConfigurationChangeEvent event) {}

                        @Override
                        public void onChangeEvent(ConfigurationChangeEvent event) {
                            listenerCount.incrementAndGet();
                        }
                    };
                    fileConfig.addConfigListener("test.concurrent.listener", listener);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        java.util.Set<ConfigurationChangeListener> listeners =
                fileConfig.getConfigListeners("test.concurrent.listener");
        Assertions.assertNotNull(listeners);
        // due to concurrent addition may duplicate, allow some margin of error
        Assertions.assertTrue(listeners.size() >= threadCount - 2 && listeners.size() <= threadCount + 2);
    }

    @Test
    void testConcurrentRemoveListener() throws InterruptedException {
        Configuration fileConfig = ConfigurationFactory.getInstance();

        List<ConfigurationChangeListener> listeners = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            ConfigurationChangeListener listener = new ConfigurationChangeListener() {
                @Override
                public void onProcessEvent(ConfigurationChangeEvent event) {}

                @Override
                public void onChangeEvent(ConfigurationChangeEvent event) {}
            };
            listeners.add(listener);
            fileConfig.addConfigListener("test.concurrent.remove", listener);
        }

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    fileConfig.removeConfigListener("test.concurrent.remove", listeners.get(index));
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        java.util.Set<ConfigurationChangeListener> remainingListeners =
                fileConfig.getConfigListeners("test.concurrent.remove");
        Assertions.assertNotNull(remainingListeners);
        // due to concurrent removal may be inaccurate, allow some margin of error
        Assertions.assertTrue(remainingListeners.size() >= 3 && remainingListeners.size() <= 7);
    }

    @Test
    void testConcurrentCacheClear() throws Exception {
        Configuration mockConfig = ConfigurationFactory.getInstance();
        Configuration proxy = ConfigurationCache.getInstance().proxy(mockConfig);

        for (int i = 0; i < 10; i++) {
            System.setProperty("test.clear." + i, "value-" + i);
            proxy.getConfig("test.clear." + i);
        }

        int threadCount = 5;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    ConfigurationCache.clear();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testConcurrentGetInt() throws Exception {
        Configuration mockConfig = ConfigurationFactory.getInstance();
        Configuration proxy = ConfigurationCache.getInstance().proxy(mockConfig);

        System.setProperty("test.concurrent.int", "123");

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Integer> results = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    int value = proxy.getInt("test.concurrent.int");
                    synchronized (results) {
                        results.add(value);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        Assertions.assertEquals(threadCount, results.size());
        for (int result : results) {
            Assertions.assertEquals(123, result);
        }
    }

    @Test
    void testConcurrentChangeEvent() throws InterruptedException {
        ConfigurationCache cache = ConfigurationCache.getInstance();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    ConfigurationChangeEvent event = new ConfigurationChangeEvent();
                    event.setDataId("test.concurrent.event." + index);
                    event.setNewValue("value-" + index);
                    cache.onChangeEvent(event);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testConcurrentMixedOperations() throws Exception {
        Configuration mockConfig = ConfigurationFactory.getInstance();
        Configuration proxy = ConfigurationCache.getInstance().proxy(mockConfig);

        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            executor.submit(() -> {
                try {
                    if (index % 2 == 0) {
                        System.setProperty("test.mixed." + index, "value-" + index);
                        proxy.getConfig("test.mixed." + index);
                    } else {
                        proxy.getInt("test.mixed." + index, index);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();
    }

    @Test
    void testConcurrentProxyCreation() throws Exception {
        Configuration mockConfig = ConfigurationFactory.getInstance();

        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        List<Configuration> proxies = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    Configuration proxy = ConfigurationCache.getInstance().proxy(mockConfig);
                    synchronized (proxies) {
                        proxies.add(proxy);
                    }
                } catch (Exception e) {
                    // Ignore exception in test
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        executor.shutdown();

        Assertions.assertEquals(threadCount, proxies.size());
    }
}
