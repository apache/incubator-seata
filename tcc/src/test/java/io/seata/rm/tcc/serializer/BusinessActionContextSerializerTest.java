/*
 * Copyright 1999-2019 Seata.io Group.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.seata.rm.tcc.serializer;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.alibaba.fastjson.JSON;

/**
 * @author zouwei
 * @className BusinessActionContextSerializerTest
 * @date: 2022/10/28 16:09
 * @description:
 */
public class BusinessActionContextSerializerTest {

    /**
     * test deserialize fastjson string
     */
    @Test
    public void testDeserializeFastJsonString() {
        Map<String, Object> dataMap = new HashMap<>();
        List<Long> a = new ArrayList<>();
        a.add(10L);
        a.add(11L);
        dataMap.put("userId", "123456");
        dataMap.put("amount", 7654L);
        dataMap.put("list", "syxbk");
        dataMap.put("hello", 'Z');
        dataMap.put("num", 50);
        dataMap.put("list1", a);
        dataMap.put("array", "zouwei");
        dataMap.put("float", 0.99f);
        dataMap.put("double", 0.99d);
        dataMap.put("bigDecimal", BigDecimal.valueOf(0.88));
        String fastJsonString = JSON.toJSONString(dataMap);
        Map<String, Object> result = BusinessActionContextSerializer.parseObject(fastJsonString, Map.class);
        List<Integer> list1 = (List<Integer>)result.get("list1");

        Assertions.assertEquals("123456", result.get("userId"));
        Assertions.assertEquals(7654, result.get("amount"));
        Assertions.assertEquals("Z", result.get("hello"));
        Assertions.assertEquals(50, result.get("num"));
        Assertions.assertEquals("zouwei", result.get("array"));
        Assertions.assertEquals(BigDecimal.valueOf(0.99), result.get("float"));
        Assertions.assertEquals(BigDecimal.valueOf(0.99), result.get("double"));
        Assertions.assertEquals(BigDecimal.valueOf(0.88), result.get("bigDecimal"));
        Assertions.assertEquals(10, list1.get(0));
    }

    /**
     * test deserialize jackson json string
     */
    @Test
    public void testDeserializeJacksonString() {
        Map<String, Object> dataMap = new HashMap<>();
        List<Long> a = new ArrayList<>();
        a.add(10L);
        a.add(11L);
        dataMap.put("userId", "123456");
        dataMap.put("amount", 7654L);
        dataMap.put("list", "syxbk");
        dataMap.put("hello", 'Z');
        dataMap.put("num", 50);
        dataMap.put("list1", a);
        dataMap.put("array", "zouwei");
        dataMap.put("float", 0.99f);
        dataMap.put("double", 0.99d);
        dataMap.put("bigDecimal", BigDecimal.valueOf(0.88));
        String fastJsonString = BusinessActionContextSerializer.toJsonString(dataMap);
        Map<String, Object> result = BusinessActionContextSerializer.parseObject(fastJsonString, Map.class);
        List<Long> list1 = (List<Long>)result.get("list1");
        Assertions.assertEquals("123456", result.get("userId"));
        Assertions.assertEquals(7654L, result.get("amount"));
        Assertions.assertEquals('Z', result.get("hello"));
        Assertions.assertEquals(50, result.get("num"));
        Assertions.assertEquals("zouwei", result.get("array"));
        Assertions.assertEquals(0.99f, result.get("float"));
        Assertions.assertEquals(0.99d, result.get("double"));
        Assertions.assertEquals(BigDecimal.valueOf(0.88), result.get("bigDecimal"));
        Assertions.assertEquals(10L, list1.get(0));
    }
}
