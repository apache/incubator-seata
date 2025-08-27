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

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.Listener;
import com.alibaba.nacos.api.exception.NacosException;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.Dispose;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.UndeclaredThrowableException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class NacosMockTest {
    private static ConfigService configService;

    /**
     * 存储配置数据的HashMap
     */
    private static final Map<String, String> configMap = new ConcurrentHashMap<>();
    /**
     * 存储监听器的集合
     */
    private static final Map<String, List<Listener>> listenerMap = new ConcurrentHashMap<>();

    private static final String NACOS_ENDPOINT = "127.0.0.1:8848";

    private static final String NACOS_GROUP = "SEATA_GROUP";

    private static final String NACOS_DATAID = "seata-mock";
    private static final String SUB_NACOS_DATAID = "KEY";

    private ConfigurationChangeListener listener;

    private static MockedStatic<NacosFactory> mockedNacosFactory;

    @BeforeAll
    public static void setup() throws NacosException {
        System.setProperty("seataEnv", "mock");
        // 创建Mock对象
        configService = Mockito.mock(ConfigService.class);

        // 创建NacosFactory的静态Mock
        mockedNacosFactory = Mockito.mockStatic(NacosFactory.class);

        // 配置NacosFactory.createConfigService返回我们的Mock对象
        mockedNacosFactory
                .when(() -> NacosFactory.createConfigService(any(Properties.class)))
                .thenReturn(configService);

        // 设置getConfig从HashMap获取数据
        when(configService.getConfig(anyString(), anyString(), anyLong())).thenAnswer(invocation -> {
            String dataId = invocation.getArgument(0);
            String group = invocation.getArgument(1);
            String key = dataId + "_" + group;
            return configMap.get(key);
        });

        // 设置publishConfig将数据存入HashMap
        when(configService.publishConfig(anyString(), anyString(), anyString())).thenAnswer(invocation -> {
            String dataId = invocation.getArgument(0);
            String group = invocation.getArgument(1);
            String content = invocation.getArgument(2);
            String key = dataId + "_" + group;
            configMap.put(key, content);

            // 通知监听器
            String listenerKey = key;
            if (listenerMap.containsKey(listenerKey)) {
                for (Listener listener : listenerMap.get(listenerKey)) {
                    listener.receiveConfigInfo(content);
                }
            }

            return true;
        });

        // 设置removeConfig从HashMap移除数据
        when(configService.removeConfig(anyString(), anyString())).thenAnswer(invocation -> {
            String dataId = invocation.getArgument(0);
            String group = invocation.getArgument(1);
            String key = dataId + "_" + group;
            configMap.remove(key);
            return true;
        });

        // 设置addListener添加监听器
        doAnswer(invocation -> {
                    String dataId = invocation.getArgument(0);
                    String group = invocation.getArgument(1);
                    Listener listener = invocation.getArgument(2);
                    String key = dataId + "_" + group;
                    if (listener instanceof NacosConfiguration.NacosListener) {
                        NacosConfiguration.NacosListener nacosListener = (NacosConfiguration.NacosListener) listener;
                        nacosListener.fillContext(dataId, group);
                    }

                    listenerMap.computeIfAbsent(key, k -> new ArrayList<>()).add(listener);
                    return null;
                })
                .when(configService)
                .addListener(anyString(), anyString(), any(Listener.class));

        // 设置removeListener移除监听器
        doAnswer(invocation -> {
                    String dataId = invocation.getArgument(0);
                    String group = invocation.getArgument(1);
                    Listener listener = invocation.getArgument(2);
                    String key = dataId + "_" + group;

                    if (listenerMap.containsKey(key)) {
                        listenerMap.get(key).remove(listener);
                    }
                    return null;
                })
                .when(configService)
                .removeListener(anyString(), anyString(), any(Listener.class));

        // 重新初始化配置
        NacosConfiguration configuration = NacosConfiguration.getInstance();
        if (configuration instanceof Dispose) {
            ((Dispose) configuration).dispose();
        }
        ConfigurationFactory.reload();
    }

    @Test
    @Order(1)
    public void getInstance() {
        Assertions.assertNotNull(configService);
        Assertions.assertNotNull(NacosConfiguration.getInstance());
        Assertions.assertNotNull(ConfigurationFactory.getInstance());
    }

    @Test
    @Order(2)
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
        configShortValue = configuration.getShort(SUB_NACOS_DATAID, (short) 64);
        Assertions.assertEquals(64, configShortValue);
        configShortValue = configuration.getShort(SUB_NACOS_DATAID, (short) 127, 1000);
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
    @Order(3)
    public void putConfigIfAbsent() {
        Configuration configuration = ConfigurationFactory.getInstance();
        Assertions.assertThrows(UndeclaredThrowableException.class, () -> {
            configuration.putConfigIfAbsent(NACOS_DATAID, "TEST");
        });
    }

    @Test
    @Order(4)
    public void removeConfig() {
        Configuration configuration = ConfigurationFactory.getInstance();
        boolean removed = configuration.removeConfig(NACOS_DATAID);
        Assertions.assertTrue(removed);
    }

    @Test
    @Order(5)
    public void putConfig() {
        Configuration configuration = ConfigurationFactory.getInstance();
        boolean added = configuration.putConfig(SUB_NACOS_DATAID, "TEST");
        Assertions.assertTrue(added);
        boolean removed = configuration.removeConfig(SUB_NACOS_DATAID);
        Assertions.assertTrue(removed);
    }

    @Test
    @Order(6)
    public void testConfigListener() throws NacosException, InterruptedException {
        Configuration configuration = ConfigurationFactory.getInstance();
        configuration.putConfig(NACOS_DATAID, "KEY=TEST");
        // prevent the listener event from batch processing
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
        // configcache listener + user listener
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

    @AfterAll
    public static void tearDown() {
        // 关闭MockedStatic
        if (mockedNacosFactory != null) {
            mockedNacosFactory.close();
        }

        // 清理数据
        configMap.clear();
        listenerMap.clear();
        System.clearProperty("seataEnv");
    }
}
