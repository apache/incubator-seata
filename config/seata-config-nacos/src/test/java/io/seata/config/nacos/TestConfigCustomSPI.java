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

import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.alibaba.nacos.api.NacosFactory;
import com.alibaba.nacos.api.config.ConfigService;
import com.alibaba.nacos.api.exception.NacosException;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TestConfigCustomSPI {

    private static final Config FILE_CONFIG = ConfigFactory.load("registry-test.conf");
    private static ConfigService configService;

    @BeforeAll
    public static void setup() throws NacosException {
        String serverAddr = FILE_CONFIG.getString("config.test.serverAddr");
        Properties properties = new Properties();
        properties.put("serverAddr", serverAddr);
        configService = NacosFactory.createConfigService(properties);
        System.setProperty("seataEnv", "test");
    }

    @Test
    public void testGetConfigProperties() throws Exception {
        Assertions.assertNotNull(configService);
        Configuration configuration = ConfigurationFactory.getInstance();
        String dataId = "nacos.config.custom.spi.test";
        String group = FILE_CONFIG.getString("config.test.group");
        String content = "seata";
        CountDownLatch listenerCountDown = new CountDownLatch(1);
        configuration.addConfigListener(dataId, new CachedConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                Assertions.assertEquals(content, event.getNewValue());
                listenerCountDown.countDown();
            }
        });
        configService.publishConfig(dataId, group, content);
        boolean reachZero = listenerCountDown.await(5, TimeUnit.SECONDS);
        Assertions.assertFalse(reachZero);
        //get config
        String config = configuration.getConfig(dataId);
        Assertions.assertEquals(content, config);
        //listener
        Set<ConfigurationChangeListener> listeners = configuration.getConfigListeners(dataId);
        Assertions.assertEquals(2, listeners.size());

    }

    @AfterAll
    public static void afterAll() {
    }
}
