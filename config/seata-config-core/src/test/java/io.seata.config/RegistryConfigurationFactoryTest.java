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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RegistryConfigurationFactoryTest {
    private static final String ENV_PROPERTY_KEY = "seataEnv";
    private static final String SYSTEM_PROPERTY_SEATA_CONFIG_NAME = "seata.config.name";
    private static final String REGISTRY_CONF_DEFAULT = "registry";
    static {
        System.setProperty(ENV_PROPERTY_KEY,"test");
        System.setProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME,REGISTRY_CONF_DEFAULT);
    }


    @Test
    void getInstance() {
        ConfigurationFactory.reload();
        Assertions.assertEquals(ConfigurationFactory.CURRENT_FILE_INSTANCE.getConfig("config.file.name"),"file-test.conf");
        Configuration instance = ConfigurationFactory.getInstance();
        Assertions.assertEquals(instance.getConfig("service.disableGlobalTransaction"),"true");
        Assertions.assertEquals(instance.getConfig("service.default.grouplist"), "127.0.0.1:8091");

    }
    @AfterAll
    public static void afterAll(){
        System.clearProperty(ENV_PROPERTY_KEY);
        System.clearProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        ConfigurationFactory.reload();
    }
}