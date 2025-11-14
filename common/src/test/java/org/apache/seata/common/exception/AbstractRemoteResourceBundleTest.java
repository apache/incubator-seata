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

package org.apache.seata.common.exception;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.MissingResourceException;

class AbstractRemoteResourceBundleTest {

    private static class TestResourceBundle extends AbstractRemoteResourceBundle {

        @Override
        protected Object handleGetObject(@NotNull String key) {
            return null;
        }

        @NotNull
        @Override
        public Enumeration<String> getKeys() {
            return new Enumeration<String>() {
                private final String[] keys = {"key1", "key2", "key3"};
                private int index = 0;

                @Override
                public boolean hasMoreElements() {
                    return index < keys.length;
                }

                @Override
                public String nextElement() {
                    if (index >= keys.length) {
                        throw new IllegalStateException("No more elements");
                    }
                    return keys[index++];
                }
            };
        }
    }

    @Test
    void testGetString_valueIsNull() {
        TestResourceBundle bundle = new TestResourceBundle();
        Assertions.assertThrows(MissingResourceException.class, () -> bundle.getString("key1"));
    }

    @Test
    void testGetString_keyNotFound() {
        TestResourceBundle bundle = new TestResourceBundle();
        Assertions.assertThrows(MissingResourceException.class, () -> bundle.getString("nonexistent"));
    }

    @Test
    void testGetObject_valueIsNull() {
        TestResourceBundle bundle = new TestResourceBundle();
        Assertions.assertThrows(MissingResourceException.class, () -> bundle.getObject("key2"));
    }

    @Test
    void testGetObject_keyNotFound() {
        TestResourceBundle bundle = new TestResourceBundle();
        Assertions.assertThrows(MissingResourceException.class, () -> bundle.getObject("missing"));
    }

    @Test
    void testGetKeys_normal() {
        TestResourceBundle bundle = new TestResourceBundle();
        Enumeration<String> keys = bundle.getKeys();

        Assertions.assertNotNull(keys);
        Assertions.assertTrue(keys.hasMoreElements());

        ArrayList<String> actual = new ArrayList<>();
        while (keys.hasMoreElements()) {
            actual.add(keys.nextElement());
        }

        Assertions.assertEquals(java.util.Arrays.asList("key1", "key2", "key3"), actual);
    }

    @Test
    void testGetKeys_empty() {
        AbstractRemoteResourceBundle emptyBundle = new AbstractRemoteResourceBundle() {
            @Override
            protected Object handleGetObject(@NotNull String key) {
                return null;
            }

            @NotNull
            @Override
            public Enumeration<String> getKeys() {
                return Collections.emptyEnumeration();
            }
        };

        Enumeration<String> keys = emptyBundle.getKeys();
        Assertions.assertNotNull(keys);
        Assertions.assertFalse(keys.hasMoreElements());
    }

    @Test
    void testContainsKey_keyAbsent() {
        TestResourceBundle bundle = new TestResourceBundle();
        Assertions.assertFalse(bundle.containsKey("key1"));
    }

    @Test
    void testToString_default() {
        TestResourceBundle bundle = new TestResourceBundle();
        Assertions.assertNotNull(bundle.toString());
    }
}
