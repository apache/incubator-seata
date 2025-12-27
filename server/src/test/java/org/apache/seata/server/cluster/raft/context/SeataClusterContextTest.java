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
package org.apache.seata.server.cluster.raft.context;

import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for SeataClusterContext covering context management logic.
 */
public class SeataClusterContextTest extends BaseSpringBootTest {

    @AfterEach
    public void cleanup() {
        // Clean up context after each test
        SeataClusterContext.unbindGroup();
    }

    @Test
    public void testBindGroupWithCustomValue() {
        String customGroup = "custom-group";
        SeataClusterContext.bindGroup(customGroup);

        String retrievedGroup = SeataClusterContext.getGroup();
        assertEquals(customGroup, retrievedGroup);
    }

    @Test
    public void testBindGroupWithDefaultValue() {
        String defaultGroup = SeataClusterContext.bindGroup();

        assertNotNull(defaultGroup);
        assertEquals(defaultGroup, SeataClusterContext.getGroup());
    }

    @Test
    public void testBindGroupReturnsDefaultGroup() {
        String group = SeataClusterContext.bindGroup();

        // Default group should be "default" based on DEFAULT_SEATA_GROUP
        assertNotNull(group);
        assertEquals("default", group);
    }

    @Test
    public void testUnbindGroupRemovesContext() {
        SeataClusterContext.bindGroup("test-group");
        assertEquals("test-group", SeataClusterContext.getGroup());

        SeataClusterContext.unbindGroup();
        assertNull(SeataClusterContext.getGroup());
    }

    @Test
    public void testGetGroupReturnsNullWhenNotBound() {
        assertNull(SeataClusterContext.getGroup());
    }

    @Test
    public void testBindGroupOverwritesPreviousValue() {
        SeataClusterContext.bindGroup("first-group");
        assertEquals("first-group", SeataClusterContext.getGroup());

        SeataClusterContext.bindGroup("second-group");
        assertEquals("second-group", SeataClusterContext.getGroup());
    }

    @Test
    public void testBindCustomGroupThenBindDefault() {
        SeataClusterContext.bindGroup("custom-group");
        assertEquals("custom-group", SeataClusterContext.getGroup());

        String defaultGroup = SeataClusterContext.bindGroup();
        assertEquals(defaultGroup, SeataClusterContext.getGroup());
        assertEquals("default", defaultGroup);
    }

    @Test
    public void testBindDefaultGroupThenBindCustom() {
        SeataClusterContext.bindGroup();
        assertEquals("default", SeataClusterContext.getGroup());

        SeataClusterContext.bindGroup("custom-group");
        assertEquals("custom-group", SeataClusterContext.getGroup());
    }

    @Test
    public void testUnbindMultipleTimes() {
        SeataClusterContext.bindGroup("test-group");

        SeataClusterContext.unbindGroup();
        assertNull(SeataClusterContext.getGroup());

        // Unbinding again should not throw exception
        SeataClusterContext.unbindGroup();
        assertNull(SeataClusterContext.getGroup());
    }

    @Test
    public void testBindEmptyString() {
        SeataClusterContext.bindGroup("");
        assertEquals("", SeataClusterContext.getGroup());
    }

    @Test
    public void testBindGroupWithWhitespace() {
        SeataClusterContext.bindGroup("  group-with-spaces  ");
        assertEquals("  group-with-spaces  ", SeataClusterContext.getGroup());
    }

    @Test
    public void testContextIsolationBetweenOperations() {
        // First operation
        SeataClusterContext.bindGroup("operation1");
        String group1 = SeataClusterContext.getGroup();
        assertEquals("operation1", group1);
        SeataClusterContext.unbindGroup();

        // Second operation should start clean
        assertNull(SeataClusterContext.getGroup());
        SeataClusterContext.bindGroup("operation2");
        String group2 = SeataClusterContext.getGroup();
        assertEquals("operation2", group2);
    }

    @Test
    public void testKeyGroupConstant() {
        assertEquals("TX_GROUP", SeataClusterContext.KEY_GROUP);
    }

    @Test
    public void testBindGroupWithSpecialCharacters() {
        String specialGroup = "group-with-@#$%^&*()";
        SeataClusterContext.bindGroup(specialGroup);
        assertEquals(specialGroup, SeataClusterContext.getGroup());
    }

    @Test
    public void testBindGroupWithVeryLongName() {
        char[] chars = new char[100];
        Arrays.fill(chars, 'a');
        String longGroup = "very-long-group-name-" + new String(chars);
        SeataClusterContext.bindGroup(longGroup);
        assertEquals(longGroup, SeataClusterContext.getGroup());
    }
}
