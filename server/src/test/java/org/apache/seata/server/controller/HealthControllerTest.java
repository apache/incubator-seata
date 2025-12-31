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
package org.apache.seata.server.controller;

import org.apache.seata.server.ServerRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * HealthController Test
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HealthController Test")
class HealthControllerTest {

    private HealthController healthController;

    @Mock
    private ServerRunner mockServerRunner;

    @BeforeEach
    void setUp() throws Exception {
        healthController = new HealthController();
        // Use reflection to set private field
        Field field = HealthController.class.getDeclaredField("serverRunner");
        field.setAccessible(true);
        field.set(healthController, mockServerRunner);
    }

    @Test
    @DisplayName("test health check when server started")
    void testHealthCheckWhenServerStarted() {
        when(mockServerRunner.started()).thenReturn(true);

        String result = healthController.healthCheck();

        assertEquals("ok", result);
        verify(mockServerRunner).started();
    }

    @Test
    @DisplayName("test health check when server not started")
    void testHealthCheckWhenServerNotStarted() {
        when(mockServerRunner.started()).thenReturn(false);

        String result = healthController.healthCheck();

        assertEquals("not_ok", result);
        verify(mockServerRunner).started();
    }

    @Test
    @DisplayName("test health check multiple times")
    void testHealthCheckMultipleTimes() {
        when(mockServerRunner.started()).thenReturn(true);

        String result1 = healthController.healthCheck();
        String result2 = healthController.healthCheck();

        assertEquals("ok", result1);
        assertEquals("ok", result2);
        verify(mockServerRunner, times(2)).started();
    }

    @Test
    @DisplayName("test health check state transition")
    void testHealthCheckStateTransition() {
        when(mockServerRunner.started()).thenReturn(false);
        String result1 = healthController.healthCheck();
        assertEquals("not_ok", result1);

        when(mockServerRunner.started()).thenReturn(true);
        String result2 = healthController.healthCheck();
        assertEquals("ok", result2);
    }
}
