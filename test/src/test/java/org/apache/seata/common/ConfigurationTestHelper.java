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

import java.lang.reflect.Method;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.seata.config.CachedConfigurationChangeListener;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the type ConfigurationTestHelper
 **/
public class ConfigurationTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationTestHelper.class);
    private static final long PUT_CONFIG_TIMEOUT = 60000L;

    static {
        System.setProperty("config.type","file");
        System.setProperty("config.file.name","file.conf");
        System.setProperty("file.listener.enabled","true");
        try {
            Method method = ConfigurationFactory.class.getDeclaredMethod("reload");
            method.setAccessible(true);
            method.invoke( null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void removeConfig(String dataId) {
        putConfig(dataId, null);
    }

    public static void putConfig(String dataId, String content) {
        CountDownLatch countDownLatch = new CountDownLatch(1);

        ConfigurationFactory.getInstance().addConfigListener(dataId,
	        (CachedConfigurationChangeListener)event -> countDownLatch.countDown());
        if (content == null) {
            System.clearProperty(dataId);
            return;
        }

        System.setProperty(dataId, content);

        try {
            boolean await = countDownLatch.await(PUT_CONFIG_TIMEOUT, TimeUnit.MILLISECONDS);
            if (await) {
                LOGGER.info("putConfig ok, dataId={}, value={}", dataId, ConfigurationFactory.getInstance().getConfig(dataId));
            } else {
                LOGGER.error("putConfig fail, dataId={}, value={}", dataId, ConfigurationFactory.getInstance().getConfig(dataId));
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

    }
}
