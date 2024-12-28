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
package org.apache.seata.spring.boot.autoconfigure;

import org.apache.seata.config.Configuration;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.config.FileConfiguration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = SeataCoreAutoConfiguration.class)
@TestPropertySource(locations = "classpath:application-test.properties")
public class SeataCoreAutoConfigurationTest {

    @Autowired
    private Environment environment;

    @Test
    public void testSeataPropertiesLoaded() {
        ConfigurationFactory.reload();

        // default file.conf
        String dbUrl = environment.getProperty("seata.store.db.url");
        assertEquals("jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true", dbUrl, "The DB URL should be correctly loaded from configuration");

        // overridden by application-test.properties
        String registryType = environment.getProperty("seata.config.type");
        assertEquals("file", registryType, "The config type should be 'file'");

        // overridden by application-test.properties
        String seataNamespaces = environment.getProperty("seata.config.nacos.namespace");
        assertEquals("seata-test-application.yml", seataNamespaces, "The Nacos namespace should be 'seata-test-application.yml'");
    }

    @Test
    public void testConfigFromFileUsingGetInstance() {
        Configuration configFromFile = ConfigurationFactory.getInstance();
        String dbUrlFromFile = configFromFile.getConfig("store.db.url");
        assertEquals("jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true", dbUrlFromFile, "The DB URL should be correctly loaded from file.conf");
        String storeFileDirFromFile = configFromFile.getConfig("store.file.dir");
        assertEquals("sessionStore", storeFileDirFromFile, "The storeFileDir should be 'sessionStore' in file.conf");
    }

    @Test
    public void testConfigFromFileUsingGetOriginFileInstance() {
        Optional<FileConfiguration> optionalConfigFromFile = ConfigurationFactory.getOriginFileInstance();
        assertTrue(optionalConfigFromFile.isPresent(), "The configuration from file.conf should be present");
        FileConfiguration configFromFile = optionalConfigFromFile.get();
        String dbUrlFromFile = configFromFile.getConfig("store.db.url");
        assertEquals("jdbc:mysql://127.0.0.1:3306/seata?rewriteBatchedStatements=true", dbUrlFromFile, "The DB URL should be correctly loaded from file.conf");
        String storeFileDirFromFile = configFromFile.getConfig("store.file.dir");
        assertEquals("sessionStore", storeFileDirFromFile, "The storeFileDir should be 'sessionStore' in file.conf");
    }

    @Test
    public void testConfigFromRegistryUsingGetOriginFileInstanceRegistry() {
        Configuration configFromRegistry = ConfigurationFactory.getOriginFileInstanceRegistry();
        String registryTypeFromRegistry = configFromRegistry.getConfig("config.type");
        assertEquals("file", registryTypeFromRegistry, "The config type should be 'file' in registry.conf");
        String seataNamespaceFromRegistry = configFromRegistry.getConfig("config.nacos.namespace");
        assertEquals("seata-test", seataNamespaceFromRegistry, "The Nacos namespace should be 'seata-test' in registry.conf");
    }
}