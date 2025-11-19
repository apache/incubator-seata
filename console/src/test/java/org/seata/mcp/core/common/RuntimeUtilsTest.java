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
package org.seata.mcp.core.common;

import org.apache.seata.mcp.core.common.RuntimeUtils;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for RuntimeUtils
 */
class RuntimeUtilsTest {

    @Test
    void testHasText() {
        assertTrue(RuntimeUtils.hasText("text"));
        assertTrue(RuntimeUtils.hasText("  text  "));
        assertFalse(RuntimeUtils.hasText(null));
        assertFalse(RuntimeUtils.hasText(""));
        assertFalse(RuntimeUtils.hasText("   "));
        assertFalse(RuntimeUtils.hasText("\t\n"));
    }

    @Test
    void testIsBlank() {
        assertTrue(RuntimeUtils.isBlank(null));
        assertTrue(RuntimeUtils.isBlank(""));
        assertTrue(RuntimeUtils.isBlank("   "));
        assertTrue(RuntimeUtils.isBlank("\t\n"));
        assertFalse(RuntimeUtils.isBlank("text"));
        assertFalse(RuntimeUtils.isBlank("  text  "));
    }

    @Test
    void testIsEmptyCollection() {
        assertTrue(RuntimeUtils.isEmpty(Collections.emptyList()));
        assertFalse(RuntimeUtils.isEmpty(Collections.singletonList("item")));
        List<String> list = new ArrayList<>();
        assertTrue(RuntimeUtils.isEmpty(list));
        list.add("item");
        assertFalse(RuntimeUtils.isEmpty(list));
    }

    @Test
    void testIsEmptyMap() {
        assertTrue(RuntimeUtils.isEmpty(Collections.emptyMap()));
        assertFalse(RuntimeUtils.isEmpty(Collections.singletonMap("key", "value")));
        Map<String, String> map = new HashMap<>();
        assertTrue(RuntimeUtils.isEmpty(map));
        map.put("key", "value");
        assertFalse(RuntimeUtils.isEmpty(map));
    }

    @Test
    void testNotNull() {
        RuntimeUtils.notNull("value", "message");
        assertThrows(IllegalArgumentException.class, () -> {
            RuntimeUtils.notNull(null, "message");
        });
    }

    @Test
    void testRequireText() {
        RuntimeUtils.requireText("text", "message");
        RuntimeUtils.requireText("  text  ", "message");
        assertThrows(IllegalArgumentException.class, () -> {
            RuntimeUtils.requireText(null, "message");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            RuntimeUtils.requireText("", "message");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            RuntimeUtils.requireText("   ", "message");
        });
    }
}
