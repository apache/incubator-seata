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
package org.apache.seata.core.rpc.netty.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 */
class HeadMapSerializerTest {

    @Test
    public void encode() throws Exception {
        HeadMapSerializer simpleMapSerializer = HeadMapSerializer.getInstance();
        Map<String, String> map = null;
        int bs = simpleMapSerializer.encode(map, null);
        Assertions.assertEquals(bs, 0);

        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();
        bs = simpleMapSerializer.encode(map, byteBuf);
        Assertions.assertEquals(bs, 0);

        map = new HashMap<String, String>();
        bs = simpleMapSerializer.encode(map, byteBuf);
        Assertions.assertEquals(bs, 0);

        map.put("1", "2");
        map.put("", "x");
        map.put("a", "");
        map.put("b", null);
        bs = simpleMapSerializer.encode(map, byteBuf);
        Assertions.assertEquals(21, bs);

        Map<String, String> map1 = simpleMapSerializer.decode(byteBuf, 21);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(4, map1.size());
        Assertions.assertEquals("2", map1.get("1"));
        Assertions.assertEquals("x", map1.get(""));
        Assertions.assertEquals("", map1.get("a"));
        Assertions.assertEquals(null, map1.get("b"));

        map1 = simpleMapSerializer.decode(byteBuf, 21);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(0, map1.size());

        map1 = simpleMapSerializer.decode(null, 21);
        Assertions.assertNotNull(map1);
        Assertions.assertEquals(0, map1.size());

        byteBuf.release();
    }

    @Test
    public void testUTF8() throws Exception {
        HeadMapSerializer mapSerializer = HeadMapSerializer.getInstance();
        String s = "test";
        // utf-8 and gbk same in English
        Assertions.assertArrayEquals(s.getBytes(StandardCharsets.UTF_8), s.getBytes("GBK"));

        Map<String, String> map = new HashMap<String, String>();
        map.put("11", "22");
        map.put("222", "333");
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();
        int bs = mapSerializer.encode(map, byteBuf);
        Map newmap = mapSerializer.decode(byteBuf, bs);
        Assertions.assertEquals(map, newmap);

        // support chinese
        map.put("你好", "你好？");
        bs = mapSerializer.encode(map, byteBuf);
        newmap = mapSerializer.decode(byteBuf, bs);
        Assertions.assertEquals(map, newmap);

        byteBuf.release();
    }

    @Test
    public void testGetInstance() {
        HeadMapSerializer instance1 = HeadMapSerializer.getInstance();
        HeadMapSerializer instance2 = HeadMapSerializer.getInstance();
        Assertions.assertNotNull(instance1);
        Assertions.assertSame(instance1, instance2);
    }

    @Test
    public void testDecodeWithZeroLength() {
        HeadMapSerializer serializer = HeadMapSerializer.getInstance();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();

        Map<String, String> map = serializer.decode(byteBuf, 0);
        Assertions.assertNotNull(map);
        Assertions.assertEquals(0, map.size());

        byteBuf.release();
    }

    @Test
    public void testDecodeWithEmptyByteBuf() {
        HeadMapSerializer serializer = HeadMapSerializer.getInstance();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();

        Map<String, String> map = serializer.decode(byteBuf, 10);
        Assertions.assertNotNull(map);
        Assertions.assertEquals(0, map.size());

        byteBuf.release();
    }

    @Test
    public void testEncodeWithNullKey() {
        HeadMapSerializer serializer = HeadMapSerializer.getInstance();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();

        Map<String, String> map = new HashMap<>();
        map.put(null, "value");
        map.put("key", "value");

        int length = serializer.encode(map, byteBuf);
        Assertions.assertTrue(length > 0);

        Map<String, String> decoded = serializer.decode(byteBuf, length);
        Assertions.assertNotNull(decoded);
        Assertions.assertEquals(1, decoded.size());
        Assertions.assertEquals("value", decoded.get("key"));

        byteBuf.release();
    }

    @Test
    public void testWriteAndReadString() {
        HeadMapSerializer serializer = HeadMapSerializer.getInstance();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();

        // test null string
        serializer.writeString(byteBuf, null);
        String readNull = serializer.readString(byteBuf);
        Assertions.assertNull(readNull);

        // test empty string
        serializer.writeString(byteBuf, "");
        String readEmpty = serializer.readString(byteBuf);
        Assertions.assertNotNull(readEmpty);
        Assertions.assertEquals("", readEmpty);

        // test normal string
        String testStr = "test string";
        serializer.writeString(byteBuf, testStr);
        String readStr = serializer.readString(byteBuf);
        Assertions.assertEquals(testStr, readStr);

        byteBuf.release();
    }

    @Test
    public void testEncodeAndDecodeComplexMap() {
        HeadMapSerializer serializer = HeadMapSerializer.getInstance();
        ByteBuf byteBuf = ByteBufAllocator.DEFAULT.heapBuffer();

        Map<String, String> map = new HashMap<>();
        map.put("normalKey", "normalValue");
        map.put("", "emptyKey");
        map.put("nullValue", null);
        map.put("specialChars", "!@#$%^&*()");
        map.put("numbers", "1234567890");

        int length = serializer.encode(map, byteBuf);
        Assertions.assertTrue(length > 0);

        Map<String, String> decoded = serializer.decode(byteBuf, length);
        Assertions.assertNotNull(decoded);
        Assertions.assertEquals(5, decoded.size());
        Assertions.assertEquals("normalValue", decoded.get("normalKey"));
        Assertions.assertEquals("emptyKey", decoded.get(""));
        Assertions.assertNull(decoded.get("nullValue"));
        Assertions.assertEquals("!@#$%^&*()", decoded.get("specialChars"));
        Assertions.assertEquals("1234567890", decoded.get("numbers"));

        byteBuf.release();
    }
}
