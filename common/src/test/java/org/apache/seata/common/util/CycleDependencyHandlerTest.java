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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CycleDependencyHandlerTest {

    @Test
    public void testContainsObject() {
        Assertions.assertFalse(CycleDependencyHandler.containsObject(null));
    }

    @Test
    public void testIsStarting() {
        // Initially not starting
        Assertions.assertFalse(CycleDependencyHandler.isStarting());

        // After start, should be starting
        CycleDependencyHandler.start();
        Assertions.assertTrue(CycleDependencyHandler.isStarting());

        // After end, should not be starting
        CycleDependencyHandler.end();
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
    }

    @Test
    public void testAddObject() {
        CycleDependencyHandler.start();

        try {
            // Add null object - should not throw exception
            CycleDependencyHandler.addObject(null);

            // Add non-null object
            Object obj = new Object();
            CycleDependencyHandler.addObject(obj);

            // Check if object is contained
            Assertions.assertTrue(CycleDependencyHandler.containsObject(obj));
        } finally {
            CycleDependencyHandler.end();
        }
    }

    @Test
    public void testContainsObjectWhenNotStarted() {
        // When not started, should return false for any object
        Assertions.assertFalse(CycleDependencyHandler.containsObject(new Object()));
    }

    @Test
    public void testToRefString() {
        Object obj = new Object();
        String refString = CycleDependencyHandler.toRefString(obj);
        Assertions.assertTrue(refString.contains("ref"));
        Assertions.assertTrue(refString.contains("Object"));
    }

    @Test
    public void testWrap() {
        Object obj = new Object();

        String result = CycleDependencyHandler.wrap(obj, (o) -> {
            return "test";
        });

        Assertions.assertEquals("test", result);
    }

    @Test
    public void testWrapWithCycle() {
        CycleDependencyHandler.start();
        try {
            Object obj = new Object();

            // Add object to simulate it's already been processed
            CycleDependencyHandler.addObject(obj);

            // Now wrap should detect cycle and return ref string
            String result = CycleDependencyHandler.wrap(obj, (o) -> {
                return "should not reach here";
            });

            Assertions.assertTrue(result.contains("ref"));
            Assertions.assertTrue(result.contains("Object"));
        } finally {
            CycleDependencyHandler.end();
        }
    }
}
