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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MapUtilTest {

    @Test
    public void testAsMap() {
        Map<String, Object> map = MapUtil.asMap("abc");
        Assertions.assertEquals(Collections.singletonMap("document", "abc"), map);

        Map<Object, Object> source = new HashMap<>();
        source.put("map", Collections.singletonMap("key", "abc"));
        source.put("list", Collections.singletonList(123));
        source.put(123, 123);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", Collections.singletonList(123));
        result.put("map", Collections.singletonMap("key", "abc"));
        result.put("[123]", 123);

        map = MapUtil.asMap((Object) source);
        Assertions.assertEquals(result, map);
    }

    @Test
    public void testGetFlattenedMap() {
        Map<String, Object> source = new HashMap<>();
        source.put("map", Collections.singletonMap("key", "abc"));
        source.put("list", Collections.singletonList(123));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list[0]", 123);
        result.put("map.key", "abc");

        Map<String, Object> map = MapUtil.getFlattenedMap(source);
        Assertions.assertEquals(result, map);
    }

    @Test
    public void testAsMapWithNonMapObject() {
        // Test with String
        Map<String, Object> result = MapUtil.asMap("testString");
        assertThat(result).containsExactlyEntriesOf(Collections.singletonMap("document", "testString"));

        // Test with Integer
        result = MapUtil.asMap(123);
        assertThat(result).containsExactlyEntriesOf(Collections.singletonMap("document", 123));

        // Test with null
        result = MapUtil.asMap(null);
        assertThat(result).containsExactlyEntriesOf(Collections.singletonMap("document", null));
    }

    @Test
    public void testAsMapWithSimpleMap() {
        Map<Object, Object> source = new HashMap<>();
        source.put("key1", "value1");
        source.put("key2", 123);
        source.put(456, "value3");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("key1", "value1");
        expected.put("key2", 123);
        expected.put("[456]", "value3");

        Map<String, Object> result = MapUtil.asMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testAsMapWithNestedMap() {
        Map<Object, Object> nestedMap = new HashMap<>();
        nestedMap.put("nestedKey", "nestedValue");

        Map<Object, Object> source = new HashMap<>();
        source.put("map", nestedMap);
        source.put("simpleKey", "simpleValue");

        Map<String, Object> expectedNested = new LinkedHashMap<>();
        expectedNested.put("nestedKey", "nestedValue");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("map", expectedNested);
        expected.put("simpleKey", "simpleValue");

        Map<String, Object> result = MapUtil.asMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testAsMapWithCharSequenceKeys() {
        Map<Object, Object> source = new HashMap<>();
        StringBuilder stringBuilder = new StringBuilder("builderKey");
        source.put(stringBuilder, "builderValue");
        source.put("stringKey", "stringValue");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("builderKey", "builderValue");
        expected.put("stringKey", "stringValue");

        Map<String, Object> result = MapUtil.asMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithEmptyMap() {
        Map<String, Object> source = new HashMap<>();
        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEmpty();
    }

    @Test
    public void testGetFlattenedMapWithSimpleValues() {
        Map<String, Object> source = new HashMap<>();
        source.put("stringKey", "stringValue");
        source.put("intKey", 123);
        source.put("nullKey", null);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("stringKey", "stringValue");
        expected.put("intKey", 123);
        expected.put("nullKey", "");

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithNestedMap() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("nestedKey", "nestedValue");

        Map<String, Object> source = new HashMap<>();
        source.put("parent", nested);
        source.put("simple", "value");

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("parent.nestedKey", "nestedValue");
        expected.put("simple", "value");

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithDeeplyNestedMap() {
        Map<String, Object> deepNested = new HashMap<>();
        deepNested.put("deepKey", "deepValue");

        Map<String, Object> nested = new HashMap<>();
        nested.put("nestedMap", deepNested);

        Map<String, Object> source = new HashMap<>();
        source.put("root", nested);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("root.nestedMap.deepKey", "deepValue");

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithCollection() {
        List<Object> list = new ArrayList<>();
        list.add("item1");
        list.add("item2");

        Map<String, Object> source = new HashMap<>();
        source.put("list", list);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("list[0]", "item1");
        expected.put("list[1]", "item2");

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithEmptyCollection() {
        List<Object> emptyList = new ArrayList<>();

        Map<String, Object> source = new HashMap<>();
        source.put("emptyList", emptyList);

        Map<String, Object> expected = new LinkedHashMap<>();

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithNestedCollectionInMap() {
        List<Object> list = new ArrayList<>();
        list.add("item");

        Map<String, Object> nested = new HashMap<>();
        nested.put("list", list);

        Map<String, Object> source = new HashMap<>();
        source.put("parent", nested);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("parent.list[0]", "item");

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    public void testGetFlattenedMapWithPathStartingWithBracket() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("[special]", "value");

        Map<String, Object> source = new HashMap<>();
        source.put("parent", nested);

        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("parent[special]", "value");

        Map<String, Object> result = MapUtil.getFlattenedMap(source);
        assertThat(result).isEqualTo(expected);
    }
}
