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
package io.seata.spring.annotation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Test cases for GlobalTransactionScanner compatibility wrapper.
 */
public class GlobalTransactionScannerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                GlobalTransactionScanner.class.isAnnotationPresent(Deprecated.class),
                "GlobalTransactionScanner should be marked as @Deprecated");
    }

    @Test
    public void testConstructorWithTxServiceGroup() {
        GlobalTransactionScanner scanner = new GlobalTransactionScanner("test-service-group");
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testConstructorWithTxServiceGroupAndMode() {
        GlobalTransactionScanner scanner = new GlobalTransactionScanner("test-service-group", 1);
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testConstructorWithApplicationIdAndTxServiceGroup() {
        GlobalTransactionScanner scanner = new GlobalTransactionScanner("test-app", "test-service-group");
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-app", scanner.getApplicationId());
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testConstructorWithApplicationIdTxServiceGroupAndMode() {
        GlobalTransactionScanner scanner = new GlobalTransactionScanner("test-app", "test-service-group", 1);
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-app", scanner.getApplicationId());
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testExtendsApacheGlobalTransactionScanner() {
        assertTrue(
                org.apache.seata.spring.annotation.GlobalTransactionScanner.class.isAssignableFrom(
                        GlobalTransactionScanner.class),
                "GlobalTransactionScanner should extend org.apache.seata.spring.annotation.GlobalTransactionScanner");
    }

    @Test
    public void testConstructorWithApplicationIdTxServiceGroupAndFailureHandler() {
        io.seata.tm.api.FailureHandler failureHandler = mock(io.seata.tm.api.FailureHandler.class);
        GlobalTransactionScanner scanner =
                new GlobalTransactionScanner("test-app", "test-service-group", failureHandler);
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-app", scanner.getApplicationId());
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testConstructorWithApplicationIdTxServiceGroupExposeProxyAndFailureHandler() {
        io.seata.tm.api.FailureHandler failureHandler = mock(io.seata.tm.api.FailureHandler.class);
        GlobalTransactionScanner scanner =
                new GlobalTransactionScanner("test-app", "test-service-group", true, failureHandler);
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-app", scanner.getApplicationId());
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testConstructorWithApplicationIdTxServiceGroupModeAndFailureHandler() {
        io.seata.tm.api.FailureHandler failureHandler = mock(io.seata.tm.api.FailureHandler.class);
        GlobalTransactionScanner scanner =
                new GlobalTransactionScanner("test-app", "test-service-group", 1, failureHandler);
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-app", scanner.getApplicationId());
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }

    @Test
    public void testConstructorWithAllParameters() {
        io.seata.tm.api.FailureHandler failureHandler = mock(io.seata.tm.api.FailureHandler.class);
        GlobalTransactionScanner scanner =
                new GlobalTransactionScanner("test-app", "test-service-group", 1, true, failureHandler);
        assertNotNull(scanner, "Scanner should be created");
        assertEquals("test-app", scanner.getApplicationId());
        assertEquals("test-service-group", scanner.getTxServiceGroup());
    }
}
