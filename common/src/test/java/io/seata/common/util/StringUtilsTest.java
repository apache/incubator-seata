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
        //case: String
        Assertions.assertEquals("aaa", StringUtils.toString("aaa"));

        //case: CharSequence
        Assertions.assertEquals("bbb", StringUtils.toString(new StringBuilder("bbb")));
        //case: Number
        Assertions.assertEquals("1", StringUtils.toString(1));
        //case: Boolean
        Assertions.assertEquals("true", StringUtils.toString(true));
        //case: Character
        Assertions.assertEquals("2", StringUtils.toString('2'));

        //case: Date
        Date date = new Date(2021 - 1900, 6 - 1, 15);
        Assertions.assertEquals("2021-06-15", StringUtils.toString(date));
        date.setTime(date.getTime() + 3600000);
        Assertions.assertEquals("2021-06-15 01:00", StringUtils.toString(date));
        date.setTime(date.getTime() + 60000);
        Assertions.assertEquals("2021-06-15 01:01", StringUtils.toString(date));
        date.setTime(date.getTime() + 50000);
        Assertions.assertEquals("2021-06-15 01:01:50", StringUtils.toString(date));
        date.setTime(date.getTime() + 12);
        Assertions.assertEquals("2021-06-15 01:01:50.012", StringUtils.toString(date));

        //case: Enum
        Assertions.assertEquals("ObjectHolder.INSTANCE", StringUtils.toString(ObjectHolder.INSTANCE));

        //case: List, and cycle dependency
        List<Object> list = new ArrayList<>();
        list.add("xxx");
        list.add(111);
        list.add(list);
        Assertions.assertEquals("[xxx, 111, (this ArrayList)]", StringUtils.toString(list));

        //case: Map, and cycle dependency
        Map<Object, Object> map = new HashMap<>();
        map.put("aaa", 111);
        map.put("bbb", true);
        map.put("self", map);
        Assertions.assertEquals("{aaa->111, bbb->true, self->(this HashMap)}", StringUtils.toString(map));
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
        //case: Map, and cycle dependency（deep case）
        List<Object> list2 = new ArrayList<>();
        list2.add(map);
        list2.add('c');
        map.put("list", list2);
        Assertions.assertEquals("{aaa->111, bbb->true, self->(this HashMap), list->[(ref HashMap), c]}", StringUtils.toString(map));
        Assertions.assertFalse(CycleDependencyHandler.isStarting());


        //case: Object
        Assertions.assertEquals("CycleDependency(s=a, obj=null)", StringUtils.toString(CycleDependency.A));
        //case: Object, and cycle dependency
        CycleDependency obj = new CycleDependency("c");
        obj.setObj(obj);
        Assertions.assertEquals("CycleDependency(s=c, obj=(this CycleDependency))", StringUtils.toString(obj));
        //case: Object
        CycleDependency obj2 = new CycleDependency("d");
        obj.setObj(obj2);
        Assertions.assertEquals("CycleDependency(s=c, obj=CycleDependency(s=d, obj=null))", StringUtils.toString(obj));
        //case: Object, and cycle dependency
        TestClass a = new TestClass();
        a.setObj(a);
        Assertions.assertEquals("TestClass(obj=(this TestClass))", StringUtils.toString(a));
        //case: Object, and cycle dependency（deep case）
        TestClass b = new TestClass();
        TestClass c = new TestClass();
        b.setObj(c);
        c.setObj(a);
        a.setObj(b);
        Assertions.assertEquals("TestClass(obj=TestClass(obj=TestClass(obj=(ref TestClass))))", StringUtils.toString(a));
    }

    class TestClass {
        private TestClass obj;

        public String toString() {
            return StringUtils.toString(this);
        }

        public TestClass getObj() {
            return obj;
        }

        public void setObj(TestClass obj) {
            this.obj = obj;
        }
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
            return "(" +
                    "s=" + s + "," +
                    "obj=" + (obj != this ? String.valueOf(obj) : "(this CycleDependency)") +
                    ')';
        }
    }
}
