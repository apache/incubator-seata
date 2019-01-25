/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.config;

import java.util.concurrent.ExecutorService;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

/**
 * @Author: jimin.jm@alibaba-inc.com
 * @Project: feats-all
 * @DateTime: 2019/1/24 1:31 PM
 * @FileName: FileConfigurationTest
 * @Description:
 */
public class FileConfigurationTest {
    private static final Config CONFIG = ConfigFactory.load();
    private static final Configuration FILE_CONFIG = ConfigurationFactory.getInstance();
    private static final String INT_DATAID = "transport.thread-factory.client-selector-thread-size";
    private static final String LONG_DATAID = "transport.thread-factory.worker-thread-size";
    private static final String BOOLEAN_DATAID = "service.disable";
    private static final String STRING_DATAID = "transport.type";
    private static final String PUT_DATAID = "transport.mock";
    private static final String NOT_EXIST_DATAID = "service.yyy.xxx";

    @Test
    public void testGetInt() {
        Assert.assertEquals(FILE_CONFIG.getInt(INT_DATAID), CONFIG.getInt(INT_DATAID));
        Assert.assertEquals(FILE_CONFIG.getInt(NOT_EXIST_DATAID), 0);
    }

    @Test
    public void testGetInt1() {
        Assert.assertEquals(FILE_CONFIG.getInt(INT_DATAID, 999), CONFIG.getInt(INT_DATAID));
        Assert.assertEquals(FILE_CONFIG.getInt(NOT_EXIST_DATAID, 999), 999);
    }

    @Test
    public void testGetInt2() {
        Assert.assertEquals(FILE_CONFIG.getInt(INT_DATAID, 999, 1000), CONFIG.getInt(INT_DATAID));
        Assert.assertEquals(FILE_CONFIG.getInt(NOT_EXIST_DATAID, 999, 1000), 999);
    }

    @Test
    public void testGetLong() {
        Assert.assertEquals(FILE_CONFIG.getLong(LONG_DATAID), CONFIG.getLong(LONG_DATAID));
        Assert.assertEquals(FILE_CONFIG.getLong(NOT_EXIST_DATAID), 0);
    }

    @Test
    public void testGetLong1() {
        Assert.assertEquals(FILE_CONFIG.getLong(LONG_DATAID, 999L), CONFIG.getLong(LONG_DATAID));
        Assert.assertEquals(FILE_CONFIG.getLong(NOT_EXIST_DATAID, 999L), 999L);
    }

    @Test
    public void testGetLong2() {
        Assert.assertEquals(FILE_CONFIG.getLong(LONG_DATAID, 999L, 1000), CONFIG.getLong(LONG_DATAID));
        Assert.assertEquals(FILE_CONFIG.getLong(NOT_EXIST_DATAID, 999L, 1000), 999L);
    }

    @Test
    public void testGetBoolean() {
        Assert.assertEquals(FILE_CONFIG.getBoolean(BOOLEAN_DATAID), CONFIG.getBoolean(BOOLEAN_DATAID));
        Assert.assertEquals(FILE_CONFIG.getBoolean(NOT_EXIST_DATAID), false);
    }

    @Test
    public void testGetBoolean1() {
        Assert.assertEquals(FILE_CONFIG.getBoolean(BOOLEAN_DATAID, true), CONFIG.getBoolean(BOOLEAN_DATAID));
        Assert.assertEquals(FILE_CONFIG.getBoolean(NOT_EXIST_DATAID, false), false);
    }

    @Test
    public void testGetBoolean2() {
        Assert.assertEquals(FILE_CONFIG.getBoolean(BOOLEAN_DATAID, true, 1000), CONFIG.getBoolean(BOOLEAN_DATAID));
        Assert.assertEquals(FILE_CONFIG.getBoolean(NOT_EXIST_DATAID, false, 1000), false);
    }

    @Test
    public void testGetConfig() {
        Assert.assertEquals(FILE_CONFIG.getConfig(STRING_DATAID), CONFIG.getString(STRING_DATAID));
        Assert.assertEquals(FILE_CONFIG.getConfig(NOT_EXIST_DATAID), null);
    }

    @Test
    public void testGetConfig1() {
        Assert.assertEquals(FILE_CONFIG.getConfig(STRING_DATAID, 1000), CONFIG.getString(STRING_DATAID));
        Assert.assertEquals(FILE_CONFIG.getConfig(NOT_EXIST_DATAID, 1000), null);
    }

    @Test
    public void testGetConfig2() {
        Assert.assertEquals(FILE_CONFIG.getConfig(STRING_DATAID, "123"), CONFIG.getString(STRING_DATAID));
        Assert.assertEquals(FILE_CONFIG.getConfig(NOT_EXIST_DATAID, "123"), "123");
    }

    @Test
    public void testGetConfig3() {
        Assert.assertEquals(FILE_CONFIG.getConfig(STRING_DATAID, "123", 1000), CONFIG.getString(STRING_DATAID));
        Assert.assertEquals(FILE_CONFIG.getConfig(NOT_EXIST_DATAID, "123", 1000), "123");
    }

    @Test
    public void testPutConfig() {
        Assert.assertTrue(FILE_CONFIG.putConfig(PUT_DATAID, "123"));
    }

    @Test
    public void testPutConfig1() {
        Assert.assertTrue(FILE_CONFIG.putConfig(PUT_DATAID, "123", 5000));
    }

    @Test
    public void testPutConfigIfAbsent() {
        Assert.assertTrue(FILE_CONFIG.putConfigIfAbsent(PUT_DATAID, "123"));
    }

    @Test
    public void testPutConfigIfAbsent1() {
        Assert.assertTrue(FILE_CONFIG.putConfigIfAbsent(PUT_DATAID, "123", 5000));
    }

    @Test
    public void testRemoveConfig() {
        Assert.assertTrue(FILE_CONFIG.removeConfig(PUT_DATAID));
    }

    @Test
    public void testRemoveConfig1() {
        Assert.assertTrue(FILE_CONFIG.removeConfig(PUT_DATAID, 5000));
    }

    @Test(dataProvider = "listenerProvider")
    public void testAddConfigListener(ConfigChangeListener listener) {
        FILE_CONFIG.addConfigListener(INT_DATAID, listener);
        Assert.assertEquals(FILE_CONFIG.getConfigListeners(INT_DATAID).size(), 1);
    }

    @Test(dataProvider = "listenerProvider")
    public void testRemoveConfigListener(ConfigChangeListener listener) {
        int currSize = FILE_CONFIG.getConfigListeners(INT_DATAID).size();
        FILE_CONFIG.addConfigListener(INT_DATAID, listener);
        FILE_CONFIG.removeConfigListener(INT_DATAID, listener);
        Assert.assertEquals(FILE_CONFIG.getConfigListeners(INT_DATAID).size(), currSize);
    }

    @DataProvider
    public static Object[][] listenerProvider() {
        ConfigChangeListener listener = new ConfigChangeListener() {
            @Override
            public ExecutorService getExecutor() {
                return null;
            }

            @Override
            public void receiveConfigInfo(String configInfo) {
                System.out.print(configInfo);
            }
        };
        return new Object[][] {{listener}};
    }
}