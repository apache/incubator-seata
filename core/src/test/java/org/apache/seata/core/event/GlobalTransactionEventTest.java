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
package org.apache.seata.core.event;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The GlobalTransactionEvent Test
 */
public class GlobalTransactionEventTest {

    private static GlobalTransactionEvent event ;

    @BeforeAll
    public static void setUp() {
        event = new GlobalTransactionEvent(123456789L, "tc", "EventName", "AppID", "Group1", 123456789L, 1234567890L, "committed", true,false);
    }
    @Test
    public void testGetId() {
        // Test the getId method
        assertEquals(123456789L, event.getId());
    }

    @Test
    public void testGetRole() {
        // Test the getRole method
        assertEquals("tc", event.getRole());
    }

    @Test
    public void testGetName() {
        // Test the getName method
        assertEquals("EventName", event.getName());
    }

    @Test
    public void testGetApplicationId() {
        // Test the getApplicationId method
        assertEquals("AppID", event.getApplicationId());
    }

    @Test
    public void testGetGroup() {
        // Test the getGroup method
        assertEquals("Group1", event.getGroup());
    }

    @Test
    public void testGetBeginTime() {
        // Test the getBeginTime method
        assertEquals(123456789L, event.getBeginTime().longValue());
    }

    @Test
    public void testGetEndTime() {
        // Test the getEndTime method
        assertEquals(1234567890L, event.getEndTime().longValue());
    }

    @Test
    public void testGetStatus() {
        // Test the getStatus method
        assertEquals("committed", event.getStatus());
    }

    @Test
    public void testIsRetryGlobal() {
        // Test the isSuccess method
        assertTrue(event.isRetryGlobal());
    }


    @Test
    public void testIsRetryBranch() {
        // Test the isSuccess method
        assertFalse(event.isRetryBranch());
    }

}
