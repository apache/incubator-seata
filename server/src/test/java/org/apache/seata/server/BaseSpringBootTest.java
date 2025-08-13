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

import org.apache.seata.config.ConfigurationCache;
import org.apache.seata.config.ConfigurationFactory;
import org.apache.seata.core.constants.ConfigurationKeys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = {"server.port=${random.int[10000,60000]}"})
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)
public abstract class BaseSpringBootTest {

    @BeforeAll
    public static void beforeAll() {
        System.setProperty(ConfigurationKeys.SHUTDOWN_WAIT, "1");
        ConfigurationCache.clear();
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }

    @AfterAll
    public static void afterAll() {
        ConfigurationCache.clear();
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }

    @AfterEach
    public void AfterEach() {
        ConfigurationFactory.reload();
        System.clearProperty(ConfigurationKeys.SERVER_SERVICE_PORT_CAMEL);
    }
}
