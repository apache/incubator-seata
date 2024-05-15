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
package io.seata.config.nacos;

import java.security.SecureRandom;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.config.listener.AbstractSharedListener;
import com.alibaba.nacos.api.exception.NacosException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestConfigCustomSPI {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestConfigCustomSPI.class);

    private static  Config FILE_CONFIG;
    private static ConfigService configService;

    private static final String CHARACTERS = "abcdefghijklmnopqrstuvwxyz";

    private static final int STRING_LENGTH = 6;

    private static final SecureRandom random = new SecureRandom();

    @BeforeAll
    public static void setup() throws NacosException {
        System.setProperty("seataEnv", "test");
        ConfigurationFactory.reload();
        ConfigurationCache.clear();
        FILE_CONFIG = ConfigFactory.load("registry-test.conf");
        configService = NacosFactory.createConfigService(NacosConfiguration.getConfigProperties());
    }

    @Test
    public void testGetConfigProperties() throws Exception {
        Assertions.assertNotNull(configService);
        Configuration configuration = ConfigurationFactory.getInstance();
        String postfix = generateRandomString();
        String dataId = "nacos.config.custom.spi." + postfix;
        System.setProperty("nacos.config.test.dataId", dataId);
        String group = FILE_CONFIG.getString("config.test.group");
        String content = "seata";
        CountDownLatch listenerCountDown = new CountDownLatch(1);
        configuration.addConfigListener(dataId, new CachedConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                LOGGER.info("onChangeEvent: {}", event.getNewValue());
                System.out.println("onChangeEvent: " + event.getNewValue());
                Assertions.assertEquals(content, event.getNewValue());
                listenerCountDown.countDown();
            }
        });
        CountDownLatch listenerCountDown2 = new CountDownLatch(1);
        LOGGER.info("add dataid: {},group: {}", dataId,group);
        configService.addListener(dataId, group, new AbstractSharedListener() {
            @Override public Executor getExecutor() {
                return Executors.newFixedThreadPool(1);
            }

            @Override
            public void innerReceive(String dataId, String group, String configInfo) {
                listenerCountDown2.countDown();
            }
        });
        configService.publishConfig(dataId, group, content);
        String currentContent = configService.getConfig(dataId, group, 5000);
        Assertions.assertEquals(content, currentContent);
        Assertions.assertTrue(listenerCountDown2.await(10, TimeUnit.SECONDS));
        boolean reachZero = listenerCountDown.await(10, TimeUnit.SECONDS);
        Assertions.assertTrue(reachZero);
        //get config
        String config = configuration.getConfig(dataId);
        Assertions.assertEquals(content, config);
        //listener
        Set<ConfigurationChangeListener> listeners = configuration.getConfigListeners(dataId);
        Assertions.assertEquals(2, listeners.size());

    }

    public static String generateRandomString() {
        StringBuilder sb = new StringBuilder(STRING_LENGTH);
        for (int i = 0; i < STRING_LENGTH; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    @AfterAll
    public static void afterAll() {
    }
}
