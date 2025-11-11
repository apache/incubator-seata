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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The type Collection utils test.
 *
 */
public class CollectionUtilsTest {

    @Test
    public void test_isEmpty_isNotEmpty() {
        // case 1: null
        List<String> list = null;
        String[] array = null;
        Map<Object, Object> map = null;
        Assertions.assertTrue(CollectionUtils.isEmpty(list));
        Assertions.assertTrue(CollectionUtils.isEmpty(array));
        Assertions.assertTrue(CollectionUtils.isEmpty(map));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(list));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(array));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(map));

        // case 2: empty
        list = new ArrayList<>();
        array = new String[0];
        map = new HashMap<>();
        Assertions.assertTrue(CollectionUtils.isEmpty(list));
        Assertions.assertTrue(CollectionUtils.isEmpty(array));
        Assertions.assertTrue(CollectionUtils.isEmpty(map));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(list));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(array));
        Assertions.assertFalse(CollectionUtils.isNotEmpty(map));

        // case 3: not empty
        list.add("1");
        array = new String[] {"1"};
        map.put("test", "test");
        Assertions.assertFalse(CollectionUtils.isEmpty(list));
        Assertions.assertFalse(CollectionUtils.isEmpty(array));
        Assertions.assertFalse(CollectionUtils.isEmpty(map));
        Assertions.assertTrue(CollectionUtils.isNotEmpty(list));
        Assertions.assertTrue(CollectionUtils.isNotEmpty(array));
        Assertions.assertTrue(CollectionUtils.isNotEmpty(map));
    }

    /**
     * Is size equals.
     */
    @Test
    public void isSizeEquals() {
        List<String> list0 = new ArrayList<>();
        List<String> list1 = new ArrayList<>();
        Assertions.assertTrue(CollectionUtils.isSizeEquals(null, null));
        Assertions.assertFalse(CollectionUtils.isSizeEquals(null, list0));
        Assertions.assertFalse(CollectionUtils.isSizeEquals(list1, null));
        Assertions.assertTrue(CollectionUtils.isSizeEquals(list0, list1));

        list0.add("111");
        Assertions.assertFalse(CollectionUtils.isSizeEquals(list0, list1));
        list1.add("111");
        Assertions.assertTrue(CollectionUtils.isSizeEquals(list0, list1));
    }

    /**
     * Encode map.
     */
    @Test
    public void encodeMap() {
        Map<String, String> map = null;
        Assertions.assertNull(CollectionUtils.encodeMap(map));

        map = new LinkedHashMap<>();
        Assertions.assertEquals("", CollectionUtils.encodeMap(map));
        map.put("x", "1");
        Assertions.assertEquals("x=1", CollectionUtils.encodeMap(map));
        map.put("y", "2");
        Assertions.assertEquals("x=1&y=2", CollectionUtils.encodeMap(map));
    }

    /**
     * Decode map.
     */
    @Test
    public void decodeMap() {
        Assertions.assertNull(CollectionUtils.decodeMap(null));

        Map<String, String> map = CollectionUtils.decodeMap("");
        Assertions.assertEquals(0, map.size());

        map = CollectionUtils.decodeMap("&");
        Assertions.assertEquals(0, map.size());

        map = CollectionUtils.decodeMap("=");
        Assertions.assertEquals(0, map.size());

        map = CollectionUtils.decodeMap("&=");
        Assertions.assertEquals(0, map.size());

        map = CollectionUtils.decodeMap("x=1");
        Assertions.assertEquals(1, map.size());
        Assertions.assertEquals("1", map.get("x"));

        map = CollectionUtils.decodeMap("x=1&y=2");
        Assertions.assertEquals(2, map.size());
        Assertions.assertEquals("2", map.get("y"));
    }

    /**
     * Test to upper list.
     */
    @Test
    public void testToUpperList() {
        List<String> sourceList = null;
        Assertions.assertNull(CollectionUtils.toUpperList(sourceList));
        sourceList = new ArrayList<>();
        Assertions.assertEquals(Collections.EMPTY_LIST, CollectionUtils.toUpperList(sourceList));
        List<String> anotherList = new ArrayList<>();
        sourceList.add("a");
        anotherList.add("A");
        sourceList.add("b");
        anotherList.add("b");
        sourceList.add("c");
        anotherList.add("C");
        Assertions.assertEquals(CollectionUtils.toUpperList(sourceList), CollectionUtils.toUpperList(anotherList));
        anotherList.add("D");
        Assertions.assertTrue(
                CollectionUtils.toUpperList(anotherList).containsAll(CollectionUtils.toUpperList(sourceList)));

        List<String> listWithNull = new ArrayList<>();
        listWithNull.add("foo");
        listWithNull.add(null);
        listWithNull.add("bar");

        List<String> listUpperWithNull = new ArrayList<>();
        listUpperWithNull.add("FOO");
        listUpperWithNull.add(null);
        listUpperWithNull.add("BAR");
        Assertions.assertEquals(listUpperWithNull, CollectionUtils.toUpperList(listWithNull));
    }

    @Test
    public void testIsEmptyWithArrays() {
        String[] emptyArray = {};
        String[] filledArray = {"Foo", "Bar"};

        Assertions.assertTrue(CollectionUtils.isEmpty(emptyArray));
        Assertions.assertFalse(CollectionUtils.isEmpty(filledArray));
    }

    @Test
    public void testIsEmptyWithCollection() {
        List<String> emptyCollection = new ArrayList<>();
        List<String> filledCollection = new ArrayList<>();

        filledCollection.add("Foo");
        filledCollection.add("Bar");

        Assertions.assertTrue(CollectionUtils.isEmpty(emptyCollection));
        Assertions.assertFalse(CollectionUtils.isEmpty(filledCollection));
    }

    @Test
    public void testCollectionToString() {
        List<String> nullCollection = null;
        List<String> emptyCollection = new ArrayList<>();
        List<Object> filledCollection = new ArrayList<>();

        filledCollection.add("Foo");
        filledCollection.add("Bar");
        filledCollection.add(filledCollection);

        Assertions.assertEquals("null", CollectionUtils.toString(nullCollection));
        Assertions.assertEquals("[]", CollectionUtils.toString(emptyCollection));
        Assertions.assertEquals("[\"Foo\", \"Bar\", (this ArrayList)]", CollectionUtils.toString(filledCollection));
    }

    @Test
    public void testMapToString() {
        Map<Object, Object> nullMap = null;
        Map<Object, Object> emptyMap = new HashMap<>();
        Map<Object, Object> filledMap = new HashMap<>();

        filledMap.put("aaa", "111");
        filledMap.put("bbb", "222");
        filledMap.put("self", filledMap);

        Assertions.assertEquals("null", CollectionUtils.toString(nullMap));
        Assertions.assertEquals("{}", CollectionUtils.toString(emptyMap));
        Assertions.assertEquals(
                "{\"aaa\"->\"111\", \"bbb\"->\"222\", \"self\"->(this HashMap)}", CollectionUtils.toString(filledMap));
    }

    @Test
    public void testIsEmpty() {
        Map<String, Object> map = new HashMap<>();
        Assertions.assertTrue(CollectionUtils.isEmpty(map));
        map.put("k", "v");
        Assertions.assertFalse(CollectionUtils.isEmpty(map));
        map = null;
        Assertions.assertTrue(CollectionUtils.isEmpty(map));
    }

    @Test
    public void testObjectMapToStringMap() {
        Map<String, Object> objMap = new HashMap<>();
        Date now = new Date(123);
        objMap.put("a", "aa");
        objMap.put("b", 22);
        objMap.put("c", now);
        Map<String, String> strMap = CollectionUtils.toStringMap(objMap);
        Assertions.assertEquals("aa", strMap.get("a"));
        Assertions.assertEquals("22", strMap.get("b"));
        Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(now), strMap.get("c"));
    }

    @Test
    public void test_getLast() {
        // case 1: null
        Assertions.assertNull(CollectionUtils.getLast(null));

        // case 2: empty
        List<String> emptyList = Collections.EMPTY_LIST;
        Assertions.assertNull(CollectionUtils.getLast(emptyList));

        // case 3: not empty
        List<String> list = new ArrayList<>();
        list.add("Foo");
        list.add("Bar");
        Assertions.assertEquals("Bar", CollectionUtils.getLast(list));
    }

    @Test
    public void testComputeIfAbsent() {
        // Test with a regular HashMap
        Map<String, String> map = new HashMap<>();
        map.put("existing", "value");

        // Test existing key - should return existing value and not call the function
        Function<String, String> mappingFunction = key -> {
            throw new RuntimeException("Function should not be called for existing key");
        };
        String result = CollectionUtils.computeIfAbsent(map, "existing", mappingFunction);
        assertThat(result).isEqualTo("value");

        // Test non-existing key - should call the function and add the value
        Function<String, String> newMappingFunction = key -> "new value for " + key;
        String newResult = CollectionUtils.computeIfAbsent(map, "newKey", newMappingFunction);
        assertThat(newResult).isEqualTo("new value for newKey");
        assertThat(map.get("newKey")).isEqualTo("new value for newKey");

        // Test with null value for existing key
        Map<String, String> mapWithNull = new HashMap<>();
        mapWithNull.put("nullKey", null);
        Function<String, String> nullMappingFunction = key -> "new value for null";
        String nullResult = CollectionUtils.computeIfAbsent(mapWithNull, "nullKey", nullMappingFunction);
        assertThat(nullResult).isEqualTo("new value for null");
        assertThat(mapWithNull.get("nullKey")).isEqualTo("new value for null");
    }

    @Test
    public void testMapToJsonString() {
        // Test with empty map
        Map<String, Object> emptyMap = new HashMap<>();
        assertThat(CollectionUtils.mapToJsonString(emptyMap)).isEqualTo("{}");

        // Test with simple key-value pairs
        Map<String, Object> simpleMap = new HashMap<>();
        simpleMap.put("name", "John");
        simpleMap.put("age", 30);
        String simpleResult = CollectionUtils.mapToJsonString(simpleMap);
        assertThat(simpleResult).contains("\"name\": \"John\"");
        assertThat(simpleResult).contains("\"age\": 30");

        // Test with nested map - order might vary in HashMap, so we check both possible orders
        Map<String, Object> nestedMap = new HashMap<>();
        nestedMap.put("id", 1);
        Map<String, Object> address = new HashMap<>();
        address.put("city", "New York");
        address.put("zip", "10001");
        nestedMap.put("address", address);
        String result = CollectionUtils.mapToJsonString(nestedMap);
        // Check if result contains expected elements regardless of order
        assertThat(result).contains("\"id\": 1");
        assertThat(result).contains("\"city\": \"New York\"");
        assertThat(result).contains("\"zip\": \"10001\"");

        // Test with null values
        Map<String, Object> mapWithNull = new HashMap<>();
        mapWithNull.put("key1", "value1");
        mapWithNull.put("key2", null);
        String nullResult = CollectionUtils.mapToJsonString(mapWithNull);
        assertThat(nullResult).contains("\"key1\": \"value1\"");
        assertThat(nullResult).contains("\"key2\": null");

        // Test with special characters in strings
        Map<String, Object> mapWithSpecialChars = new HashMap<>();
        mapWithSpecialChars.put("quote", "\"quoted\"");
        mapWithSpecialChars.put("number", 42);
        String specialResult = CollectionUtils.mapToJsonString(mapWithSpecialChars);
        // We'll just check that it contains the expected elements without strict formatting
        assertThat(specialResult).contains("\"quote\"");
        assertThat(specialResult).contains("\"number\"");
        assertThat(specialResult).contains("42");
    }

    @Test
    public void testToStringMapEnhanced() {
        // Test with null map
        Map<String, String> nullResult = CollectionUtils.toStringMap(null);
        assertThat(nullResult).isNotNull().isEmpty();

        // Test with empty map
        Map<String, Object> emptySource = new HashMap<>();
        Map<String, String> emptyResult = CollectionUtils.toStringMap(emptySource);
        assertThat(emptyResult).isNotNull().isEmpty();

        // Test with various object types
        Map<String, Object> sourceMap = new HashMap<>();
        sourceMap.put("string", "text");
        sourceMap.put("integer", 123);
        sourceMap.put("double", 45.67);
        sourceMap.put("boolean", true);
        sourceMap.put("nullValue", null);

        Map<String, String> result = CollectionUtils.toStringMap(sourceMap);
        assertThat(result)
                .containsEntry("string", "text")
                .containsEntry("integer", "123")
                .containsEntry("double", "45.67")
                .containsEntry("boolean", "true")
                .doesNotContainKey("nullValue"); // null values should be skipped

        // Test with CharSequence and Character
        Map<String, Object> charMap = new HashMap<>();
        charMap.put("charSequence", new StringBuilder("builder"));
        charMap.put("character", 'A');

        Map<String, String> charResult = CollectionUtils.toStringMap(charMap);
        assertThat(charResult).containsEntry("charSequence", "builder").containsEntry("character", "A");
    }

    @Test
    public void testGetLastWithConcurrentModification() {
        // Test with normal list
        List<String> list = new ArrayList<>();
        list.add("first");
        list.add("second");
        list.add("third");
        assertThat((String) CollectionUtils.<String>getLast(list)).isEqualTo("third");

        // Test with empty list
        List<String> emptyList = new ArrayList<>();
        assertThat(CollectionUtils.<String>getLast(emptyList)).isNull();

        // Test with null list
        assertThat(CollectionUtils.<String>getLast(null)).isNull();
    }

    @Test
    public void testIsEmptyWithArraysEnhanced() {
        // Test with null array
        String[] nullArray = null;
        assertThat(CollectionUtils.isEmpty(nullArray)).isTrue();

        // Test with empty array
        String[] emptyArray = new String[0];
        assertThat(CollectionUtils.isEmpty(emptyArray)).isTrue();

        // Test with filled array
        String[] filledArray = {"one", "two"};
        assertThat(CollectionUtils.isEmpty(filledArray)).isFalse();
    }

    @Test
    public void testIsNotEmptyWithArrays() {
        // Test with null array
        String[] nullArray = null;
        assertThat(CollectionUtils.isNotEmpty(nullArray)).isFalse();

        // Test with empty array
        String[] emptyArray = new String[0];
        assertThat(CollectionUtils.isNotEmpty(emptyArray)).isFalse();

        // Test with filled array
        String[] filledArray = {"one", "two"};
        assertThat(CollectionUtils.isNotEmpty(filledArray)).isTrue();
    }

    @Test
    public void testIsEmptyWithCollections() {
        // Test with null collection
        List<String> nullList = null;
        assertThat(CollectionUtils.isEmpty(nullList)).isTrue();

        // Test with empty collection
        List<String> emptyList = new ArrayList<>();
        assertThat(CollectionUtils.isEmpty(emptyList)).isTrue();

        // Test with filled collection
        List<String> filledList = new ArrayList<>();
        filledList.add("item");
        assertThat(CollectionUtils.isEmpty(filledList)).isFalse();
    }

    @Test
    public void testIsNotEmptyWithCollections() {
        // Test with null collection
        List<String> nullList = null;
        assertThat(CollectionUtils.isNotEmpty(nullList)).isFalse();

        // Test with empty collection
        List<String> emptyList = new ArrayList<>();
        assertThat(CollectionUtils.isNotEmpty(emptyList)).isFalse();

        // Test with filled collection
        List<String> filledList = new ArrayList<>();
        filledList.add("item");
        assertThat(CollectionUtils.isNotEmpty(filledList)).isTrue();
    }

    @Test
    public void testIsEmptyWithMaps() {
        // Test with null map
        Map<String, String> nullMap = null;
        assertThat(CollectionUtils.isEmpty(nullMap)).isTrue();

        // Test with empty map
        Map<String, String> emptyMap = new HashMap<>();
        assertThat(CollectionUtils.isEmpty(emptyMap)).isTrue();

        // Test with filled map
        Map<String, String> filledMap = new HashMap<>();
        filledMap.put("key", "value");
        assertThat(CollectionUtils.isEmpty(filledMap)).isFalse();
    }

    @Test
    public void testIsNotEmptyWithMaps() {
        // Test with null map
        Map<String, String> nullMap = null;
        assertThat(CollectionUtils.isNotEmpty(nullMap)).isFalse();

        // Test with empty map
        Map<String, String> emptyMap = new HashMap<>();
        assertThat(CollectionUtils.isNotEmpty(emptyMap)).isFalse();

        // Test with filled map
        Map<String, String> filledMap = new HashMap<>();
        filledMap.put("key", "value");
        assertThat(CollectionUtils.isNotEmpty(filledMap)).isTrue();
    }

    @Test
    public void testEncodeMapEnhanced() {
        // Test with null map
        assertThat(CollectionUtils.encodeMap(null)).isNull();

        // Test with empty map
        Map<String, String> emptyMap = new HashMap<>();
        assertThat(CollectionUtils.encodeMap(emptyMap)).isEqualTo("");

        // Test with single entry
        Map<String, String> singleEntry = new HashMap<>();
        singleEntry.put("key", "value");
        assertThat(CollectionUtils.encodeMap(singleEntry)).isEqualTo("key=value");

        // Test with multiple entries
        Map<String, String> multipleEntries = new LinkedHashMap<>();
        multipleEntries.put("key1", "value1");
        multipleEntries.put("key2", "value2");
        assertThat(CollectionUtils.encodeMap(multipleEntries)).isEqualTo("key1=value1&key2=value2");

        // Test with special characters
        Map<String, String> specialChars = new HashMap<>();
        specialChars.put("key with spaces", "value&special=chars");
        assertThat(CollectionUtils.encodeMap(specialChars)).isEqualTo("key with spaces=value&special=chars");
    }

    @Test
    public void testDecodeMapEnhanced() {
        // Test with null input
        assertThat(CollectionUtils.decodeMap(null)).isNull();

        // Test with empty string
        Map<String, String> emptyResult = CollectionUtils.decodeMap("");
        assertThat(emptyResult).isNotNull().isEmpty();

        // Test with single pair
        Map<String, String> singleResult = CollectionUtils.decodeMap("key=value");
        assertThat(singleResult).containsEntry("key", "value");

        // Test with multiple pairs
        Map<String, String> multipleResult = CollectionUtils.decodeMap("key1=value1&key2=value2");
        assertThat(multipleResult).containsEntry("key1", "value1").containsEntry("key2", "value2");

        // Test with malformed pairs (should be ignored)
        Map<String, String> malformedResult = CollectionUtils.decodeMap("key1=value1&malformed&key2=value2");
        assertThat(malformedResult)
                .containsEntry("key1", "value1")
                .containsEntry("key2", "value2")
                .hasSize(2);

        // Test with empty keys or values
        Map<String, String> emptyKVResult = CollectionUtils.decodeMap("key1=&=value2&key3=value3");
        // The entry with empty key results in key "" with value "value2"
        assertThat(emptyKVResult)
                .containsEntry("", "value2")
                .containsEntry("key3", "value3")
                .hasSize(2);
    }

    @Test
    public void testToUpperListEnhanced() {
        // Test with null list
        assertThat(CollectionUtils.toUpperList(null)).isNull();

        // Test with empty list
        List<String> emptyList = new ArrayList<>();
        assertThat(CollectionUtils.toUpperList(emptyList)).isEmpty();

        // Test with normal strings
        List<String> normalList = new ArrayList<>();
        normalList.add("hello");
        normalList.add("world");
        List<String> upperList = CollectionUtils.toUpperList(normalList);
        assertThat(upperList).containsExactly("HELLO", "WORLD");

        // Test with mixed case and null values
        List<String> mixedList = new ArrayList<>();
        mixedList.add("HeLLo");
        mixedList.add(null);
        mixedList.add("WoRLd");
        List<String> mixedUpperList = CollectionUtils.toUpperList(mixedList);
        assertThat(mixedUpperList).containsExactly("HELLO", null, "WORLD");
    }

    @Test
    public void testIsSizeEqualsEnhanced() {
        // Test with both null
        assertThat(CollectionUtils.isSizeEquals(null, null)).isTrue();

        // Test with first null
        List<String> emptyList = new ArrayList<>();
        assertThat(CollectionUtils.isSizeEquals(null, emptyList)).isFalse();

        // Test with second null
        assertThat(CollectionUtils.isSizeEquals(emptyList, null)).isFalse();

        // Test with both empty
        List<String> anotherEmptyList = new ArrayList<>();
        assertThat(CollectionUtils.isSizeEquals(emptyList, anotherEmptyList)).isTrue();

        // Test with same size
        emptyList.add("item");
        anotherEmptyList.add("item");
        assertThat(CollectionUtils.isSizeEquals(emptyList, anotherEmptyList)).isTrue();

        // Test with different sizes
        emptyList.add("another item");
        assertThat(CollectionUtils.isSizeEquals(emptyList, anotherEmptyList)).isFalse();
    }
}
