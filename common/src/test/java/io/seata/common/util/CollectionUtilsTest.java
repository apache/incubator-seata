/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * The type Collection utils test.
 *
 * @author Geng Zhang
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
        array = new String[]{"1"};
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
        List<String> emptyCollection = new ArrayList<>();
        List<String> filledCollection = new ArrayList<>();

        filledCollection.add("Foo");
        filledCollection.add("Bar");

        Assertions.assertEquals("", CollectionUtils.toString(emptyCollection));
        Assertions.assertEquals("[Foo,Bar]", CollectionUtils.toString(filledCollection));
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
        Date now = new Date();
        objMap.put("a", "aa");
        objMap.put("b", 22);
        objMap.put("c", now);
        Map<String, String> strMap = CollectionUtils.toStringMap(objMap);
        Assertions.assertEquals("aa", strMap.get("a"));
        Assertions.assertEquals("22", strMap.get("b"));
        Assertions.assertEquals(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S").format(now), strMap.get("c"));
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
}
