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
package org.apache.seata.benchmark;

import org.junit.jupiter.api.parallel.ResourceLock;
import org.junit.jupiter.api.parallel.Resources;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedConstruction;
import picocli.CommandLine;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mockConstruction;

@ResourceLock(Resources.SYSTEM_PROPERTIES)
class BenchmarkApplicationTest {

    @ParameterizedTest
    @ValueSource(strings = {
        "--server=127.0.0.1:9091 --tx-service-group=benchmark_test",
        "-s 127.0.0.1:9091 --tx-service-group benchmark_test"
    })
    void usesParsedOptionsToConfigureSeata(String options) {
        String[] keys = {
            "seata.config.type", "seata.registry.type", "service.default.grouplist", "service.vgroupMapping.benchmark_test"
        };
        Map<String, String> previous = new HashMap<>();
        for (String key : keys) {
            previous.put(key, System.getProperty(key));
            System.clearProperty(key);
        }
        try (MockedConstruction<BenchmarkRunner> runners = mockConstruction(BenchmarkRunner.class, (mock, context) -> {
            assertEquals("file", System.getProperty("seata.config.type"));
            assertEquals("file", System.getProperty("seata.registry.type"));
            assertEquals("127.0.0.1:9091", System.getProperty("service.default.grouplist"));
            assertEquals("default", System.getProperty("service.vgroupMapping.benchmark_test"));
        })) {
            assertEquals(0, new CommandLine(new BenchmarkApplication()).execute(
                    ("--threads=1 " + options).split(" ")));
            assertEquals(1, runners.constructed().size());
        } finally {
            previous.forEach((key, value) -> {
                if (value == null) {
                    System.clearProperty(key);
                } else {
                    System.setProperty(key, value);
                }
            });
        }
    }
}
