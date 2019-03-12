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
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type File configuration test.
 *
 * @author jimin.jm @alibaba-inc.com
 * @date 2019 /1/24
 */
public class FileConfigurationTest {
    private final Config config;
    private final Configuration fileConfig = new FileConfiguration("file.conf");
    private static final String INT_DATAID = "transport.thread-factory.client-selector-thread-size";
    private static final String LONG_DATAID = "transport.thread-factory.worker-thread-size";
    private static final String BOOLEAN_DATAID = "service.disable";
    private static final String STRING_DATAID = "transport.type";
    private static final String PUT_DATAID = "transport.mock";
    private static final String NOT_EXIST_DATAID = "service.yyy.xxx";

    /**
     * Instantiates a new File configuration test.
     */
    public FileConfigurationTest() {
        config = ConfigFactory.load("file.conf");
    }

    /**
     * Test get int.
     */
    @Test
    public void testGetInt() {
        assertThat(fileConfig.getInt(INT_DATAID)).isEqualTo(config.getInt(INT_DATAID));
        assertThat(fileConfig.getInt(NOT_EXIST_DATAID)).isEqualTo(0);
    }

    /**
     * Test get int 1.
     */
    @Test
    public void testGetInt1() {
        assertThat(fileConfig.getInt(INT_DATAID, 999)).isEqualTo(config.getInt(INT_DATAID));
        assertThat(fileConfig.getInt(NOT_EXIST_DATAID, 999)).isEqualTo(999);
    }

    /**
     * Test get int 2.
     */
    @Test
    public void testGetInt2() {
        assertThat(fileConfig.getInt(INT_DATAID, 999, 1000)).isEqualTo(config.getInt(INT_DATAID));
        assertThat(fileConfig.getInt(NOT_EXIST_DATAID, 999, 1000)).isEqualTo(999);
    }

    /**
     * Test get long.
     */
    @Test
    public void testGetLong() {
        assertThat(fileConfig.getLong(LONG_DATAID)).isEqualTo(config.getLong(LONG_DATAID));
        assertThat(fileConfig.getLong(NOT_EXIST_DATAID)).isEqualTo(0);
    }

    /**
     * Test get long 1.
     */
    @Test
    public void testGetLong1() {
        assertThat(fileConfig.getLong(LONG_DATAID, 999L)).isEqualTo(config.getLong(LONG_DATAID));
        assertThat(fileConfig.getLong(NOT_EXIST_DATAID, 999L)).isEqualTo(999L);
    }

    /**
     * Test get long 2.
     */
    @Test
    public void testGetLong2() {
        assertThat(fileConfig.getLong(LONG_DATAID, 999L, 1000)).isEqualTo(config.getLong(LONG_DATAID));
        assertThat(fileConfig.getLong(NOT_EXIST_DATAID, 999L, 1000)).isEqualTo(999L);
    }

    /**
     * Test get boolean.
     */
    @Test
    public void testGetBoolean() {
        assertThat(fileConfig.getBoolean(BOOLEAN_DATAID)).isEqualTo(config.getBoolean(BOOLEAN_DATAID));
        assertThat(fileConfig.getBoolean(NOT_EXIST_DATAID)).isFalse();
    }

    /**
     * Test get boolean 1.
     */
    @Test
    public void testGetBoolean1() {
        assertThat(fileConfig.getBoolean(BOOLEAN_DATAID, true)).isEqualTo(config.getBoolean(BOOLEAN_DATAID));
        assertThat(fileConfig.getBoolean(NOT_EXIST_DATAID, false)).isFalse();
    }

    /**
     * Test get boolean 2.
     */
    @Test
    public void testGetBoolean2() {
        assertThat(fileConfig.getBoolean(BOOLEAN_DATAID, true, 1000)).isEqualTo(config.getBoolean(BOOLEAN_DATAID));
        assertThat(fileConfig.getBoolean(NOT_EXIST_DATAID, false, 1000)).isFalse();
    }

    /**
     * Test get config.
     */
    @Test
    public void testGetConfig() {
        assertThat(fileConfig.getConfig(STRING_DATAID)).isEqualTo(config.getString(STRING_DATAID));
        assertThat(fileConfig.getConfig(NOT_EXIST_DATAID)).isNull();
    }

    /**
     * Test get config 1.
     */
    @Test
    public void testGetConfig1() {
        assertThat(fileConfig.getConfig(STRING_DATAID, 1000)).isEqualTo(config.getString(STRING_DATAID));
        assertThat(fileConfig.getConfig(NOT_EXIST_DATAID, 1000)).isNull();
    }

    /**
     * Test get config 2.
     */
    @Test
    public void testGetConfig2() {
        assertThat(fileConfig.getConfig(STRING_DATAID, "123")).isEqualTo(config.getString(STRING_DATAID));
        assertThat(fileConfig.getConfig(NOT_EXIST_DATAID, "123")).isEqualTo("123");
    }

    /**
     * Test get config 3.
     */
    @Test
    public void testGetConfig3() {
        assertThat(fileConfig.getConfig(STRING_DATAID, "123", 1000)).isEqualTo(config.getString(STRING_DATAID));
        assertThat(fileConfig.getConfig(NOT_EXIST_DATAID, "123", 1000)).isEqualTo("123");
    }

    /**
     * Test put config.
     */
    @Test
    public void testPutConfig() {
        assertThat(fileConfig.putConfig(PUT_DATAID, "123")).isTrue();
    }

    /**
     * Test put config 1.
     */
    @Test
    public void testPutConfig1() {
        assertThat(fileConfig.putConfig(PUT_DATAID, "123", 5000)).isTrue();
    }

    /**
     * Test put config if absent.
     */
    @Test
    public void testPutConfigIfAbsent() {
        assertThat(fileConfig.putConfigIfAbsent(PUT_DATAID, "123")).isTrue();

    }

    /**
     * Test put config if absent 1.
     */
    @Test
    public void testPutConfigIfAbsent1() {
        assertThat(fileConfig.putConfigIfAbsent(PUT_DATAID, "123", 5000)).isTrue();
    }

    /**
     * Test remove config.
     */
    @Test
    public void testRemoveConfig() {
        assertThat(fileConfig.removeConfig(PUT_DATAID)).isTrue();

    }

    /**
     * Test remove config 1.
     */
    @Test
    public void testRemoveConfig1() {
        assertThat(fileConfig.removeConfig(PUT_DATAID, 5000)).isTrue();
    }

    /**
     * Test add config listener.
     */
    @Test
    public void testAddConfigListener() {
        fileConfig.addConfigListener(INT_DATAID, listenerProvider());
        assertThat(fileConfig.getConfigListeners(INT_DATAID).size()).isEqualTo(1);
    }

    /**
     * Test remove config listener.
     */
    @Test
    public void testRemoveConfigListener() {
        ConfigChangeListener listener = listenerProvider();
        fileConfig.addConfigListener(INT_DATAID, listener);
        fileConfig.removeConfigListener(INT_DATAID, listener);
        assertThat(fileConfig.getConfigListeners(INT_DATAID)).isEmpty();
    }

    /**
     * Listener provider
     *
     * @return ConfigChangeListener config change listener
     */
    public ConfigChangeListener listenerProvider() {
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
        return listener;
    }
}