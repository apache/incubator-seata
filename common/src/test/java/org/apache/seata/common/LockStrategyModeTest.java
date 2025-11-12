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
package org.apache.seata.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * The type Lock strategy mode test.
 */
public class LockStrategyModeTest {

    @Test
    public void testEnumValues() {
        LockStrategyMode[] values = LockStrategyMode.values();
        assertEquals(2, values.length);
        assertNotNull(LockStrategyMode.OPTIMISTIC);
        assertNotNull(LockStrategyMode.PESSIMISTIC);
    }

    @Test
    public void testEnumValueOf() {
        assertEquals(LockStrategyMode.OPTIMISTIC, LockStrategyMode.valueOf("OPTIMISTIC"));
        assertEquals(LockStrategyMode.PESSIMISTIC, LockStrategyMode.valueOf("PESSIMISTIC"));
    }
}
