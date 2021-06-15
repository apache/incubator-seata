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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.Constants;
import io.seata.common.holder.ObjectHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The type String utils test.
 *
 * @author Otis.z
 * @author Geng Zhang
 */
public class StringUtilsTest {

    /**
     * Test is empty.
     */
    @Test
    public void testIsNullOrEmpty() {

        assertThat(StringUtils.isNullOrEmpty(null)).isTrue();
        assertThat(StringUtils.isNullOrEmpty("abc")).isFalse();
        assertThat(StringUtils.isNullOrEmpty("")).isTrue();
        assertThat(StringUtils.isNullOrEmpty(" ")).isFalse();
    }

    @Test
    public void testInputStream2String() throws IOException {
        assertNull(StringUtils.inputStream2String(null));
        String data = "abc\n"
                + ":\"klsdf\n"
                + "2ks,x:\".,-3sd˚ø≤ø¬≥";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(Constants.DEFAULT_CHARSET));
        assertThat(StringUtils.inputStream2String(inputStream)).isEqualTo(data);
    }

    @Test
    void inputStream2Bytes() {
        assertNull(StringUtils.inputStream2Bytes(null));
        String data = "abc\n"
                + ":\"klsdf\n"
                + "2ks,x:\".,-3sd˚ø≤ø¬≥";
        byte[] bs = data.getBytes(Constants.DEFAULT_CHARSET);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(Constants.DEFAULT_CHARSET));
        assertThat(StringUtils.inputStream2Bytes(inputStream)).isEqualTo(bs);
    }

    @Test
    void testEquals() {
        Assertions.assertTrue(StringUtils.equals("1", "1"));
        Assertions.assertFalse(StringUtils.equals("1", "2"));
        Assertions.assertFalse(StringUtils.equals(null, "1"));
        Assertions.assertFalse(StringUtils.equals("1", null));
        Assertions.assertFalse(StringUtils.equals("", null));
        Assertions.assertFalse(StringUtils.equals(null, ""));
    }

    @Test
    void testEqualsIgnoreCase() {
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("a", "a"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("a", "A"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("A", "a"));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("1", "2"));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase(null, "1"));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("1", null));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("", null));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase(null, ""));
    }

    @Test
    void testToStringAndCycleDependency() throws StackOverflowError {
        //case: string
        Assertions.assertEquals("aaa", StringUtils.toString("aaa"));

        //case: number character boolean
        Assertions.assertEquals("1", StringUtils.toString(1));
        Assertions.assertEquals("bbb", StringUtils.toString(new StringBuilder("bbb")));
        Assertions.assertEquals("true", StringUtils.toString(true));

        //case: date
        Date date = new Date(2021 - 1900, 6 - 1, 15);
        Assertions.assertEquals("2021-06-15", StringUtils.toString(date));
        date.setTime(date.getTime() + 3600000);
        Assertions.assertEquals("2021-06-15 01:00", StringUtils.toString(date));
        date.setTime(date.getTime() + 50000);
        Assertions.assertEquals("2021-06-15 01:00:50", StringUtils.toString(date));
        date.setTime(date.getTime() + 12);
        Assertions.assertEquals("2021-06-15 01:00:50.012", StringUtils.toString(date));

        //case: list, and cycle dependency
        List<Object> list = new ArrayList<>();
        list.add(list);
        list.add("xxx");
        list.add(111);
        Assertions.assertEquals("[" + list.toString() + ", xxx, 111]", StringUtils.toString(list));

        //case: map, and cycle dependency
        Map<Object, Object> map = new HashMap<>();
        map.put(map, map);
        map.put("aaa", 111);
        map.put("bbb", true);
        Assertions.assertEquals("{" + map.toString() + "->" + map.toString() + ", aaa->111, bbb->true}", StringUtils.toString(map));

        //case: enum
        Assertions.assertEquals(ObjectHolder.INSTANCE.name(), StringUtils.toString(ObjectHolder.INSTANCE));

        //case: object, and cycle dependency
        Assertions.assertEquals("(s=a; obj=null)", StringUtils.toString(CycleDependency.A));
        CycleDependency obj = new CycleDependency("c");
        obj.setObj(obj);
        Assertions.assertEquals("(s=c; obj={s='c'})", StringUtils.toString(obj));
    }

    static class CycleDependency {
        public static final CycleDependency A = new CycleDependency("a");
        public static final CycleDependency B = new CycleDependency("b");

        private String s;
        private CycleDependency obj;

        private CycleDependency(String s) {
            this.s = s;
        }

        public CycleDependency getObj() {
            return obj;
        }

        public void setObj(CycleDependency obj) {
            this.obj = obj;
        }

        @Override
        public String toString() {
            return "{" +
                    "s='" + s + '\'' +
                    '}';
        }
    }
}
