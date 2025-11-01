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
package io.seata.saga.engine;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for StateMachineConfig interface compatibility wrapper.
 */
public class StateMachineConfigTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateMachineConfig.class.isAnnotationPresent(Deprecated.class),
                "StateMachineConfig should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        assertTrue(StateMachineConfig.class.isInterface(), "StateMachineConfig should be an interface");
    }
}
