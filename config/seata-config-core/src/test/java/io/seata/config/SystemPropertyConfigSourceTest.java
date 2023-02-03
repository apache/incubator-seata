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
 * @author wang.liang
 */
class SystemPropertyConfigSourceTest {

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testAddConfigListener() throws InterruptedException {
        Configuration config = ConfigurationFactory.getInstance();

        String dataId = "mockDataId";

        // false
        System.setProperty(dataId, "false");
        boolean value = config.getBoolean(dataId);
        Assertions.assertFalse(value);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        CountDownLatch countDownLatch2 = new CountDownLatch(2);
        ConfigurationFactory.addConfigListener(dataId, (event) -> {
            countDownLatch.countDown();
            countDownLatch2.countDown();
        });

        // true
        System.setProperty(dataId, "true");
        countDownLatch.await(3, TimeUnit.SECONDS);
        value = config.getBoolean(dataId);
        Assertions.assertTrue(value);

        // false
        System.setProperty(dataId, "false");
        countDownLatch2.await(3, TimeUnit.SECONDS);
        value = config.getBoolean(dataId);
        Assertions.assertFalse(value);

        // clean
        System.clearProperty(dataId);
        ConfigurationFactory.reload();
    }

    @Test
    void testDiffDefaultValue() {
        Configuration config = ConfigurationFactory.getInstance();
        ConfigurationFactory.cleanCaches();

        int intValue1 = config.getInt("int.not.exist", 100);
        int intValue2 = config.getInt("int.not.exist", 200);
        Assertions.assertNotEquals(intValue1, intValue2);
        String strValue1 = config.getString("str.not.exist", "en");
        String strValue2 = config.getString("str.not.exist", "us");
        Assertions.assertNotEquals(strValue1, strValue2);
        boolean bolValue1 = config.getBoolean("boolean.not.exist", true);
        boolean bolValue2 = config.getBoolean("boolean.not.exist", false);
        Assertions.assertNotEquals(bolValue1, bolValue2);

        String value = "QWERT";
        System.setProperty("mockDataId1", value);
        String content1 = config.getString("mockDataId1");
        Assertions.assertEquals(content1, value);
        String content2 = config.getString("mockDataId1", "hehe");
        Assertions.assertEquals(content2, value);

        String content3 = config.getString("mockDataId2");
        Assertions.assertNull(content3);
        String content4 = config.getString("mockDataId2", value);
        Assertions.assertEquals(content4, value);
        String content5 = config.getString("mockDataId2");
        Assertions.assertNull(content5);


        // test blank value
        value = "";
        System.setProperty("mockDataId3", value);
        Assertions.assertEquals(config.getString("mockDataId3"), value);
        Assertions.assertNotEquals(config.getString("mockDataId3", "1"), value);


        System.clearProperty("mockDataId1");
        System.clearProperty("mockDataId3");
        ConfigurationFactory.reload();
    }

}
