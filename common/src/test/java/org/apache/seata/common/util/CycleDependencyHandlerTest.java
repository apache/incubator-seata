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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CycleDependencyHandlerTest {

    @AfterEach
    public void tearDown() {
        CycleDependencyHandler.end();
    }

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

    @Test
    public void testWrapPropagatesFailureAndAllowsSameThreadReuse() {
        Object obj = new Object();
        IllegalStateException failure = new IllegalStateException("Conversion failed");

        IllegalStateException thrown = Assertions.assertThrows(
                IllegalStateException.class,
                () -> CycleDependencyHandler.wrap(obj, ignored -> {
                    throw failure;
                }));

        Assertions.assertSame(failure, thrown);
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
        Assertions.assertFalse(CycleDependencyHandler.containsObject(obj));

        AtomicInteger invocations = new AtomicInteger();
        String result = CycleDependencyHandler.wrap(obj, current -> {
            Assertions.assertSame(obj, current);
            invocations.incrementAndGet();
            return "converted";
        });

        Assertions.assertEquals("converted", result);
        Assertions.assertEquals(1, invocations.get());
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
    }

    @Test
    public void testNestedWrapSuccessPreservesOwnerState() {
        Object owner = new Object();
        Object nested = new Object();
        CycleDependencyHandler.start();
        CycleDependencyHandler.addObject(owner);

        try {
            String result = CycleDependencyHandler.wrap(nested, current -> {
                Assertions.assertSame(nested, current);
                Assertions.assertTrue(CycleDependencyHandler.containsObject(owner));
                Assertions.assertTrue(CycleDependencyHandler.containsObject(nested));
                return "nested";
            });

            Assertions.assertEquals("nested", result);
            Assertions.assertTrue(CycleDependencyHandler.isStarting());
            Assertions.assertTrue(CycleDependencyHandler.containsObject(owner));
            Assertions.assertTrue(CycleDependencyHandler.containsObject(nested));
        } finally {
            CycleDependencyHandler.end();
        }
    }

    @Test
    public void testNestedWrapFailurePreservesOwnerState() {
        Object owner = new Object();
        Object nested = new Object();
        IllegalStateException failure = new IllegalStateException("Nested conversion failed");
        CycleDependencyHandler.start();
        CycleDependencyHandler.addObject(owner);

        try {
            IllegalStateException thrown = Assertions.assertThrows(
                    IllegalStateException.class,
                    () -> CycleDependencyHandler.wrap(nested, ignored -> {
                        throw failure;
                    }));

            Assertions.assertSame(failure, thrown);
            Assertions.assertTrue(CycleDependencyHandler.isStarting());
            Assertions.assertTrue(CycleDependencyHandler.containsObject(owner));
            Assertions.assertTrue(CycleDependencyHandler.containsObject(nested));

            Assertions.assertEquals("another", CycleDependencyHandler.wrap(new Object(), ignored -> "another"));
            Assertions.assertTrue(CycleDependencyHandler.containsObject(owner));
        } finally {
            CycleDependencyHandler.end();
        }
    }

    @Test
    public void testRecursiveWrapSkipsCallbackAndReturnsReference() {
        Object obj = new Object();
        AtomicInteger invocations = new AtomicInteger();

        String result = CycleDependencyHandler.wrap(obj, current -> {
            invocations.incrementAndGet();
            return CycleDependencyHandler.wrap(current, ignored -> {
                invocations.incrementAndGet();
                return "unexpected";
            });
        });

        Assertions.assertEquals("(ref Object)", result);
        Assertions.assertEquals(1, invocations.get());
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
        Assertions.assertFalse(CycleDependencyHandler.containsObject(obj));
    }

    @Test
    public void testWrapStateIsIndependentBetweenThreads() throws Exception {
        Object shared = new Object();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CycleDependencyHandler.start();
        CycleDependencyHandler.addObject(shared);

        try {
            Future<String> result = executor.submit(() -> {
                try {
                    Assertions.assertFalse(CycleDependencyHandler.isStarting());
                    Assertions.assertFalse(CycleDependencyHandler.containsObject(shared));

                    String converted = CycleDependencyHandler.wrap(shared, current -> {
                        Assertions.assertSame(shared, current);
                        Assertions.assertTrue(CycleDependencyHandler.isStarting());
                        Assertions.assertTrue(CycleDependencyHandler.containsObject(shared));
                        return "worker";
                    });

                    Assertions.assertFalse(CycleDependencyHandler.isStarting());
                    Assertions.assertFalse(CycleDependencyHandler.containsObject(shared));
                    return converted;
                } finally {
                    CycleDependencyHandler.end();
                }
            });

            Assertions.assertEquals("worker", result.get(5, TimeUnit.SECONDS));
            Assertions.assertTrue(CycleDependencyHandler.isStarting());
            Assertions.assertTrue(CycleDependencyHandler.containsObject(shared));
        } finally {
            CycleDependencyHandler.end();
            executor.shutdownNow();
            Assertions.assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    public void testIdentityTrackingDoesNotCallObjectHashCodeOrEquals() {
        UnsafeHashObject obj = new UnsafeHashObject();

        String result = CycleDependencyHandler.wrap(obj, current -> {
            Assertions.assertTrue(CycleDependencyHandler.containsObject(current));
            return CycleDependencyHandler.wrap(current, ignored -> "unexpected");
        });

        Assertions.assertEquals("(ref UnsafeHashObject)", result);
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
    }

    private static class UnsafeHashObject {

        @Override
        public int hashCode() {
            throw new AssertionError("Object hashCode must not be called");
        }

        @Override
        public boolean equals(Object other) {
            throw new AssertionError("Object equals must not be called");
        }
    }
}
