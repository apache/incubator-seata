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
package org.apache.seata.server.env;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class PortConfigUnitTest {
    @TempDir
    Path directory;

    @Test
    void explicitPropertiesAndYamlSupportValidMissingAndMalformedPorts() throws Exception {
        String previous = System.getProperty("spring.config.location");
        try {
            Path file = directory.resolve("application.properties");
            System.setProperty("spring.config.location", file.toString());
            Files.write(file, "server.port=9090".getBytes(StandardCharsets.UTF_8));
            assertEquals(9090, PortHelper.getPortFromConfigFile());
            Files.write(file, "server.port=invalid".getBytes(StandardCharsets.UTF_8));
            assertEquals(8080, PortHelper.getPortFromConfigFile());
            Files.write(file, "other=value".getBytes(StandardCharsets.UTF_8));
            assertEquals(8080, PortHelper.getPortFromConfigFile());
            Path yaml = directory.resolve("application.yml");
            System.setProperty("spring.config.location", yaml.toString());
            Files.write(yaml, "server:\n  port: 9191\n".getBytes(StandardCharsets.UTF_8));
            assertEquals(9191, PortHelper.getPortFromConfigFile());
            Files.write(yaml, "other: value".getBytes(StandardCharsets.UTF_8));
            assertEquals(8080, PortHelper.getPortFromConfigFile());
        } finally {
            if (previous == null) {
                System.clearProperty("spring.config.location");
            } else {
                System.setProperty("spring.config.location", previous);
            }
        }
    }
}
