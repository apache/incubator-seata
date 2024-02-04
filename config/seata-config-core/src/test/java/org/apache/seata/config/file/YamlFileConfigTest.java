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
package org.apache.seata.config.file;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;



class YamlFileConfigTest {

    @Test
    void getString() throws IOException {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            YamlFileConfig config = new YamlFileConfig(new File("registry-test-yaml.yml"), "");
            config.getString("registry.type");
        });

        YamlFileConfig config = new YamlFileConfig(new File("src/test/resources/registry-test-yaml.yml"), "");
        Assertions.assertEquals("file", config.getString("registry.type"));
        Assertions.assertEquals("file.conf", config.getString("registry.file.name"));

        // not exist
        Assertions.assertNull(config.getString("registry.null.name"));
        Assertions.assertNull(config.getString("null"));
        // inner exception
        Assertions.assertNull(config.getString(null));
    }
}
