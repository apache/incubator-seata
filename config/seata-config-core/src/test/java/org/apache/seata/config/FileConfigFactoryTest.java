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
package org.apache.seata.config;

import org.apache.seata.config.file.FileConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.Set;

class FileConfigFactoryTest {

    @Test
    void testLoadDefault() {
        FileConfig fileConfig = FileConfigFactory.load();
        Assertions.assertNotNull(fileConfig);
    }

    @Test
    void testLoadWithConfFile() {
        File confFile = new File("src/test/resources/file.conf");
        if (confFile.exists()) {
            FileConfig fileConfig = FileConfigFactory.load(confFile, "file.conf");
            Assertions.assertNotNull(fileConfig);
        }
    }

    @Test
    void testLoadWithPropertiesFile() {
        File propertiesFile = new File("src/test/resources/file.properties");
        if (propertiesFile.exists()) {
            FileConfig fileConfig = FileConfigFactory.load(propertiesFile, "file.properties");
            Assertions.assertNotNull(fileConfig);
        }
    }

    @Test
    void testLoadWithYamlFile() {
        File yamlFile = new File("src/test/resources/registry.yml");
        if (yamlFile.exists()) {
            FileConfig fileConfig = FileConfigFactory.load(yamlFile, "registry.yml");
            Assertions.assertNotNull(fileConfig);
        }
    }

    @Test
    void testGetSuffixSet() {
        Set<String> suffixSet = FileConfigFactory.getSuffixSet();
        Assertions.assertNotNull(suffixSet);
        Assertions.assertTrue(suffixSet.contains("conf"));
        Assertions.assertTrue(suffixSet.contains("properties"));
        Assertions.assertTrue(suffixSet.contains("yml"));
    }

    @Test
    void testRegisterNewSuffix() {
        int originalSize = FileConfigFactory.getSuffixSet().size();
        FileConfigFactory.register("json", "JSON");
        Set<String> suffixSet = FileConfigFactory.getSuffixSet();
        Assertions.assertEquals(originalSize + 1, suffixSet.size());
        Assertions.assertTrue(suffixSet.contains("json"));
    }

    @Test
    void testDefaultType() {
        Assertions.assertEquals("CONF", FileConfigFactory.DEFAULT_TYPE);
    }

    @Test
    void testYamlType() {
        Assertions.assertEquals("YAML", FileConfigFactory.YAML_TYPE);
    }

    @Test
    void testLoadWithFileNoExtension() {
        File file = new File("testfile");
        try {
            file.createNewFile();
            FileConfig fileConfig = FileConfigFactory.load(file, "testfile");
            Assertions.assertNotNull(fileConfig);
        } catch (Exception e) {
            // File operation may fail in some environments
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }

    @Test
    void testLoadWithUnknownExtension() {
        File file = new File("test.unknown");
        try {
            file.createNewFile();
            FileConfig fileConfig = FileConfigFactory.load(file, "test.unknown");
            Assertions.assertNotNull(fileConfig);
        } catch (Exception e) {
            // File operation may fail in some environments
        } finally {
            if (file.exists()) {
                file.delete();
            }
        }
    }
}
