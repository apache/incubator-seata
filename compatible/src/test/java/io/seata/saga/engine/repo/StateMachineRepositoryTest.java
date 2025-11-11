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
package io.seata.saga.engine.repo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for StateMachineRepository interface compatibility wrapper.
 */
public class StateMachineRepositoryTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                StateMachineRepository.class.isAnnotationPresent(Deprecated.class),
                "StateMachineRepository should be marked as @Deprecated");
    }

    @Test
    public void testIsInterface() {
        assertTrue(StateMachineRepository.class.isInterface(), "StateMachineRepository should be an interface");
    }

    @Test
    public void testHasSameMethodSignatures() {
        // Verify that the interface has the main methods of Apache StateMachineRepository
        assertTrue(StateMachineRepository.class.isInterface(), "StateMachineRepository should be an interface");
        try {
            StateMachineRepository.class.getMethod("getStateMachineById", String.class);
            StateMachineRepository.class.getMethod("getStateMachine", String.class, String.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(
                    "StateMachineRepository should have same method signatures as Apache interface", e);
        }
    }
}
