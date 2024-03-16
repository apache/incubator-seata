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
package org.apache.seata.common;

import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * the type ConfigurationTestHelper
 **/
public class ConfigurationTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationTestHelper.class);
    private static final long PUT_CONFIG_TIMEOUT = 60000L;

    public static void removeConfig(String dataId) {
        //ConfigurationCache.removeConfigListener(dataId);
        String content = System.clearProperty(dataId);
        ConfigurationFactory.getInstance().removeConfig(dataId);
        LOGGER.info("removeConfig, dataId={}, content={}", dataId, content);
    }

    public static void putConfig(String dataId, String content) {
        if (content == null) {
            removeConfig(dataId);
            return;
        }

        CountDownLatch countDownLatch = new CountDownLatch(1);
        ConfigurationCache.addConfigListener(dataId, event -> countDownLatch.countDown());
        System.setProperty(dataId, content);
        ConfigurationFactory.getInstance().putConfig(dataId, content);

        try {
            boolean await = countDownLatch.await(PUT_CONFIG_TIMEOUT, TimeUnit.MILLISECONDS);
            if(await){
                LOGGER.info("putConfig ok, dataId={}, content={}", dataId, content);
            }else {
                LOGGER.error("putConfig fail, dataId={}, content={}", dataId, content);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
