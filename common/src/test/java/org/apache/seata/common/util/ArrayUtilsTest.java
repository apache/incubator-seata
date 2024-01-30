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


public class ArrayUtilsTest {

    @Test
    public void testToArray() {
        Assertions.assertNull(ArrayUtils.toArray(null));

        Object obj = new String[]{"1", "2", "3"};
        Object[] array = ArrayUtils.toArray(obj);
        Assertions.assertArrayEquals(new String[]{"1", "2", "3"}, array);

        Object obj1 = new String[]{};
        Object[] array1 = ArrayUtils.toArray(obj1);
        Assertions.assertArrayEquals(new String[]{}, array1);
    }

    @Test
    public void testToArrayException() {
        Assertions.assertThrows(ClassCastException.class, () -> {
            Object[] array = ArrayUtils.toArray(new Object());
        });
    }

    @Test
    public void testToString() {
        Assertions.assertEquals("null", ArrayUtils.toString((Object[]) null));
        Assertions.assertEquals("[]", ArrayUtils.toString(new Object[]{}));
        Assertions.assertEquals("[\"1\", \"2\", \"3\"]", ArrayUtils.toString(new String[]{"1", "2", "3"}));

        Assertions.assertEquals("null", ArrayUtils.toString((Object) null));
        Assertions.assertEquals("[]", ArrayUtils.toString((Object) new Object[]{}));
        Assertions.assertEquals("123", ArrayUtils.toString(123));
        Assertions.assertEquals("[1, 2, 3]", ArrayUtils.toString((Object) new int[]{1, 2, 3}));
        Assertions.assertEquals("[\"1\", \"2\", \"3\"]", ArrayUtils.toString((Object) new String[]{"1", "2", "3"}));
    }
}
