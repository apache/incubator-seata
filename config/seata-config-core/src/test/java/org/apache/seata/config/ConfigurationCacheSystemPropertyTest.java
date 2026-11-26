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

import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConfigurationCacheSystemPropertyTest {
    private final ConfigurationCache cache = ConfigurationCache.getInstance();

    @Test
    void ignoresCachedConfigurationWhenReadingSystemProperties() throws Exception {
        String key = "seata.test.config.cache.system.lookup";
        String previous = System.getProperty(key);
        try {
            System.clearProperty(key);
            Configuration configuration = cache.proxy(new InMemoryConfiguration("configured"));

            assertEquals("configured", configuration.getConfig(key));
            assertNull(configuration.getConfigFromSys(key));

            System.setProperty(key, "overridden");
            assertEquals("overridden", configuration.getConfigFromSys(key));
        } finally {
            restoreSystemProperty(key, previous);
            cache.onChangeEvent(new ConfigurationChangeEvent(key, null));
        }
    }

    @Test
    void doesNotCacheSystemPropertyOnlyLookups() throws Exception {
        String key = "seata.test.config.cache.system.only";
        String previous = System.getProperty(key);
        try {
            Configuration configuration = cache.proxy(new InMemoryConfiguration(null));
            System.setProperty(key, "first");
            assertEquals("first", configuration.getConfigFromSys(key));

            System.setProperty(key, "second");
            assertEquals("second", configuration.getConfigFromSys(key));
        } finally {
            restoreSystemProperty(key, previous);
            cache.onChangeEvent(new ConfigurationChangeEvent(key, null));
        }
    }

    @Test
    void firstNormalReadDoesNotReuseAnEarlierSystemOnlyRead() throws Exception {
        String key = "seata.test.config.cache.system.then.normal";
        String previous = System.getProperty(key);
        try {
            Configuration configuration = cache.proxy(new InMemoryConfiguration("configured"));
            System.setProperty(key, "first");
            assertEquals("first", configuration.getConfigFromSys(key));

            System.setProperty(key, "second");
            assertEquals("second", configuration.getConfig(key));
        } finally {
            restoreSystemProperty(key, previous);
            cache.onChangeEvent(new ConfigurationChangeEvent(key, null));
        }
    }

    @Test
    void stillCachesNormalConfigurationReads() throws Exception {
        String key = "seata.test.config.cache.system.normal";
        String previous = System.getProperty(key);
        try {
            System.clearProperty(key);
            InMemoryConfiguration provider = new InMemoryConfiguration("configured");
            Configuration configuration = cache.proxy(provider);

            assertEquals("configured", configuration.getConfig(key));
            assertEquals("configured", configuration.getConfig(key));
            assertEquals(1, provider.readCount);
        } finally {
            restoreSystemProperty(key, previous);
            cache.onChangeEvent(new ConfigurationChangeEvent(key, null));
        }
    }

    private static void restoreSystemProperty(String key, String previous) {
        if (previous == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, previous);
        }
    }

    private static final class InMemoryConfiguration extends AbstractConfiguration {
        private final String value;
        private int readCount;

        private InMemoryConfiguration(String value) {
            this.value = value;
        }

        @Override
        public String getLatestConfig(String dataId, String defaultValue, long timeoutMills) {
            readCount++;
            return value == null ? defaultValue : value;
        }

        @Override
        public boolean putConfig(String dataId, String content, long timeoutMills) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean putConfigIfAbsent(String dataId, String content, long timeoutMills) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeConfig(String dataId, long timeoutMills) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addConfigListener(String dataId, ConfigurationChangeListener listener) {}

        @Override
        public void removeConfigListener(String dataId, ConfigurationChangeListener listener) {}

        @Override
        public Set<ConfigurationChangeListener> getConfigListeners(String dataId) {
            return Collections.emptySet();
        }

        @Override
        public String getTypeName() {
            return "in-memory";
        }
    }
}
