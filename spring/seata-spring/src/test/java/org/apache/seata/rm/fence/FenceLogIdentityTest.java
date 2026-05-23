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
package org.apache.seata.rm.fence;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for FenceLogIdentity
 * Tests the inner class FenceLogIdentity of SpringFenceHandler
 * This test class is completely isolated and safe from static state pollution
 */
public class FenceLogIdentityTest {

    private Object fenceLogIdentity;
    private Class<?> fenceLogIdentityClass;

    @BeforeEach
    public void setUp() throws Exception {
        // Get inner class
        Class<?>[] innerClasses = SpringFenceHandler.class.getDeclaredClasses();
        for (Class<?> innerClass : innerClasses) {
            if ("FenceLogIdentity".equals(innerClass.getSimpleName())) {
                fenceLogIdentityClass = innerClass;
                break;
            }
        }

        assertNotNull(fenceLogIdentityClass, "FenceLogIdentity inner class should exist");

        // Create an instance
        Constructor<?> constructor = fenceLogIdentityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        fenceLogIdentity = constructor.newInstance();
    }

    @Test
    public void testDefaultConstructor() throws Exception {
        // Given & When
        Constructor<?> constructor = fenceLogIdentityClass.getDeclaredConstructor();
        constructor.setAccessible(true);
        Object instance = constructor.newInstance();

        // Then
        assertNotNull(instance);
    }

    @Test
    public void testGetXidInitiallyNull() throws Exception {
        // Given
        java.lang.reflect.Method getXidMethod = fenceLogIdentityClass.getDeclaredMethod("getXid");
        getXidMethod.setAccessible(true);

        // When
        String xid = (String) getXidMethod.invoke(fenceLogIdentity);

        // Then
        assertNull(xid);
    }

    @Test
    public void testGetBranchIdInitiallyNull() throws Exception {
        // Given
        java.lang.reflect.Method getBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("getBranchId");
        getBranchIdMethod.setAccessible(true);

        // When
        Long branchId = (Long) getBranchIdMethod.invoke(fenceLogIdentity);

        // Then
        assertNull(branchId);
    }

    @Test
    public void testSetAndGetXid() throws Exception {
        // Given
        String testXid = "test-xid-123";
        java.lang.reflect.Method setXidMethod = fenceLogIdentityClass.getDeclaredMethod("setXid", String.class);
        java.lang.reflect.Method getXidMethod = fenceLogIdentityClass.getDeclaredMethod("getXid");
        setXidMethod.setAccessible(true);
        getXidMethod.setAccessible(true);

        // When
        setXidMethod.invoke(fenceLogIdentity, testXid);
        String result = (String) getXidMethod.invoke(fenceLogIdentity);

        // Then
        assertEquals(testXid, result);
    }

    @Test
    public void testSetAndGetBranchId() throws Exception {
        // Given
        Long testBranchId = 123456L;
        java.lang.reflect.Method setBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("setBranchId", Long.class);
        java.lang.reflect.Method getBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("getBranchId");
        setBranchIdMethod.setAccessible(true);
        getBranchIdMethod.setAccessible(true);

        // When
        setBranchIdMethod.invoke(fenceLogIdentity, testBranchId);
        Long result = (Long) getBranchIdMethod.invoke(fenceLogIdentity);

        // Then
        assertEquals(testBranchId, result);
    }

    @Test
    public void testSetXidToNull() throws Exception {
        // Given
        java.lang.reflect.Method setXidMethod = fenceLogIdentityClass.getDeclaredMethod("setXid", String.class);
        java.lang.reflect.Method getXidMethod = fenceLogIdentityClass.getDeclaredMethod("getXid");
        setXidMethod.setAccessible(true);
        getXidMethod.setAccessible(true);

        // When
        setXidMethod.invoke(fenceLogIdentity, (String) null);
        String result = (String) getXidMethod.invoke(fenceLogIdentity);

        // Then
        assertNull(result);
    }

    @Test
    public void testSetBranchIdToNull() throws Exception {
        // Given
        java.lang.reflect.Method setBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("setBranchId", Long.class);
        java.lang.reflect.Method getBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("getBranchId");
        setBranchIdMethod.setAccessible(true);
        getBranchIdMethod.setAccessible(true);

        // When
        setBranchIdMethod.invoke(fenceLogIdentity, (Long) null);
        Long result = (Long) getBranchIdMethod.invoke(fenceLogIdentity);

        // Then
        assertNull(result);
    }

    @Test
    public void testSetAndGetMultipleValues() throws Exception {
        // Given
        String testXid1 = "test-xid-1";
        String testXid2 = "test-xid-2";
        Long testBranchId1 = 111L;
        Long testBranchId2 = 222L;

        java.lang.reflect.Method setXidMethod = fenceLogIdentityClass.getDeclaredMethod("setXid", String.class);
        java.lang.reflect.Method getXidMethod = fenceLogIdentityClass.getDeclaredMethod("getXid");
        java.lang.reflect.Method setBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("setBranchId", Long.class);
        java.lang.reflect.Method getBranchIdMethod = fenceLogIdentityClass.getDeclaredMethod("getBranchId");

        setXidMethod.setAccessible(true);
        getXidMethod.setAccessible(true);
        setBranchIdMethod.setAccessible(true);
        getBranchIdMethod.setAccessible(true);

        // When & Then - Set first group of values
        setXidMethod.invoke(fenceLogIdentity, testXid1);
        setBranchIdMethod.invoke(fenceLogIdentity, testBranchId1);

        assertEquals(testXid1, getXidMethod.invoke(fenceLogIdentity));
        assertEquals(testBranchId1, getBranchIdMethod.invoke(fenceLogIdentity));

        // When & Then - Set second group of values
        setXidMethod.invoke(fenceLogIdentity, testXid2);
        setBranchIdMethod.invoke(fenceLogIdentity, testBranchId2);

        assertEquals(testXid2, getXidMethod.invoke(fenceLogIdentity));
        assertEquals(testBranchId2, getBranchIdMethod.invoke(fenceLogIdentity));
    }
}
