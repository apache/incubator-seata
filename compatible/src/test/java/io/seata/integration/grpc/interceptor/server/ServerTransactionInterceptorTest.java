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
package io.seata.integration.grpc.interceptor.server;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for ServerTransactionInterceptor compatibility wrapper.
 */
public class ServerTransactionInterceptorTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ServerTransactionInterceptor.class.isAnnotationPresent(Deprecated.class),
                "ServerTransactionInterceptor should be marked as @Deprecated");
    }

    @Test
    public void testExtendsApacheSeataClass() {
        assertTrue(
                org.apache.seata.integration.grpc.interceptor.server.ServerTransactionInterceptor.class
                        .isAssignableFrom(ServerTransactionInterceptor.class),
                "ServerTransactionInterceptor should extend Apache Seata ServerTransactionInterceptor");
    }

    @Test
    public void testConstructor() {
        ServerTransactionInterceptor interceptor = new ServerTransactionInterceptor();
        assertNotNull(interceptor);
    }

    @Test
    public void testInstanceOfApacheSeataClass() {
        ServerTransactionInterceptor interceptor = new ServerTransactionInterceptor();
        assertTrue(
                interceptor
                        instanceof org.apache.seata.integration.grpc.interceptor.server.ServerTransactionInterceptor,
                "Instance should be of Apache Seata ServerTransactionInterceptor type");
    }
}
