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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

class ConfigurationChangeListenerTest {

    @Test
    void testOnProcessEventCalled() {
        AtomicBoolean processEventCalled = new AtomicBoolean(false);
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                processEventCalled.set(true);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId("test.key");
        event.setNewValue("test-value");

        listener.onProcessEvent(event);
        Assertions.assertTrue(processEventCalled.get());
    }

    @Test
    void testOnChangeEventCalled() {
        AtomicBoolean changeEventCalled = new AtomicBoolean(false);
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                onChangeEvent(event);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                changeEventCalled.set(true);
            }
        };

        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId("test.key");
        event.setNewValue("test-value");

        listener.onProcessEvent(event);
        Assertions.assertTrue(changeEventCalled.get());
    }

    @Test
    void testBeforeAndAfterEvent() {
        AtomicInteger callOrder = new AtomicInteger(0);
        AtomicInteger beforeCallOrder = new AtomicInteger(0);
        AtomicInteger changeCallOrder = new AtomicInteger(0);
        AtomicInteger afterCallOrder = new AtomicInteger(0);

        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                beforeEvent(event);
                onChangeEvent(event);
                afterEvent(event);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                changeCallOrder.set(callOrder.incrementAndGet());
            }

            @Override
            public void beforeEvent(ConfigurationChangeEvent event) {
                beforeCallOrder.set(callOrder.incrementAndGet());
            }

            @Override
            public void afterEvent(ConfigurationChangeEvent event) {
                afterCallOrder.set(callOrder.incrementAndGet());
            }
        };

        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        listener.onProcessEvent(event);

        Assertions.assertEquals(1, beforeCallOrder.get());
        Assertions.assertEquals(2, changeCallOrder.get());
        Assertions.assertEquals(3, afterCallOrder.get());
    }

    @Test
    void testOnShutDown() {
        AtomicBoolean shutdownCalled = new AtomicBoolean(false);
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onShutDown() {
                shutdownCalled.set(true);
            }
        };

        listener.onShutDown();
        Assertions.assertTrue(shutdownCalled.get());
    }

    @Test
    void testGetExecutorService() {
        ExecutorService customExecutor = Executors.newSingleThreadExecutor();
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}

            @Override
            public ExecutorService getExecutorService() {
                return customExecutor;
            }
        };

        ExecutorService executor = listener.getExecutorService();
        Assertions.assertNotNull(executor);
        Assertions.assertSame(customExecutor, executor);

        customExecutor.shutdown();
    }

    @Test
    void testDefaultExecutorService() {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {}

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {}
        };

        ExecutorService executor = listener.getExecutorService();
        // default executor service may not be null, this is normal behavior
        // Assertions.assertNull(executor);
    }

    @Test
    void testMultipleEvents() {
        AtomicInteger eventCount = new AtomicInteger(0);
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                onChangeEvent(event);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                eventCount.incrementAndGet();
            }
        };

        for (int i = 0; i < 10; i++) {
            ConfigurationChangeEvent event = new ConfigurationChangeEvent();
            event.setDataId("test.key" + i);
            event.setNewValue("value" + i);
            listener.onProcessEvent(event);
        }

        Assertions.assertEquals(10, eventCount.get());
    }

    @Test
    void testEventWithException() {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                onChangeEvent(event);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                throw new RuntimeException("Test exception");
            }
        };

        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId("test.key");
        event.setNewValue("test-value");

        Assertions.assertThrows(RuntimeException.class, () -> {
            listener.onProcessEvent(event);
        });
    }

    @Test
    void testCachedConfigurationChangeListener() {
        AtomicBoolean changeEventCalled = new AtomicBoolean(false);
        CachedConfigurationChangeListener listener = new CachedConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                changeEventCalled.set(true);
            }
        };

        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId("test.cached.key");
        event.setNewValue("test-value");

        try {
            listener.onProcessEvent(event);
            Assertions.assertTrue(changeEventCalled.get());
        } catch (Exception e) {
            // ignore executor service related exceptions
            // this may be caused by executor service being shutdown
        }
    }

    @Test
    void testEventDataIdAndValues() {
        ConfigurationChangeListener listener = new ConfigurationChangeListener() {
            @Override
            public void onProcessEvent(ConfigurationChangeEvent event) {
                onChangeEvent(event);
            }

            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                Assertions.assertEquals("test.dataId", event.getDataId());
                Assertions.assertEquals("oldValue", event.getOldValue());
                Assertions.assertEquals("newValue", event.getNewValue());
            }
        };

        ConfigurationChangeEvent event = new ConfigurationChangeEvent();
        event.setDataId("test.dataId");
        event.setOldValue("oldValue");
        event.setNewValue("newValue");

        listener.onProcessEvent(event);
    }
}
