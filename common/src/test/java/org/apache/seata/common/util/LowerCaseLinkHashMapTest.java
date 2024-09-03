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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


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
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Assertions.assertNull(map.get("keyPut"));
        map.put("keyPut", "valuePut");
        Assertions.assertEquals("valuePut", map.get("keyPut"));
        Assertions.assertEquals("valuePut", map.remove("keyPut"));
        Assertions.assertNull(map.get("keyPut"));
        Assertions.assertNull(map.remove(123));
        map.put("keyPut", "valuePut");
        Assertions.assertEquals("valuePut", map.get("keyPut"));
        Assertions.assertFalse( map.remove("keyPut","VALUEPUT"));
        Assertions.assertEquals("valuePut", map.get("keyPut"));
        Assertions.assertTrue(map.remove("keyPut","valuePut"));
        Assertions.assertNull(map.get("keyPut"));
        Assertions.assertFalse(map.remove(123, 123));
    }

    @Test
    void testPutAllAndClear() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
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
    }

    @Test
    void values() {
        List<String> values = new ArrayList<>();
        values.add("Value");
        values.add("Value2");
        Assertions.assertArrayEquals(values.toArray(), lowerCaseLinkHashMap.values().toArray());
    }

    @Test
    void getOrDefault() {
        Assertions.assertEquals("Value", lowerCaseLinkHashMap.getOrDefault("Key", "abc"));
        Assertions.assertEquals("Value", lowerCaseLinkHashMap.getOrDefault("key", "abc"));
        Assertions.assertEquals("abc", lowerCaseLinkHashMap.getOrDefault("default", "abc"));
    }

    @Test
    void replaceAll() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        // replace all values with key
        map.replaceAll((key, value) -> key);
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Assertions.assertEquals(entry.getKey(), entry.getValue());
        }
    }

    @Test
    void putIfAbsent() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object obj = map.putIfAbsent("putIfAbsent", "putIfAbsent");
        Assertions.assertNull(obj);
        Assertions.assertEquals("putIfAbsent", map.get("putIfAbsent"));
        obj = map.putIfAbsent("key", "putIfAbsent");
        Assertions.assertEquals("Value", obj);
        Assertions.assertEquals("Value", map.get("key"));
    }

    @Test
    void testRemove() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object value = map.remove("key");
        Assertions.assertEquals("Value", value);
        Assertions.assertFalse(map.containsKey("key"));
        Assertions.assertTrue(map.containsKey("key2"));
    }

    @Test
    void testReplace() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object replace = map.replace("key", "replace");
        Assertions.assertEquals("Value", replace);
        Assertions.assertEquals("replace",map.get("key"));

        boolean result = map.replace("key2", "value2", "replace");
        Assertions.assertFalse(result);
        Assertions.assertEquals("Value2", map.get("key2"));
        
        result = map.replace("key2", "Value2", "replace");
        Assertions.assertTrue(result);
        Assertions.assertEquals("replace", map.get("key2"));
    }

    @Test
    void computeIfAbsent() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object result = map.computeIfAbsent("key", String::toUpperCase);
        Assertions.assertEquals("Value", result);
        Assertions.assertEquals("Value", map.get("key"));

        result = map.computeIfAbsent("computeIfAbsent", String::toUpperCase);
        Assertions.assertEquals("COMPUTEIFABSENT", result);
        Assertions.assertEquals("COMPUTEIFABSENT", map.get("computeIfAbsent"));
    }

    @Test
    void computeIfPresent() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object result = map.computeIfPresent("key", (key,value)-> key.toUpperCase());
        Assertions.assertEquals("KEY", result);
        Assertions.assertEquals("KEY", map.get("key"));

        result = map.computeIfPresent("key", (key, value) -> null);
        Assertions.assertNull(result);
        Assertions.assertFalse(map.containsKey("key"));
        
        result = map.computeIfPresent("computeIfPresent", (key,value)-> key.toUpperCase());
        Assertions.assertNull(result);
        Assertions.assertFalse(map.containsKey("computeIfPresent"));
    }

    @Test
    void compute() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object result = map.compute("key", (key,value)-> key.toUpperCase());
        Assertions.assertEquals("KEY", result);
        Assertions.assertEquals("KEY", map.get("key"));

        result = map.compute("key", (key, value) -> null);
        Assertions.assertNull(result);
        Assertions.assertFalse(map.containsKey("key"));

        result = map.compute("compute", (key,value)-> key.toUpperCase());
        Assertions.assertEquals("COMPUTE", result);
        Assertions.assertEquals("COMPUTE", map.get("compute"));
    }

    @Test
    void merge() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Object result = map.merge("key", "merge",(oldValue,value)-> oldValue.toString().toUpperCase());
        Assertions.assertEquals("VALUE", result);
        Assertions.assertEquals("VALUE", map.get("key"));

        result = map.merge("key", "merge", (oldValue, value) -> null);
        Assertions.assertNull(result);
        Assertions.assertFalse(map.containsKey("key"));

        result = map.merge("compute", "merge", (oldValue, value) -> oldValue.toString().toUpperCase());
        Assertions.assertEquals("merge", result);
        Assertions.assertEquals("merge", map.get("compute"));
    }

    @Test
    void testEquals() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Assertions.assertTrue(map.equals(lowerCaseLinkHashMap));
        map.put("equals", "equals");
        Assertions.assertFalse(map.equals(lowerCaseLinkHashMap));
    }

    @Test
    void testHashCode() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Assertions.assertEquals(lowerCaseLinkHashMap.hashCode(), map.hashCode());
        map.put("equals", "equals2");
        Assertions.assertNotEquals(lowerCaseLinkHashMap.hashCode(), map.hashCode());
    }

    @Test
    void testToString() {
        Map<String, Object> map = new LowerCaseLinkHashMap<>(lowerCaseLinkHashMap);
        Assertions.assertEquals(lowerCaseLinkHashMap.toString(), map.toString());
        map.put("toString", "toString2");
        Assertions.assertNotEquals(lowerCaseLinkHashMap.toString(), map.toString());
    }
    
}
