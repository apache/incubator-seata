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

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


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
}
