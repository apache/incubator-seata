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
package org.apache.seata.config.processor;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ProcessorYamlTest {

    @Test
    void testProcessor_NormalYaml() {
        String yamlConfig = "server:\n" + "  port: 8080\n"
                + "  host: localhost\n"
                + "spring:\n"
                + "  datasource:\n"
                + "    url: jdbc:mysql://localhost:3306/test\n"
                + "    username: root";

        ProcessorYaml processorYaml = new ProcessorYaml();
        Properties props = processorYaml.processor(yamlConfig);

        assertEquals("8080", props.getProperty("server.port", ""));
        assertEquals("localhost", props.getProperty("server.host"));
        assertEquals("jdbc:mysql://localhost:3306/test", props.getProperty("spring.datasource.url"));
        assertEquals("root", props.getProperty("spring.datasource.username"));
    }

    @Test
    void testProcessor_InvalidYaml_ShouldThrowException() {

        String invalidYaml = "server:\n" + "  port: 8080\n" + "::host localhost";

        ProcessorYaml processorYaml = new ProcessorYaml();

        Assertions.assertThrows(Exception.class, () -> {
            processorYaml.processor(invalidYaml);
        });
    }

    @Test
    void testProcessor_EmptyYaml() {
        String emptyYaml = "";
        ProcessorYaml processorYaml = new ProcessorYaml();
        Properties props = processorYaml.processor(emptyYaml);
        assertTrue(props.size() == 1);
    }
}
