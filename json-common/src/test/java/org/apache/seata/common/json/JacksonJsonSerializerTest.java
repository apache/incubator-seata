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
import org.apache.seata.common.json.impl.JacksonJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JacksonJsonSerializerTest {

    private JsonSerializer jsonSerializer;

    @BeforeEach
    void setUp() {
        // Use factory to get Jackson serializer by name
        jsonSerializer = JsonSerializerFactory.getSerializer("jackson");
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
        String json =
                "{\"@class\":\"org.apache.seata.common.json.JacksonJsonSerializerTest$TestObject\",\"name\":\"test\",\"value\":123}";
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
    public void testToJSONString_withAutoType() {
        TestObject obj = new TestObject("withType", 789);
        String jsonWithAutoType = jsonSerializer.toJSONString(obj, false, false);

        assertThat(jsonWithAutoType).isNotNull();
        assertThat(jsonWithAutoType).contains("@type");
        assertThat(jsonWithAutoType).contains("\"name\":\"withType\"");
        assertThat(jsonWithAutoType).contains("\"value\":789");
    }

    @Test
    public void testParseObject_withAutoType() {
        TestObject original = new TestObject("autoTypeTest", 999);
        // Serialize with autoType enabled
        String jsonWithAutoType = jsonSerializer.toJSONString(original, false, false);

        // Deserialize with autoType enabled
        TestObject restored = jsonSerializer.parseObject(jsonWithAutoType, TestObject.class, false);

        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("autoTypeTest");
        assertThat(restored.getValue()).isEqualTo(999);
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
                .hasMessageContaining("Jackson deserialize error");
    }

    @Test
    public void testFactoryReturnsCorrectInstance() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("jackson");
        assertThat(serializer).isNotNull();
        assertThat(serializer).isInstanceOf(JacksonJsonSerializer.class);
    }

    @Test
    public void testFactoryReturnsDefaultInstance() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer(null);
        assertThat(serializer).isNotNull();
    }

    @Test
    public void testToJSONString_throwsException() {
        Object unserializable = new Object() {
            private final java.io.InputStream stream = System.in;
        };

        assertThatThrownBy(() -> jsonSerializer.toJSONString(unserializable))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Jackson serialize error");
    }

    @Test
    public void testParseObject_nullText() {
        assertThat(jsonSerializer.parseObject(null, String.class)).isNull();
    }

    @Test
    public void testToJSONString_prettyPrint() {
        TestObject obj = new TestObject("pretty", 789);
        String prettyJson = jsonSerializer.toJSONString(obj, true);

        // Pretty JSON should contain newlines and indentation
        assertThat(prettyJson).contains("\n");
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
    public void testParseObject_ignoreAutoType() {
        TestObject obj = new TestObject("ignored", 222);
        String json = jsonSerializer.toJSONString(obj);

        TestObject restored = jsonSerializer.parseObject(json, TestObject.class, true);

        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("ignored");
        assertThat(restored.getValue()).isEqualTo(222);
    }

    @Test
    public void testParseObject_withType() {
        String json =
                "{\"@type\":\"org.apache.seata.common.json.JacksonJsonSerializerTest$TestObject\",\"name\":\"test\",\"value\":123}";
        Type type = new TypeReference<TestObject>() {}.getType();

        TestObject obj = jsonSerializer.parseObjectWithType(json, type);
        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("test");
        assertThat(obj.getValue()).isEqualTo(123);

        assertThatThrownBy(() -> jsonSerializer.parseObjectWithType("{invalid json}", type))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Jackson deserialize error");
    }

    @Test
    public void testUseAutoType() {
        String jsonWithAutoType = "{\"@type\":\"some.type\",\"name\":\"test\"}";
        boolean hasAutoType = jsonSerializer.useAutoType(jsonWithAutoType);
        assertThat(hasAutoType).isTrue();

        String jsonWithoutAutoType = "{\"name\":\"test\"}";
        boolean noAutoType = jsonSerializer.useAutoType(jsonWithoutAutoType);
        assertThat(noAutoType).isFalse();

        boolean nullAutoType = jsonSerializer.useAutoType(null);
        assertThat(nullAutoType).isFalse();

        boolean emptyAutoType = jsonSerializer.useAutoType("");
        assertThat(emptyAutoType).isFalse();
    }

    @Test
    public void testToJSONString_withPrettyPrint() {
        TestObject obj = new TestObject("pretty", 789);

        String prettyJson = jsonSerializer.toJSONString(obj, true);
        assertThat(prettyJson).contains("\n");
        assertThat(prettyJson).contains("@type");

        String normalJson = jsonSerializer.toJSONString(obj, false);
        assertThat(normalJson).doesNotContain("\n");
        assertThat(normalJson).contains("@type");
    }

    @Test
    public void testToJSONString_withIgnoreAutoTypeAndPrettyPrint() {
        TestObject obj = new TestObject("noType", 111);

        String jsonIgnorePretty = jsonSerializer.toJSONString(obj, true, true);
        assertThat(jsonIgnorePretty).contains("\n");
        assertThat(jsonIgnorePretty).doesNotContain("@type");

        String jsonIgnoreNormal = jsonSerializer.toJSONString(obj, true, false);
        assertThat(jsonIgnoreNormal).doesNotContain("\n");
        assertThat(jsonIgnoreNormal).doesNotContain("@type");

        String jsonNoIgnorePretty = jsonSerializer.toJSONString(obj, false, true);
        assertThat(jsonNoIgnorePretty).contains("\n");
        assertThat(jsonNoIgnorePretty).contains("@type");

        String jsonNoIgnoreNormal = jsonSerializer.toJSONString(obj, false, false);
        assertThat(jsonNoIgnoreNormal).doesNotContain("\n");
        assertThat(jsonNoIgnoreNormal).contains("@type");

        List<String> emptyList = new ArrayList<>();
        String emptyListJson = jsonSerializer.toJSONString(emptyList, false, false);
        assertThat(emptyListJson).isEqualTo("[]");

        Object invalidObject = new Object() {
            private final java.io.InputStream stream = System.in;
        };

        assertThatThrownBy(() -> jsonSerializer.toJSONString(invalidObject, false, false))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Jackson serialize error");
    }

    @Test
    public void testParseObject_withIgnoreAutoType() {
        String jsonWithAutoType = jsonSerializer.toJSONString(new TestObject("ignored", 222), false, false);

        TestObject objIgnore = jsonSerializer.parseObject(jsonWithAutoType, TestObject.class, true);
        assertThat(objIgnore).isNotNull();
        assertThat(objIgnore.getName()).isEqualTo("ignored");
        assertThat(objIgnore.getValue()).isEqualTo(222);

        TestObject objNoIgnore = jsonSerializer.parseObject(jsonWithAutoType, TestObject.class, false);
        assertThat(objNoIgnore).isNotNull();
        assertThat(objNoIgnore.getName()).isEqualTo("ignored");
        assertThat(objNoIgnore.getValue()).isEqualTo(222);

        List<?> emptyList = jsonSerializer.parseObject("[]", List.class, false);
        assertThat(emptyList).isEmpty();

        List<?> emptyListIgnore = jsonSerializer.parseObject("[]", List.class, true);
        assertThat(emptyListIgnore).isEmpty();

        assertThat(jsonSerializer.parseObject(null, TestObject.class, false)).isNull();

        assertThatThrownBy(() -> jsonSerializer.parseObject("{invalid json}", TestObject.class, false))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Jackson deserialize error");
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
