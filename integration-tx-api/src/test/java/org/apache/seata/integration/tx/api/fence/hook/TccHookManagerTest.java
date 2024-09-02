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
package org.apache.seata.integration.tx.api.fence.hook;


import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;


public class TccHookManagerTest {

    @BeforeEach
    public void setUp() {
        TccHookManager.clear();
    }

    @Test
    public void testRegisterHook() {
        TccHook hook = mock(TccHook.class);
        TccHookManager.registerHook(hook);

        List<TccHook> hooks = TccHookManager.getHooks();
        assertEquals(1, hooks.size());
        assertTrue(hooks.contains(hook));
    }

    @Test
    public void testClear() {
        TccHook hook = mock(TccHook.class);
        TccHookManager.registerHook(hook);
        List<TccHook> hooks = TccHookManager.getHooks();
        assertEquals(1, hooks.size());
        assertTrue(hooks.contains(hook));

        TccHookManager.clear();

        assertTrue(TccHookManager.getHooks().isEmpty());
    }

    @Test
    public void testGetHooks() {
        TccHook hook1 = mock(TccHook.class);
        TccHook hook2 = mock(TccHook.class);
        TccHookManager.registerHook(hook1);
        TccHookManager.registerHook(hook2);

        List<TccHook> hooks = TccHookManager.getHooks();
        assertEquals(2, hooks.size());
        assertTrue(hooks.contains(hook1));
        assertTrue(hooks.contains(hook2));

        // Check unmodifiable list
        assertThrows(UnsupportedOperationException.class, () -> hooks.add(mock(TccHook.class)));
    }
}
