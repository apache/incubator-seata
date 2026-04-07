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

import com.alibaba.fastjson.TypeReference;
import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.impl.FastjsonJsonSerializer;
import org.apache.seata.common.json.impl.JacksonJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FastjsonJsonSerializerTest {

    private JsonSerializer jsonSerializer;

    @BeforeEach
    void setUp() {
        // Use factory to get FastJSON serializer by name
        jsonSerializer = JsonSerializerFactory.getSerializer("fastjson");
    }

    @Test
    public void testToJSONString_basicObject() {
        TestObject obj = new TestObject("test", 123);
        String json = jsonSerializer.toJSONString(obj);

        assertThat(json).contains("\"name\":\"test\"");
        assertThat(json).contains("\"value\":123");
    }

    @Test
    public void testParseObject_basicObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = jsonSerializer.parseObject(json, TestObject.class);

        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("test");
        assertThat(obj.getValue()).isEqualTo(123);
    }

    @Test
    public void testToJSONString_and_parseObject() {
        TestObject original = new TestObject("school", 456);
        String json = jsonSerializer.toJSONString(original);
        TestObject restored = jsonSerializer.parseObject(json, TestObject.class);

        assertThat(restored.getName()).isEqualTo(original.getName());
        assertThat(restored.getValue()).isEqualTo(original.getValue());
    }

    @Test
    public void testUseAutoType_withType() {
        String json = "{\"@type\":\"some.type\",\"name\":\"test\"}";
        boolean hasAutoType = jsonSerializer.useAutoType(json);
        assertThat(hasAutoType).isTrue();
    }

    @Test
    public void testUseAutoType_withoutType() {
        String json = "{\"name\":\"test\"}";
        boolean hasAutoType = jsonSerializer.useAutoType(json);
        assertThat(hasAutoType).isFalse();
    }

    @Test
    public void testToJSONString_prettyPrint() {
        TestObject obj = new TestObject("pretty", 789);
        String prettyJson = jsonSerializer.toJSONString(obj, true);

        assertThat(prettyJson).contains("\n");
        assertThat(prettyJson).contains("\t");
    }

    @Test
    public void testToJSONString_ignoreAutoType() {
        TestObject obj = new TestObject("noType", 111);
        String jsonWithoutType = jsonSerializer.toJSONString(obj, true, true);

        assertThat(jsonWithoutType).doesNotContain("@type");
    }

    @Test
    public void testParseObject_ignoreAutoType() {
        String json = jsonSerializer.toJSONString(new TestObject("ignored", 222));

        TestObject obj = jsonSerializer.parseObject(json, TestObject.class, true);

        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("ignored");
        assertThat(obj.getValue()).isEqualTo(222);
    }

    @Test
    public void testEmptyList_serialization() {
        List<String> emptyList = new ArrayList<>();
        String json = jsonSerializer.toJSONString(emptyList, false, false);
        assertThat(json).isEqualTo("[]");
    }

    @Test
    public void testEmptyList_deserialization() {
        String json = "[]";
        List<?> list = jsonSerializer.parseObject(json, List.class, false);
        assertThat(list).isEmpty();
    }

    @Test
    public void testNullInput_toJSONString() {
        String json = jsonSerializer.toJSONString(null);
        assertThat(json).isEqualTo("null");
    }

    @Test
    public void testNullInput_parseObject() {
        TestObject obj = jsonSerializer.parseObject(null, TestObject.class);
        assertThat(obj).isNull();

        TestObject obj2 = jsonSerializer.parseObject("{}", null);
        assertThat(obj2).isNull();
    }

    @Test
    public void testParseObject_invalidJson() {
        assertThatThrownBy(() -> jsonSerializer.parseObject("{invalid json}", TestObject.class))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("FastJSON deserialize error");
    }

    @Test
    public void testFactoryReturnsCorrectInstance() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("fastjson");
        assertThat(serializer).isNotNull();
        assertThat(serializer).isInstanceOf(FastjsonJsonSerializer.class);
    }

    @Test
    public void testFactoryReturnsDefaultInstance() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer(null);
        assertThat(serializer).isNotNull();
        assertThat(serializer).isInstanceOf(JacksonJsonSerializer.class);
    }

    @Test
    public void testToJSONString_emptyList() {
        List<String> emptyList = new ArrayList<>();
        String json = jsonSerializer.toJSONString(emptyList, false, false);
        assertThat(json).isEqualTo("[]");
    }

    @Test
    public void testToJSONString_withAutoType() {
        TestObject obj = new TestObject("withType", 789);
        String jsonWithAutoType = jsonSerializer.toJSONString(obj, false, false);

        assertThat(jsonWithAutoType).contains("@type");
    }

    @Test
    public void testParseObject_nullJson() {
        assertThat(jsonSerializer.parseObject(null, TestObject.class, false)).isNull();
    }

    @Test
    public void testParseObject_emptyList() {
        String json = "[]";
        List<?> list = jsonSerializer.parseObject(json, List.class, false);
        assertThat(list).isEmpty();
    }

    @Test
    public void testParseObject_withAutoType() {
        TestObject original = new TestObject("autoTypeTest", 999);
        String jsonWithAutoType = jsonSerializer.toJSONString(original, false, false);

        TestObject restored = jsonSerializer.parseObject(jsonWithAutoType, TestObject.class, false);

        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("autoTypeTest");
        assertThat(restored.getValue()).isEqualTo(999);
    }

    @Test
    public void testParseObject_withType() {

        String json = "{\"name\":\"test\",\"value\":123}";
        Type type = new TypeReference<TestObject>() {}.getType();

        TestObject obj = jsonSerializer.parseObjectWithType(json, type);
        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("test");
        assertThat(obj.getValue()).isEqualTo(123);

        String listJson = "[{\"name\":\"item1\",\"value\":1},{\"name\":\"item2\",\"value\":2}]";
        Type listType = new TypeReference<List<TestObject>>() {}.getType();

        List<TestObject> list = jsonSerializer.parseObjectWithType(listJson, listType);
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getName()).isEqualTo("item1");
        assertThat(list.get(1).getValue()).isEqualTo(2);
    }

    @Test
    public void testParseObject_withIgnoreAutoType() {

        String jsonWithAutoType = jsonSerializer.toJSONString(new TestObject("ignored", 222));

        TestObject objIgnore = jsonSerializer.parseObject(jsonWithAutoType, TestObject.class, true);
        assertThat(objIgnore).isNotNull();
        assertThat(objIgnore.getName()).isEqualTo("ignored");
        assertThat(objIgnore.getValue()).isEqualTo(222);

        TestObject objNoIgnore = jsonSerializer.parseObject(jsonWithAutoType, TestObject.class, false);
        assertThat(objNoIgnore).isNotNull();
        assertThat(objNoIgnore.getName()).isEqualTo("ignored");
        assertThat(objNoIgnore.getValue()).isEqualTo(222);

        List<?> emptyListResult = jsonSerializer.parseObject("", List.class, false);
        assertThat(emptyListResult).isNull();

        List<?> emptyList = jsonSerializer.parseObject("[]", List.class, false);
        assertThat(emptyList).isEmpty();

        assertThat(jsonSerializer.parseObject(null, TestObject.class, false)).isNull();

        assertThatThrownBy(() -> jsonSerializer.parseObject("{invalid json}", TestObject.class, false))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("FastJSON deserialize error");
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
}
