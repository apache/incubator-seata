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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author liuqiufeng
 */
public class LowerCaseLinkHashMapTest {

    private static final Map<String, Object> lowerCaseLinkHashMap = new LowerCaseLinkHashMap<>();

    public LowerCaseLinkHashMapTest() {
        lowerCaseLinkHashMap.put("Key", "Value");
        lowerCaseLinkHashMap.put("Key2", "Value2");
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void size() {
        Assertions.assertEquals(2,lowerCaseLinkHashMap.size());
    }

    @Test
    void isEmpty() {
        Assertions.assertFalse(lowerCaseLinkHashMap.isEmpty());
        Assertions.assertTrue(new LowerCaseLinkHashMap<>().isEmpty());
    }

    @Test
    void containsKey() {
        Assertions.assertTrue(lowerCaseLinkHashMap.containsKey("key"));
        Assertions.assertTrue(lowerCaseLinkHashMap.containsKey("Key"));
        Assertions.assertTrue(lowerCaseLinkHashMap.containsKey("KEY"));
        Assertions.assertFalse(lowerCaseLinkHashMap.containsKey("test"));
        Assertions.assertFalse(lowerCaseLinkHashMap.containsKey(123));
    }

    @Test
    void containsValue() {
        Assertions.assertTrue(lowerCaseLinkHashMap.containsValue("Value"));
        Assertions.assertFalse(lowerCaseLinkHashMap.containsValue("test"));
    }

    @Test
    void get() {
        Assertions.assertEquals("Value",lowerCaseLinkHashMap.get("key"));
        // not exist
        Assertions.assertNull(lowerCaseLinkHashMap.get("key12"));
        Assertions.assertNull(lowerCaseLinkHashMap.get(123));
    }

    @Test
    void testPutAndRemove() {
        Assertions.assertNull(lowerCaseLinkHashMap.get("keyPut"));
        lowerCaseLinkHashMap.put("keyPut", "valuePut");
        Assertions.assertEquals("valuePut", lowerCaseLinkHashMap.get("keyPut"));
        Assertions.assertEquals("valuePut", lowerCaseLinkHashMap.remove("keyPut"));
        Assertions.assertNull(lowerCaseLinkHashMap.get("keyPut"));
        Assertions.assertNull(lowerCaseLinkHashMap.remove(123));
    }

    @Test
    void testPutAllAndClear() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>();
        map.putAll(lowerCaseLinkHashMap);
        Assertions.assertEquals(map, lowerCaseLinkHashMap);
        map.clear();
        Assertions.assertTrue(map.isEmpty());
    }

    @Test
    void keySet() {
        Set<String> keySet = new HashSet<>();
        keySet.add("Key");
        keySet.add("Key2");
        Assertions.assertEquals(keySet, lowerCaseLinkHashMap.keySet());
        // TODO: 2023/2/7 待确认
    }

    @Test
    void values() {
        List<String> values = new ArrayList<>();
        values.add("Value");
        values.add("Value2");
        Assertions.assertArrayEquals(values.toArray(), lowerCaseLinkHashMap.values().toArray());
    }

    @Test
    void entrySet() {
        // TODO: 2023/2/7
    }

    @Test
    void getOrDefault() {
    }

    @Test
    void replaceAll() {
    }

    @Test
    void putIfAbsent() {
    }

    @Test
    void testRemove() {
    }

    @Test
    void replace() {
    }

    @Test
    void testReplace() {
    }

    @Test
    void computeIfAbsent() {
    }

    @Test
    void computeIfPresent() {
    }

    @Test
    void compute() {
    }

    @Test
    void merge() {
    }

    @Test
    void testClone() {
    }

    @Test
    void testEquals() {
    }

    @Test
    void testHashCode() {
    }

    @Test
    void testToString() {
    }
}
