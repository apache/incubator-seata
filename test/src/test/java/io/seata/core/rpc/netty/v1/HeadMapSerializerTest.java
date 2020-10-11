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
package io.seata.core.rpc.netty.v1;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Geng Zhang
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
}