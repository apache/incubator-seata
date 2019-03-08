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

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.concurrent.ExecutorService;

import static org.assertj.core.api.Assertions.*;
/**
 * @author Wu
 * @date 2019/3/8
 */
public class ConfigurationFactoryTest {
    private final Config config;
    private static final String INT_DATAID = "transport.thread-factory.client-selector-thread-size";
    private static final String LONG_DATAID = "transport.thread-factory.worker-thread-size";
    private static final String BOOLEAN_DATAID = "service.disable";
    private static final String STRING_DATAID = "transport.type";
    private static final String PUT_DATAID = "transport.mock";
    private static final String NOT_EXIST_DATAID = "service.yyy.xxx";

    /**
     * Instantiates a new File configuration test for comparison
     */
    public ConfigurationFactoryTest() {
        config = ConfigFactory.load("file.conf");
    }

    @Test
    public void testGetInstance(){
        Configuration fileConfig=ConfigurationFactory.getInstance();
        assertThat(fileConfig).isNotNull().hasSameClassAs(new FileConfiguration());
        assertThat(fileConfig.getInt(INT_DATAID)).isEqualTo(config.getInt(INT_DATAID));
        assertThat(fileConfig.getInt(NOT_EXIST_DATAID)).isEqualTo(0);

        //validate read and write operation
        assertThat(fileConfig.getInt(INT_DATAID, 999)).isEqualTo(config.getInt(INT_DATAID));
        assertThat(fileConfig.getInt(NOT_EXIST_DATAID, 999)).isEqualTo(999);

        assertThat(fileConfig.getLong(LONG_DATAID)).isEqualTo(config.getLong(LONG_DATAID));
        assertThat(fileConfig.getLong(NOT_EXIST_DATAID)).isEqualTo(0);

        assertThat(fileConfig.getBoolean(BOOLEAN_DATAID)).isEqualTo(config.getBoolean(BOOLEAN_DATAID));
        assertThat(fileConfig.getBoolean(NOT_EXIST_DATAID)).isFalse();

        assertThat(fileConfig.getConfig(STRING_DATAID)).isEqualTo(config.getString(STRING_DATAID));
        assertThat(fileConfig.getConfig(NOT_EXIST_DATAID)).isNull();

        assertThat(fileConfig.putConfig(PUT_DATAID, "123")).isTrue();

        assertThat(fileConfig.putConfig(PUT_DATAID, "123", 5000)).isTrue();

        assertThat(fileConfig.putConfigIfAbsent(PUT_DATAID, "123")).isTrue();

        assertThat(fileConfig.removeConfig(PUT_DATAID)).isTrue();

        fileConfig.addConfigListener(INT_DATAID, listenerProvider());
        assertThat(fileConfig.getConfigListeners(INT_DATAID).size()).isEqualTo(1);

    }

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
