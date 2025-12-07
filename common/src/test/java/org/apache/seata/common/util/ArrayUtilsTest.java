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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ArrayUtilsTest {

    @Test
    public void testToArray() {
        Assertions.assertNull(ArrayUtils.toArray(null));

        Object obj = new String[] {"1", "2", "3"};
        Object[] array = ArrayUtils.toArray(obj);
        Assertions.assertArrayEquals(new String[] {"1", "2", "3"}, array);

        Object obj1 = new String[] {};
        Object[] array1 = ArrayUtils.toArray(obj1);
        Assertions.assertArrayEquals(new String[] {}, array1);
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
        Assertions.assertEquals("[]", ArrayUtils.toString(new Object[] {}));
        Assertions.assertEquals("[\"1\", \"2\", \"3\"]", ArrayUtils.toString(new String[] {"1", "2", "3"}));

        Assertions.assertEquals("null", ArrayUtils.toString((Object) null));
        Assertions.assertEquals("[]", ArrayUtils.toString((Object) new Object[] {}));
        Assertions.assertEquals("123", ArrayUtils.toString(123));
        Assertions.assertEquals("[1, 2, 3]", ArrayUtils.toString((Object) new int[] {1, 2, 3}));
        Assertions.assertEquals("[\"1\", \"2\", \"3\"]", ArrayUtils.toString((Object) new String[] {"1", "2", "3"}));
    }

    @Test
    public void testToArrayWithNull() {
        Object[] result = ArrayUtils.toArray(null);
        assertThat(result).isNull();
    }

    @Test
    public void testToArrayWithEmptyArray() {
        Object[] emptyArray = new Object[0];
        Object[] result = ArrayUtils.toArray(emptyArray);
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        assertThat(result.length).isEqualTo(0);
    }

    @Test
    public void testToArrayWithStringArray() {
        String[] stringArray = {"a", "b", "c"};
        Object[] result = ArrayUtils.toArray((Object) stringArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    public void testToArrayWithPrimitiveArray() {
        int[] intArray = {1, 2, 3};
        Object[] result = ArrayUtils.toArray((Object) intArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    public void testToArrayWithDoubleArray() {
        double[] doubleArray = {1.1, 2.2, 3.3};
        Object[] result = ArrayUtils.toArray((Object) doubleArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1.1, 2.2, 3.3);
    }

    @Test
    public void testToArrayWithBooleanArray() {
        boolean[] booleanArray = {true, false, true};
        Object[] result = ArrayUtils.toArray((Object) booleanArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(true, false, true);
    }

    @Test
    public void testToArrayWithCharArray() {
        char[] charArray = {'a', 'b', 'c'};
        Object[] result = ArrayUtils.toArray((Object) charArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly('a', 'b', 'c');
    }

    @Test
    public void testToArrayWithByteArray() {
        byte[] byteArray = {1, 2, 3};
        Object[] result = ArrayUtils.toArray((Object) byteArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly((byte) 1, (byte) 2, (byte) 3);
    }

    @Test
    public void testToArrayWithLongArray() {
        long[] longArray = {1L, 2L, 3L};
        Object[] result = ArrayUtils.toArray((Object) longArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1L, 2L, 3L);
    }

    @Test
    public void testToArrayWithFloatArray() {
        float[] floatArray = {1.1f, 2.2f, 3.3f};
        Object[] result = ArrayUtils.toArray((Object) floatArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly(1.1f, 2.2f, 3.3f);
    }

    @Test
    public void testToArrayWithShortArray() {
        short[] shortArray = {1, 2, 3};
        Object[] result = ArrayUtils.toArray((Object) shortArray);
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).containsExactly((short) 1, (short) 2, (short) 3);
    }

    @Test
    public void testToArrayWithNonArrayObject() {
        assertThatThrownBy(() -> ArrayUtils.toArray("not an array"))
                .isInstanceOf(ClassCastException.class)
                .hasMessage("'arrayObj' is not an array, can't cast to Object[]");

        assertThatThrownBy(() -> ArrayUtils.toArray(new Object()))
                .isInstanceOf(ClassCastException.class)
                .hasMessage("'arrayObj' is not an array, can't cast to Object[]");

        assertThatThrownBy(() -> ArrayUtils.toArray(123))
                .isInstanceOf(ClassCastException.class)
                .hasMessage("'arrayObj' is not an array, can't cast to Object[]");
    }

    @Test
    public void testToStringWithNullArray() {
        String result = ArrayUtils.toString((Object[]) null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    public void testToStringWithEmptyArray() {
        String result = ArrayUtils.toString(new Object[0]);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void testToStringWithStringArray() {
        String result = ArrayUtils.toString(new String[] {"a", "b", "c"});
        assertThat(result).isEqualTo("[\"a\", \"b\", \"c\"]");
    }

    @Test
    public void testToStringWithIntegerArray() {
        String result = ArrayUtils.toString(new Integer[] {1, 2, 3});
        assertThat(result).isEqualTo("[1, 2, 3]");
    }

    @Test
    public void testToStringWithMixedTypeArray() {
        String result = ArrayUtils.toString(new Object[] {"a", 1, true});
        assertThat(result).isEqualTo("[\"a\", 1, true]");
    }

    @Test
    public void testToStringWithNullElements() {
        String result = ArrayUtils.toString(new Object[] {null, "a", null});
        assertThat(result).isEqualTo("[null, \"a\", null]");
    }

    @Test
    public void testToStringWithRecursiveArray() {
        Object[] recursiveArray = new Object[2];
        recursiveArray[0] = "element";
        recursiveArray[1] = recursiveArray; // self-reference

        String result = ArrayUtils.toString(recursiveArray);
        assertThat(result).contains("element");
        assertThat(result).contains("this Object[]");
    }

    @Test
    public void testToStringWithNullObject() {
        String result = ArrayUtils.toString((Object) null);
        assertThat(result).isEqualTo("null");
    }

    @Test
    public void testToStringWithNonArrayObject() {
        String result = ArrayUtils.toString("not an array");
        assertThat(result).isEqualTo("\"not an array\"");

        result = ArrayUtils.toString(123);
        assertThat(result).isEqualTo("123");

        result = ArrayUtils.toString(true);
        assertThat(result).isEqualTo("true");
    }

    @Test
    public void testToStringWithEmptyObjectArray() {
        String result = ArrayUtils.toString((Object) new Object[0]);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void testToStringWithEmptyPrimitiveArray() {
        String result = ArrayUtils.toString((Object) new int[0]);
        assertThat(result).isEqualTo("[]");
    }

    @Test
    public void testToStringWithPrimitiveIntArray() {
        String result = ArrayUtils.toString((Object) new int[] {1, 2, 3});
        assertThat(result).isEqualTo("[1, 2, 3]");
    }

    @Test
    public void testToStringWithPrimitiveDoubleArray() {
        String result = ArrayUtils.toString((Object) new double[] {1.1, 2.2, 3.3});
        assertThat(result).isEqualTo("[1.1, 2.2, 3.3]");
    }

    @Test
    public void testToStringWithPrimitiveBooleanArray() {
        String result = ArrayUtils.toString((Object) new boolean[] {true, false, true});
        assertThat(result).isEqualTo("[true, false, true]");
    }

    @Test
    public void testToStringWithPrimitiveCharArray() {
        String result = ArrayUtils.toString((Object) new char[] {'a', 'b', 'c'});
        assertThat(result).isEqualTo("['a', 'b', 'c']");
    }

    @Test
    public void testToStringWithComplexObjectArray() {
        Object[] complexArray = new Object[] {new Object[] {"nested", "array"}, 42, "string"};
        String result = ArrayUtils.toString(complexArray);
        assertThat(result).contains("nested");
        assertThat(result).contains("array");
        assertThat(result).contains("42");
        assertThat(result).contains("string");
    }
}
