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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.seata.common.Constants;
import io.seata.common.holder.ObjectHolder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.opentest4j.AssertionFailedError;

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
    public void testHump2Line(){
        assertThat(StringUtils.hump2Line("abc-d").equals("abcD")).isTrue();
        assertThat(StringUtils.hump2Line("aBc").equals("a-bc")).isTrue();
        assertThat(StringUtils.hump2Line("abc").equals("abc")).isTrue();
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
    void testToStringAndCycleDependency() throws Exception {
        //case: String
        Assertions.assertEquals("\"aaa\"", StringUtils.toString("aaa"));

        //case: CharSequence
        Assertions.assertEquals("\"bbb\"", StringUtils.toString(new StringBuilder("bbb")));
        //case: Number
        Assertions.assertEquals("1", StringUtils.toString(1));
        //case: Boolean
        Assertions.assertEquals("true", StringUtils.toString(true));
        //case: Character
        Assertions.assertEquals("'2'", StringUtils.toString('2'));
        //case: Charset
        Assertions.assertEquals("UTF-8", StringUtils.toString(StandardCharsets.UTF_8));
        //case: Thread
        try {
            Assertions.assertEquals("Thread[main,5,main]", StringUtils.toString(Thread.currentThread()));
        } catch (AssertionFailedError e) {
            // for java21 and above
            Assertions.assertEquals("Thread[#" + Thread.currentThread().getId() + ",main,5,main]", StringUtils.toString(Thread.currentThread()));
        }

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

        //case: Annotation
        TestAnnotation annotation = TestClass.class.getAnnotation(TestAnnotation.class);
        Assertions.assertEquals("@" + TestAnnotation.class.getSimpleName() + "(test=true)", StringUtils.toString(annotation));

        //case: Class
        Class<?> clazz = TestClass.class;
        Assertions.assertEquals("Class<" + clazz.getSimpleName() + ">", StringUtils.toString(clazz));

        //case: Method
        Method method = clazz.getMethod("setObj", TestClass.class);
        Assertions.assertEquals("Method<" + clazz.getSimpleName() + ".setObj(" + clazz.getSimpleName() + ")>", StringUtils.toString(method));

        //case: Field
        Field field = clazz.getDeclaredField("s");
        Assertions.assertEquals("Field<" + clazz.getSimpleName() + ".(String s)>", StringUtils.toString(field));

        //case: List, and cycle dependency
        List<Object> list = new ArrayList<>();
        list.add("xxx");
        list.add(111);
        list.add(list);
        Assertions.assertEquals("[\"xxx\", 111, (this ArrayList)]", StringUtils.toString(list));

        //case: String Array
        String[] strArr = new String[2];
        strArr[0] = "11";
        strArr[1] = "22";
        Assertions.assertEquals("[\"11\", \"22\"]", StringUtils.toString(strArr));
        //case: int Array
        int[] intArr = new int[2];
        intArr[0] = 11;
        intArr[1] = 22;
        Assertions.assertEquals("[11, 22]", StringUtils.toString(intArr));
        //case: Array, and cycle dependency
        Object[] array = new Object[3];
        array[0] = 1;
        array[1] = '2';
        array[2] = array;
        Assertions.assertEquals("[1, '2', (this Object[])]", StringUtils.toString(array));

        //case: Map, and cycle dependency
        Map<Object, Object> map = new HashMap<>();
        map.put("aaa", 111);
        map.put("bbb", true);
        map.put("self", map);
        Assertions.assertEquals("{\"aaa\"->111, \"bbb\"->true, \"self\"->(this HashMap)}", StringUtils.toString(map));
        Assertions.assertFalse(CycleDependencyHandler.isStarting());
        //case: Map, and cycle dependency（deep case）
        List<Object> list2 = new ArrayList<>();
        list2.add(map);
        list2.add('c');
        map.put("list", list2);
        Assertions.assertEquals("{\"aaa\"->111, \"bbb\"->true, \"self\"->(this HashMap), \"list\"->[(ref HashMap), 'c']}", StringUtils.toString(map));
        Assertions.assertFalse(CycleDependencyHandler.isStarting());


        //case: Object
        Assertions.assertEquals("CycleDependency(s=\"a\", obj=null)", StringUtils.toString(CycleDependency.A));
        //case: Object, and cycle dependency
        CycleDependency obj = new CycleDependency("c");
        obj.setObj(obj);
        Assertions.assertEquals("CycleDependency(s=\"c\", obj=(this CycleDependency))", StringUtils.toString(obj));
        //case: Object
        CycleDependency obj2 = new CycleDependency("d");
        obj.setObj(obj2);
        Assertions.assertEquals("CycleDependency(s=\"c\", obj=CycleDependency(s=\"d\", obj=null))", StringUtils.toString(obj));
        //case: Object, and cycle dependency
        TestClass a = new TestClass();
        a.setObj(a);
        Assertions.assertEquals("TestClass(obj=(this TestClass), s=null)", StringUtils.toString(a));
        //case: Object, and cycle dependency（deep case）
        TestClass b = new TestClass();
        TestClass c = new TestClass();
        b.setObj(c);
        c.setObj(a);
        a.setObj(b);
        Assertions.assertEquals("TestClass(obj=TestClass(obj=TestClass(obj=(ref TestClass), s=null), s=null), s=null)", StringUtils.toString(a));

        //case: anonymous class from an interface
        Object anonymousObj = new TestInterface() {
            private String a = "aaa";

            @Override
            public void test() {
            }
        };
        Assertions.assertEquals("TestInterface$(a=\"aaa\")", StringUtils.toString(anonymousObj));

        //case: anonymous class from an abstract class
        anonymousObj = new TestAbstractClass() {
            private String a = "aaa";

            @Override
            public void test() {
            }
        };
        Assertions.assertEquals("TestAbstractClass$(a=\"aaa\")", StringUtils.toString(anonymousObj));

        //final confirm: do not triggered the `toString` and `hashCode` methods
        Assertions.assertFalse(TestClass.hashCodeTriggered);
        Assertions.assertFalse(TestClass.toStringTriggered);
        Assertions.assertFalse(CycleDependency.hashCodeTriggered);
        Assertions.assertFalse(CycleDependency.toStringTriggered);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface TestAnnotation {
        boolean test() default false;
    }

    interface TestInterface {
        void test();
    }

    abstract class TestAbstractClass {
        abstract void test();
    }

    @TestAnnotation(test = true)
    static class TestClass {
        public static boolean hashCodeTriggered = false;
        public static boolean toStringTriggered = false;

        private TestClass obj;
        private String s;

        @Override
        public int hashCode() {
            hashCodeTriggered = true;
            return super.hashCode();
        }

        @Override
        public String toString() {
            toStringTriggered = true;
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
        public static boolean hashCodeTriggered = false;
        public static boolean toStringTriggered = false;

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
        public int hashCode() {
            hashCodeTriggered = true;
            return super.hashCode();
        }

        @Override
        public String toString() {
            toStringTriggered = true;
            return "(" +
                    "s=" + s + "," +
                    "obj=" + (obj != this ? String.valueOf(obj) : "(this CycleDependency)") +
                    ')';
        }
    }
}
