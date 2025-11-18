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
package org.apache.seata.common.json;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonUtilTest {

    @BeforeEach
    void setUp() {}

    @Test
    public void testToJSONString_basicObject() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtil.toJSONString(obj);

        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\":\"test\"");
        assertThat(json).contains("\"value\":123");
    }

    @Test
    public void testParseObject_basicObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = JsonUtil.parseObject(json, TestObject.class);

        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("test");
        assertThat(obj.getValue()).isEqualTo(123);
    }

    @Test
    public void testToJSONString_and_parseObject_apple() {
        TestObject original = new TestObject("apple", 456);
        String json = JsonUtil.toJSONString(original);
        TestObject restored = JsonUtil.parseObject(json, TestObject.class);

        assertThat(restored.getName()).isEqualTo(original.getName());
        assertThat(restored.getValue()).isEqualTo(original.getValue());
    }

    @Test
    public void testParseObject_nullInputs() {
        TestObject obj1 = JsonUtil.parseObject(null, TestObject.class);
        assertThat(obj1).isNull();

        TestObject obj2 = JsonUtil.parseObject("{\"name\":\"test\"}", null);
        assertThat(obj2).isNull();
    }

    @Test
    public void testParseObject_prefixLogic() {
        String normalJson = "{\"name\":\"normalTest\",\"value\":888}";
        TestObject obj = JsonUtil.parseObject(normalJson, TestObject.class);
        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("normalTest");
        assertThat(obj.getValue()).isEqualTo(888);
    }

    @Test
    public void testToJSONString_nullObject() {
        String json = JsonUtil.toJSONString(null);
        assertThat(json).isEqualTo("null");
    }

    @Test
    public void testParseObject_complexObject() {
        ComplexTestObject complexObj = new ComplexTestObject();
        complexObj.setName("complex");
        complexObj.setValue(789);
        complexObj.setNested(new TestObject("nested", 1));

        String json = JsonUtil.toJSONString(complexObj);
        ComplexTestObject restored = JsonUtil.parseObject(json, ComplexTestObject.class);

        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("complex");
        assertThat(restored.getValue()).isEqualTo(789);
        assertThat(restored.getNested()).isNotNull();
        assertThat(restored.getNested().getName()).isEqualTo("nested");
        assertThat(restored.getNested().getValue()).isEqualTo(1);
    }

    public static class TestObject {
        private String name;
        private int value;

        public TestObject() {}

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }

    public static class ComplexTestObject {
        private String name;
        private int value;
        private TestObject nested;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public TestObject getNested() {
            return nested;
        }

        public void setNested(TestObject nested) {
            this.nested = nested;
        }
    }
}
