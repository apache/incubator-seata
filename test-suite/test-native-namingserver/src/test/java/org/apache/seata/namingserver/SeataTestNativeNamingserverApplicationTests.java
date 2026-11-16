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
package org.apache.seata.namingserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot context-load test for the native Namingserver test application.
 *
 * <p><b>Prerequisite:</b> This test validates that the Spring application context
 * starts successfully. The full integration tests (see sibling test classes in this
 * package) additionally require a native Namingserver binary running on
 * {@code 127.0.0.1:8081} — the CI workflow starts it automatically, but running
 * locally requires building the native image first
 * (see the {@code test-native-namingserver} Maven profile / Makefile targets).
 */
@SpringBootTest
class SeataTestNativeNamingserverApplicationTests {

    @Test
    void contextLoads() {}
}
