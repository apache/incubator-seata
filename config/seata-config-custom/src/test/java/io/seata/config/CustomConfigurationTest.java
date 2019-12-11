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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

/**
 * @author ggndnn
 */
public class CustomConfigurationTest {
    @Test
    public void testCustomConfigLoad() throws Exception {
        Configuration configuration = ConfigurationFactory.getInstance();
        Assertions.assertTrue(configuration instanceof CustomConfigurationForTest);
        Properties properties;
        try (InputStream input = CustomConfigurationForTest.class.getClassLoader().getResourceAsStream("custom_for_test.properties")) {
            properties = new Properties();
            properties.load(input);
        }
        Assertions.assertNotNull(properties);
        for (String name : properties.stringPropertyNames()) {
            String value = properties.getProperty(name);
            Assertions.assertNotNull(value);
            Assertions.assertEquals(value, configuration.getConfig(name));
        }
    }
}