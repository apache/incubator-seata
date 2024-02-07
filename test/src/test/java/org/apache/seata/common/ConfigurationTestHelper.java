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

import org.apache.commons.lang.ObjectUtils;
import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * the type ConfigurationTestHelper
 **/
public class ConfigurationTestHelper {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationTestHelper.class);
    private static final long PUT_CONFIG_TIMEOUT = 30000L;
    private static final long PUT_CONFIG_CHECK_GAP = 500L;

    public static void removeConfig(String dataId) {
        putConfig(dataId, null);
    }

    public static void putConfig(String dataId, String content) {
        ConfigurationCache.addConfigListener(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
        if (content == null) {
            System.clearProperty(dataId);
            ConfigurationFactory.getInstance().removeConfig(dataId);
            return;
        }

        System.setProperty(dataId, content);
        ConfigurationFactory.getInstance().putConfig(dataId, content);

        long start = System.currentTimeMillis();
        while (!ObjectUtils.equals(content, ConfigurationFactory.getInstance().getConfig(dataId))) {
            if (PUT_CONFIG_TIMEOUT < System.currentTimeMillis() - start) {
                LOGGER.error("putConfig timeout, dataId={}, timeout={}ms", dataId, PUT_CONFIG_TIMEOUT);
                return;
            }
            try {
                Thread.sleep(PUT_CONFIG_CHECK_GAP);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        LOGGER.info("putConfig ok, dataId={}, cost {}ms", dataId, System.currentTimeMillis() - start);
    }
}
