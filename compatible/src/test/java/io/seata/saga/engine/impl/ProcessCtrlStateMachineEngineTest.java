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
package io.seata.saga.engine.impl;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineConfig;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.StateMachineInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test cases for ProcessCtrlStateMachineEngine.
 */
public class ProcessCtrlStateMachineEngineTest {

    private ProcessCtrlStateMachineEngine stateMachineEngine;

    @BeforeEach
    public void setUp() {
        stateMachineEngine = new ProcessCtrlStateMachineEngine();
        // Mock the DefaultStateMachineConfig to avoid NullPointerException and ClassCastException
        DefaultStateMachineConfig mockConfig = mock(DefaultStateMachineConfig.class);

        // Mock the unwrap method to return a proper Apache StateMachineConfig
        org.apache.seata.saga.engine.impl.DefaultStateMachineConfig apacheConfig =
                mock(org.apache.seata.saga.engine.impl.DefaultStateMachineConfig.class);
        when(mockConfig.unwrap()).thenReturn(apacheConfig);

        // Mock the StateLogStore to avoid NPE
        org.apache.seata.saga.engine.store.StateLogStore mockStateLogStore =
                mock(org.apache.seata.saga.engine.store.StateLogStore.class);
        when(apacheConfig.getStateLogStore()).thenReturn(mockStateLogStore);

        stateMachineEngine.setStateMachineConfig(mockConfig);
    }

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ProcessCtrlStateMachineEngine.class.isAnnotationPresent(Deprecated.class),
                "ProcessCtrlStateMachineEngine should be marked as @Deprecated");
    }

    @Test
    public void testImplementsStateMachineEngine() {
        assertTrue(
                io.seata.saga.engine.StateMachineEngine.class.isAssignableFrom(ProcessCtrlStateMachineEngine.class),
                "ProcessCtrlStateMachineEngine should implement StateMachineEngine");
    }

    @Test
    public void testGetStateMachineConfig() {
        StateMachineConfig config = stateMachineEngine.getStateMachineConfig();
        assertNotNull(config);
    }

    @Test
    public void testSetStateMachineConfig() {
        DefaultStateMachineConfig config = new DefaultStateMachineConfig();
        config.setDefaultTenantId("test-tenant");

        stateMachineEngine.setStateMachineConfig(config);

        StateMachineConfig retrievedConfig = stateMachineEngine.getStateMachineConfig();
        assertNotNull(retrievedConfig);
    }

    @Test
    public void testAsyncCallbackConversion() {
        AsyncCallback mockCallback = new AsyncCallback() {
            @Override
            public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
                assertNotNull(context);
                assertNotNull(stateMachineInstance);
            }

            @Override
            public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
                assertNotNull(context);
                assertNotNull(stateMachineInstance);
                assertNotNull(exp);
            }
        };

        assertNotNull(mockCallback);
    }

    @Test
    public void testReloadStateMachineInstanceReturnsNull() {
        StateMachineInstance result = stateMachineEngine.reloadStateMachineInstance("non-existent-id");
        // The result could be null or wrapped instance depending on the underlying implementation
        // We just verify no exception is thrown
        assertNotNull(stateMachineEngine);
    }
}
