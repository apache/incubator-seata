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
package org.apache.seata.config.nacos;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.Dispose;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;

public class NacosMockTest {
    private static ConfigService configService;
    private static final String NACOS_ENDPOINT = "127.0.0.1:8848";

    private static final String NACOS_GROUP = "SEATA_GROUP";

    private static final String NACOS_DATAID = "seata-mock";
    private static final String SUB_NACOS_DATAID = "KEY";

    private ConfigurationChangeListener listener;

    @BeforeAll
    public static void setup() throws NacosException {
        System.setProperty("seataEnv", "mock");
        NacosConfiguration configuration = NacosConfiguration.getInstance();
        if (configuration instanceof Dispose) {
            ((Dispose)configuration).dispose();
        }
        ConfigurationFactory.reload();
        Properties properties = new Properties();
        properties.setProperty("serverAddr", NACOS_ENDPOINT);
        configService = NacosFactory.createConfigService(properties);
        configService.removeConfig(NACOS_DATAID, NACOS_GROUP);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void getInstance() {
        Assertions.assertNotNull(configService);
        Assertions.assertNotNull(NacosConfiguration.getInstance());
        Assertions.assertNotNull(ConfigurationFactory.getInstance());
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void getConfig() {
        Configuration configuration = ConfigurationFactory.getInstance();
        String configStrValue = configuration.getConfig(SUB_NACOS_DATAID);
        Assertions.assertNull(configStrValue);
        configStrValue = configuration.getConfig(SUB_NACOS_DATAID, 1000);
        Assertions.assertNull(configStrValue);
        configStrValue = configuration.getConfig(SUB_NACOS_DATAID, "TEST", 1000);
        Assertions.assertEquals("TEST", configStrValue);
        ConfigurationCache.clear();
        System.setProperty(SUB_NACOS_DATAID, "SYS-TEST");
        configStrValue = configuration.getConfig(SUB_NACOS_DATAID, "TEST", 1000);
        Assertions.assertEquals("SYS-TEST", configStrValue);
        ConfigurationCache.clear();
        System.clearProperty(SUB_NACOS_DATAID);

        ConfigurationCache.clear();
        int configIntValue = configuration.getInt(SUB_NACOS_DATAID);
        Assertions.assertEquals(0, configIntValue);
        configIntValue = configuration.getInt(SUB_NACOS_DATAID, 100);
        Assertions.assertEquals(100, configIntValue);
        configIntValue = configuration.getInt(SUB_NACOS_DATAID, 100, 1000);
        Assertions.assertEquals(100, configIntValue);

        ConfigurationCache.clear();
        boolean configBoolValue = configuration.getBoolean(SUB_NACOS_DATAID);
        Assertions.assertEquals(false, configBoolValue);
        configBoolValue = configuration.getBoolean(SUB_NACOS_DATAID, true);
        Assertions.assertEquals(true, configBoolValue);
        configBoolValue = configuration.getBoolean(SUB_NACOS_DATAID, true, 1000);
        Assertions.assertEquals(true, configBoolValue);

        ConfigurationCache.clear();
        short configShortValue = configuration.getShort(SUB_NACOS_DATAID);
        Assertions.assertEquals(0, configShortValue);
        configShortValue = configuration.getShort(SUB_NACOS_DATAID, (short)64);
        Assertions.assertEquals(64, configShortValue);
        configShortValue = configuration.getShort(SUB_NACOS_DATAID, (short)127, 1000);
        Assertions.assertEquals(127, configShortValue);

        ConfigurationCache.clear();
        long configLongValue = configuration.getShort(SUB_NACOS_DATAID);
        Assertions.assertEquals(0L, configLongValue);
        configLongValue = configuration.getLong(SUB_NACOS_DATAID, 12345678L);
        Assertions.assertEquals(12345678L, configLongValue);
        configLongValue = configuration.getLong(SUB_NACOS_DATAID, 65535L, 1000);
        Assertions.assertEquals(65535L, configLongValue);

        ConfigurationCache.clear();
        Duration configDurValue = configuration.getDuration(SUB_NACOS_DATAID);
        Assertions.assertEquals(Duration.ZERO, configDurValue);
        Duration defaultDuration = Duration.ofMillis(1000);
        configDurValue = configuration.getDuration(SUB_NACOS_DATAID, defaultDuration);
        Assertions.assertEquals(defaultDuration, configDurValue);
        defaultDuration = Duration.ofMillis(1000);
        configDurValue = configuration.getDuration(SUB_NACOS_DATAID, defaultDuration, 1000);
        Assertions.assertEquals(defaultDuration, configDurValue);

        ConfigurationCache.clear();
        configStrValue = configuration.getLatestConfig(SUB_NACOS_DATAID, "DEFAULT", 1000);
        Assertions.assertEquals("DEFAULT", configStrValue);

    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void putConfigIfAbsent() {
        Configuration configuration = ConfigurationFactory.getInstance();
        Assertions.assertThrows(UndeclaredThrowableException.class, () -> {
            configuration.putConfigIfAbsent(NACOS_DATAID, "TEST");
        });
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void removeConfig() {
        Configuration configuration = ConfigurationFactory.getInstance();
        boolean removed = configuration.removeConfig(NACOS_DATAID);
        Assertions.assertTrue(removed);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void putConfig() {
        Configuration configuration = ConfigurationFactory.getInstance();
        boolean added = configuration.putConfig(SUB_NACOS_DATAID, "TEST");
        Assertions.assertTrue(added);
        boolean removed = configuration.removeConfig(SUB_NACOS_DATAID);
        Assertions.assertTrue(removed);
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    public void testConfigListener() throws NacosException, InterruptedException {
        Configuration configuration = ConfigurationFactory.getInstance();
        configuration.putConfig(NACOS_DATAID, "KEY=TEST");
        //prevent the listener event from batch processing
        Thread.sleep(1000);
        CountDownLatch latch = new CountDownLatch(1);
        listener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                Assertions.assertEquals(SUB_NACOS_DATAID, event.getDataId());
                latch.countDown();
            }
        };
        configuration.addConfigListener(SUB_NACOS_DATAID, listener);
        Thread.sleep(1000);
        configuration.putConfig(NACOS_DATAID, "KEY=VALUE");
        latch.await(1000, TimeUnit.MILLISECONDS);
        Set<ConfigurationChangeListener> listeners = configuration.getConfigListeners(SUB_NACOS_DATAID);
        //configcache listener + user listener
        Assertions.assertEquals(2, listeners.size());

        configuration.removeConfigListener(SUB_NACOS_DATAID, listener);
        listeners = configuration.getConfigListeners(SUB_NACOS_DATAID);
        Assertions.assertEquals(1, listeners.size());
    }

    @AfterEach
    public void afterEach() throws NacosException {
        configService.removeConfig(NACOS_DATAID, NACOS_GROUP);
        ConfigurationFactory.reload();
    }
}
