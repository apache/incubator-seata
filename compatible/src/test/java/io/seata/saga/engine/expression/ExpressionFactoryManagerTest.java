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
package io.seata.saga.engine.expression;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for ExpressionFactoryManager interface compatibility wrapper.
 */
public class ExpressionFactoryManagerTest {

    @Test
    public void testDeprecatedAnnotation() {
        assertTrue(
                ExpressionFactoryManager.class.isAnnotationPresent(Deprecated.class),
                "ExpressionFactoryManager should be marked as @Deprecated");
    }

    @Test
    public void testCanInstantiate() {
        ExpressionFactoryManager manager = new ExpressionFactoryManager();
        assertTrue(manager != null, "ExpressionFactoryManager should be instantiable");
    }

    @Test
    public void testHasUnwrapMethod() throws NoSuchMethodException {
        ExpressionFactoryManager manager = new ExpressionFactoryManager();
        assertTrue(
                manager.unwrap() != null,
                "ExpressionFactoryManager should have unwrap method returning Apache instance");
        assertTrue(
                manager.unwrap() instanceof org.apache.seata.saga.engine.expression.ExpressionFactoryManager,
                "unwrap() should return Apache ExpressionFactoryManager instance");
    }
}
