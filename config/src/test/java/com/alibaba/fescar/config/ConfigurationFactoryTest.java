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

import static org.assertj.core.api.Assertions.assertThat;
import com.alibaba.fescar.common.exception.NotSupportYetException;
import org.junit.Test;
import static com.alibaba.fescar.config.ConfigurationFactory.FILE_INSTANCE;

/**
 * @author Wu
 * @date 2019/3/9
 */
public class ConfigurationFactoryTest {
    /**
     * get config type from registry.conf
     */
    private final static ConfigType CONFIG_TYPE = ConfigType.getType(
            FILE_INSTANCE.getConfig(ConfigurationKeys.FILE_ROOT_CONFIG + ConfigurationKeys.FILE_CONFIG_SPLIT_CHAR
                    + ConfigurationKeys.FILE_ROOT_TYPE));
    /**
     * get configuration instance
     */
    private final static Configuration CONFIGURATION_INSTANCE = ConfigurationFactory.getInstance();

    /**
     * test for getInstance method
     */
    @Test
    public void testGetInstance() throws NotSupportYetException {
        if (ConfigType.Nacos.equals(CONFIG_TYPE)) {
            assertThat(CONFIGURATION_INSTANCE).isInstanceOf(NacosConfiguration.class);
        } else if (ConfigType.Apollo.equals(CONFIG_TYPE)) {
            assertThat(CONFIGURATION_INSTANCE).isInstanceOf(ApolloConfiguration.class);
        } else if (ConfigType.File.equals(CONFIG_TYPE)) {
            assertThat(CONFIGURATION_INSTANCE).isInstanceOf(FileConfiguration.class);
        }
    }
}
