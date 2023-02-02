/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.config;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author slievrly
 */
class FileConfigSourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void addConfigListener() throws InterruptedException {
        ConfigurationFactory.cleanCaches();

        String dataId = "mockDataId";
        System.setProperty(dataId, "false");

        Configuration fileConfig = ConfigurationFactory.getInstance();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        boolean value = fileConfig.getBoolean(dataId);
        ConfigurationFactory.addConfigListener(dataId, (event) -> {
            Assertions.assertEquals(Boolean.parseBoolean(event.getNewValue()), !Boolean.parseBoolean(event.getOldValue()));
            countDownLatch.countDown();
        });

        System.setProperty(dataId, String.valueOf(!value));
        ConfigurationFactory.removeCache(dataId);
        countDownLatch.await(5, TimeUnit.SECONDS);
        System.setProperty("file.listener.enabled", "false");

        System.setProperty(dataId, String.valueOf(value));
        ConfigurationFactory.removeCache(dataId);
        Thread.sleep(2000);
        boolean currentValue = fileConfig.getBoolean(dataId);
        Assertions.assertNotEquals(value, currentValue);
        System.setProperty(dataId, String.valueOf(!value));

        ConfigurationFactory.cleanCaches();
    }

    @Test
    void testDiffDefaultValue() {
        ConfigurationFactory.cleanCaches();

        Configuration fileConfig = ConfigurationFactory.getInstance();
        int intValue1 = fileConfig.getInt("int.not.exist", 100);
        int intValue2 = fileConfig.getInt("int.not.exist", 200);
        Assertions.assertNotEquals(intValue1, intValue2);
        String strValue1 = fileConfig.getString("str.not.exist", "en");
        String strValue2 = fileConfig.getString("str.not.exist", "us");
        Assertions.assertNotEquals(strValue1, strValue2);
        boolean bolValue1 = fileConfig.getBoolean("boolean.not.exist", true);
        boolean bolValue2 = fileConfig.getBoolean("boolean.not.exist", false);
        Assertions.assertNotEquals(bolValue1, bolValue2);

        String value = "QWERT";
        System.setProperty("mockDataId1", value);
        String content1 = fileConfig.getString("mockDataId1");
        Assertions.assertEquals(content1, value);
        String content2 = fileConfig.getString("mockDataId1", "hehe");
        Assertions.assertEquals(content2, value);

        String content3 = fileConfig.getString("mockDataId2");
        Assertions.assertNull(content3);
        String content4 = fileConfig.getString("mockDataId2", value);
        Assertions.assertEquals(content4, value);
        String content5 = fileConfig.getString("mockDataId2");
        Assertions.assertNull(content5);


        // test blank value
        value = "";
        System.setProperty("mockDataId3", value);
        Assertions.assertEquals(fileConfig.getString("mockDataId3"), value);
        Assertions.assertNotEquals(fileConfig.getString("mockDataId3", "1"), value);

        ConfigurationFactory.cleanCaches();
    }

}
