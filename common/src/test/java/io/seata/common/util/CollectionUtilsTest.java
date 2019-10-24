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

import java.util.ArrayList;
import java.util.Collections;
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
}
