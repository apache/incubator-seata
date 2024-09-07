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
package org.apache.seata.integration.tx.api.util;

import java.lang.reflect.Method;
import java.util.AbstractList;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class ClassUtilsTest {

    @Test
    public void testGetPackageName() {
        String packageName = ClassUtils.getPackageName(AbstractMap.class);
        assertEquals("java.util", packageName);
    }

    @Test
    public void testGetPackageNameForSimpleName() {
        String packageName = ClassUtils.getPackageName(AbstractMap.class.getSimpleName());
        assertEquals("", packageName);
    }

    @Test
    public void testGetMostSpecificMethodWhenClassIsNull() throws NoSuchMethodException {
        Method method = AbstractMap.class.getDeclaredMethod("clone");
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, null);
        assertEquals(AbstractMap.class, specificMethod.getDeclaringClass());
    }

    @Test
    public void testGetMostSpecificMethodFromSameClass() throws NoSuchMethodException {
        Method method = AbstractMap.class.getDeclaredMethod("clone");
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, AbstractMap.class);
        assertEquals(AbstractMap.class, specificMethod.getDeclaringClass());
    }

    @Test
    public void testGetMostSpecificNotPublicMethod() throws NoSuchMethodException {
        Method method = AbstractMap.class.getDeclaredMethod("clone");
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, HashMap.class);
        assertNotEquals(HashMap.class.getDeclaredMethod("clone"), specificMethod);
    }

    @Test
    public void testGetMostSpecificPublicMethod() throws NoSuchMethodException {
        Method method = Map.class.getDeclaredMethod("remove", Object.class, Object.class);
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, HashMap.class);
        assertEquals(HashMap.class.getDeclaredMethod("remove", Object.class, Object.class), specificMethod);
    }

    @Test
    public void testGetMostSpecificPrivateMethod() throws NoSuchMethodException {
        Method method = AbstractList.class.getDeclaredMethod("rangeCheckForAdd", int.class);
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, ArrayList.class);
        assertNotEquals(ArrayList.class.getDeclaredMethod("rangeCheckForAdd", int.class), specificMethod);
    }

    @Test
    public void testGetMostSpecificMethodWhichNotExistsInTargetClass() throws NoSuchMethodException {
        Method method = ArrayList.class.getDeclaredMethod("sort", Comparator.class);
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, Map.class);
        assertEquals(ArrayList.class.getDeclaredMethod("sort", Comparator.class), specificMethod);
    }
}
