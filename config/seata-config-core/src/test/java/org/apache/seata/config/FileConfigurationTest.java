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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class FileConfigurationTest {

    Logger logger = LoggerFactory.getLogger(FileConfigurationTest.class);

    @BeforeAll
   static void setUp() {
        System.setProperty("file.listener.enabled", "true");
        ConfigurationCache.clear();
    }

    @AfterAll
    static void tearDown() {
        ConfigurationCache.clear();
        System.setProperty("file.listener.enabled", "true");
    }

    @Test
    void addConfigListener() throws InterruptedException {
        logger.info("addConfigListener");
        ConfigurationFactory.reload();
        Configuration fileConfig = ConfigurationFactory.getInstance();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        String dataId = "service.disableGlobalTransaction";
        boolean value = fileConfig.getBoolean(dataId);
        fileConfig.addConfigListener(dataId, (CachedConfigurationChangeListener)event -> {
            logger.info("before dataId: {}, oldValue: {}, newValue: {}", event.getDataId(), event.getOldValue(),
                event.getNewValue());
            Assertions.assertEquals(Boolean.parseBoolean(event.getNewValue()),
                !Boolean.parseBoolean(event.getOldValue()));
            logger.info("after dataId: {}, oldValue: {}, newValue: {}", event.getDataId(), event.getOldValue(),
                event.getNewValue());
            countDownLatch.countDown();
        });
        System.setProperty(dataId, String.valueOf(!value));
        logger.info(System.currentTimeMillis()+", dataId: {}, oldValue: {}", dataId, value);
        countDownLatch.await(60,TimeUnit.SECONDS);
        logger.info(System.currentTimeMillis()+", dataId: {}, currenValue: {}", dataId, fileConfig.getBoolean(dataId));
        Assertions.assertNotEquals(fileConfig.getBoolean(dataId), value);
        //wait for loop safety, loop time is LISTENER_CONFIG_INTERVAL=1s
        CountDownLatch countDownLatch2 = new CountDownLatch(1);
        fileConfig.addConfigListener("file.listener.enabled", (CachedConfigurationChangeListener)event -> {
            if (!Boolean.parseBoolean(event.getNewValue())) {
                countDownLatch2.countDown();
            }
        });
        System.setProperty("file.listener.enabled", "false");
        countDownLatch2.await(10, TimeUnit.SECONDS);
        System.setProperty(dataId, String.valueOf(value));
        //sleep for a period of time to simulate waiting for a cache refresh.Actually, it doesn't trigger.
        Thread.sleep(1000);

        boolean currentValue = fileConfig.getBoolean(dataId);
        Assertions.assertNotEquals(value, currentValue);
        System.setProperty(dataId, String.valueOf(!value));
    }

    @Test
    void testDiffDefaultValue() {
        Configuration fileConfig = ConfigurationFactory.getInstance();
        int intValue1 = fileConfig.getInt("int.not.exist", 100);
        int intValue2 = fileConfig.getInt("int.not.exist", 200);
        Assertions.assertNotEquals(intValue1, intValue2);
        String strValue1 = fileConfig.getConfig("str.not.exist", "en");
        String strValue2 = fileConfig.getConfig("str.not.exist", "us");
        Assertions.assertNotEquals(strValue1, strValue2);
        boolean bolValue1 = fileConfig.getBoolean("boolean.not.exist", true);
        boolean bolValue2 = fileConfig.getBoolean("boolean.not.exist", false);
        Assertions.assertNotEquals(bolValue1, bolValue2);

        String value = "QWERT";
        System.setProperty("mockDataId1", value);
        String content1 = fileConfig.getConfig("mockDataId1");
        Assertions.assertEquals(content1, value);
        String content2 = fileConfig.getConfig("mockDataId1", "hehe");
        Assertions.assertEquals(content2, value);

        String content3 = fileConfig.getConfig("mockDataId2");
        Assertions.assertNull(content3);
        String content4 = fileConfig.getConfig("mockDataId2", value);
        Assertions.assertEquals(content4, value);
        String content5 = fileConfig.getConfig("mockDataId2");
        Assertions.assertEquals(content5, value);

    }

}
