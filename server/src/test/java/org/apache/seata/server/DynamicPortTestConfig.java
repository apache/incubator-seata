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
package org.apache.seata.server;

import java.io.IOException;
import java.net.ServerSocket;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@TestConfiguration
public class DynamicPortTestConfig {
    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) throws IOException {
        try (ServerSocket serverSocket = new ServerSocket(0);
             ServerSocket exporterSocket = new ServerSocket(0);
        ) {
            int servicePort = serverSocket.getLocalPort();
            int exporterPort = exporterSocket.getLocalPort();

            registry.add("server.servicePort", () -> String.valueOf(servicePort));
            registry.add("metrics.exporter.prometheus.port", () -> String.valueOf(exporterPort));

            System.clearProperty("server.servicePort");
            System.clearProperty("metrics.exporter.prometheus.port");

            System.setProperty("server.servicePort", String.valueOf(servicePort));
            System.setProperty("metrics.exporter.prometheus.port", String.valueOf(exporterPort));
        }
    }
}
