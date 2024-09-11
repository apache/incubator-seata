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
package org.apache.seata.config.zk;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.curator.test.TestingServer;
import org.apache.seata.config.ConfigurationChangeEvent;
import org.apache.seata.config.ConfigurationChangeListener;
import org.apache.seata.config.ConfigurationChangeType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The type zk configuration test
 */
public class ZkConfigurationTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ZkConfigurationTest.class);

    protected static TestingServer server = null;

    @BeforeAll
    public static void adBeforeClass() throws Exception {
        System.setProperty("config.type", "zk");
        System.setProperty("config.zk.serverAddr", "127.0.0.1:2181");
        server = new TestingServer(2181);
        server.start();
    }

    @AfterAll
    public static void adAfterClass() throws Exception {
        if (server != null) {
            server.stop();
        }
    }

    @Test
    public void testCheckExist() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        boolean exist = zookeeperConfiguration.checkExists("/");
        Assertions.assertTrue(exist);
    }

    @Test
    public void testPutConfig() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final boolean[] listened = {false};
        String dataId = "putMockDataId";
        ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                LOGGER.info("onChangeEvent:{}", event);
                if (event.getChangeType() == ConfigurationChangeType.MODIFY) {
                    Assertions.assertEquals("value2", event.getNewValue());
                    listened[0] = true;
                    countDownLatch.countDown();
                }
            }
        };
        zookeeperConfiguration.createPersistent(zookeeperConfiguration.buildPath(dataId), "value");
        zookeeperConfiguration.addConfigListener(dataId, changeListener);
        zookeeperConfiguration.putConfig(dataId, "value2");
        try {
            countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        Assertions.assertTrue(listened[0]);

        zookeeperConfiguration.removeConfig(dataId);

        zookeeperConfiguration.removeConfigListener(dataId, changeListener);
    }

    @Test
    public void testRemoveConfig() {
        ZookeeperConfiguration zookeeperConfiguration = new ZookeeperConfiguration();
        CountDownLatch countDownLatch = new CountDownLatch(1);
        final boolean[] listened = {false};
        String dataId = "removeMockDataId";
        zookeeperConfiguration.createPersistent(zookeeperConfiguration.buildPath(dataId), "value");
        ConfigurationChangeListener changeListener = new ConfigurationChangeListener() {
            @Override
            public void onChangeEvent(ConfigurationChangeEvent event) {
                LOGGER.info("onChangeEvent:{}", event);
                if (event.getChangeType() == ConfigurationChangeType.DELETE) {
                    Assertions.assertNull(event.getNewValue());
                    listened[0] = true;
                    countDownLatch.countDown();
                }
            }
        };

        zookeeperConfiguration.addConfigListener(dataId, changeListener);
        zookeeperConfiguration.putConfig(dataId, "value2");
        boolean remove = zookeeperConfiguration.removeConfig(dataId);
        Assertions.assertTrue(remove);
        try {
            countDownLatch.await(10000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        Assertions.assertTrue(listened[0]);
    }

}
