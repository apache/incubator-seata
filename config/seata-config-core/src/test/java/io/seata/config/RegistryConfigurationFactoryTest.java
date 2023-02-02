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

import static io.seata.config.ConfigProperty.ENV_PROPERTY_KEY;
import static io.seata.config.ConfigProperty.REGISTRY_CONF_DEFAULT;
import static io.seata.config.ConfigProperty.SYSTEM_PROPERTY_SEATA_CONFIG_NAME;


/**
 * @author wangwei-ying
 */
class RegistryConfigurationFactoryTest {

    @Test
    void getInstance() {
        System.setProperty(ENV_PROPERTY_KEY,"test");
        System.setProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME,REGISTRY_CONF_DEFAULT);
        ConfigurationFactory.reload();
        Assertions.assertEquals("file-test.conf",ConfigurationFactory.getInstance().getString("config.file.name"));
        Configuration instance = ConfigurationFactory.getInstance();
        Assertions.assertEquals("true",instance.getString("service.disableGlobalTransaction"));
        Assertions.assertEquals("127.0.0.1:8091", instance.getString("service.default.grouplist"));

    }
    @AfterAll
    public static void afterAll(){
        System.clearProperty(ENV_PROPERTY_KEY);
        System.clearProperty(SYSTEM_PROPERTY_SEATA_CONFIG_NAME);
        ConfigurationFactory.reload();
    }
}