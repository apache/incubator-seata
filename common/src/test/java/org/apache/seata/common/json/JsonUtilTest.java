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

import org.apache.seata.common.exception.JsonParseException;
import org.apache.seata.common.json.impl.FastjsonJsonSerializer;
import org.apache.seata.common.json.impl.GsonJsonSerializer;
import org.apache.seata.common.json.impl.JacksonJsonSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonUtilTest {

    private JsonUtil fastjsonUtil;
    private JsonUtil jacksonUtil;
    private JsonUtil gsonUtil;

    @BeforeEach
    void setUp() {
        // Create JsonUtil instances with different serializers
        fastjsonUtil = new JsonUtil(JsonSerializerFactory.getSerializer("fastjson"));
        jacksonUtil = new JsonUtil(JsonSerializerFactory.getSerializer("jackson"));
        gsonUtil = new JsonUtil(JsonSerializerFactory.getSerializer("gson"));
    }

    @Test
    public void testGetSerializer_returnsCorrectInstance() {
        assertThat(fastjsonUtil.getSerializer()).isInstanceOf(FastjsonJsonSerializer.class);
        assertThat(jacksonUtil.getSerializer()).isInstanceOf(JacksonJsonSerializer.class);
        assertThat(gsonUtil.getSerializer()).isInstanceOf(GsonJsonSerializer.class);
    }

    @Test
    public void testToJSONString_basicObject_allSerializers() {
        TestObject obj = new TestObject("test", 123);

        // FastJSON should include @type
        String fastjson = fastjsonUtil.toJSONString(obj);
        assertThat(fastjson).contains("\"name\":\"test\"");
        assertThat(fastjson).contains("\"value\":123");
        assertThat(fastjson).contains("@type");

        // Jackson should not include @type by default
        String jackson = jacksonUtil.toJSONString(obj);
        assertThat(jackson).contains("\"name\":\"test\"");
        assertThat(jackson).contains("\"value\":123");
        assertThat(jackson).doesNotContain("@type");

        // Gson should not include @type
        String gson = gsonUtil.toJSONString(obj);
        assertThat(gson).contains("\"name\":\"test\"");
        assertThat(gson).contains("\"value\":123");
        assertThat(gson).doesNotContain("@type");
    }

    @Test
    public void testParseObject_basicObject_allSerializers() {
        // Use simple JSON without @type (compatible with all serializers)
        String json = "{\"name\":\"test\",\"value\":123}";

        TestObject fastjsonObj = fastjsonUtil.parseObject(json, TestObject.class);
        TestObject jacksonObj = jacksonUtil.parseObject(json, TestObject.class);
        TestObject gsonObj = gsonUtil.parseObject(json, TestObject.class);

        assertThat(fastjsonObj).isNotNull();
        assertThat(jacksonObj).isNotNull();
        assertThat(gsonObj).isNotNull();

        assertThat(fastjsonObj.getName()).isEqualTo("test");
        assertThat(jacksonObj.getValue()).isEqualTo(123);
        assertThat(gsonObj.getName()).isEqualTo("test");
    }

    @Test
    public void testToJSONString_and_parseObject_allSerializers() {
        TestObject original = new TestObject("school", 456);

        String fastjson = fastjsonUtil.toJSONString(original);
        TestObject fastjsonRestored = fastjsonUtil.parseObject(fastjson, TestObject.class);
        assertThat(fastjsonRestored.getName()).isEqualTo(original.getName());
        assertThat(fastjsonRestored.getValue()).isEqualTo(original.getValue());

        String jackson = jacksonUtil.toJSONString(original);
        TestObject jacksonRestored = jacksonUtil.parseObject(jackson, TestObject.class);
        assertThat(jacksonRestored.getName()).isEqualTo(original.getName());
        assertThat(jacksonRestored.getValue()).isEqualTo(original.getValue());

        String gson = gsonUtil.toJSONString(original);
        TestObject gsonRestored = gsonUtil.parseObject(gson, TestObject.class);
        assertThat(gsonRestored.getName()).isEqualTo(original.getName());
        assertThat(gsonRestored.getValue()).isEqualTo(original.getValue());
    }

    @Test
    public void testToJSONString_nullInput() {
        assertThat(fastjsonUtil.toJSONString(null)).isEqualTo("null");
        assertThat(jacksonUtil.toJSONString(null)).isEqualTo("null");
        assertThat(gsonUtil.toJSONString(null)).isEqualTo("null");
    }

    @Test
    public void testParseObject_nullText() {
        assertThat(fastjsonUtil.parseObject(null, TestObject.class)).isNull();
        assertThat(jacksonUtil.parseObject(null, TestObject.class)).isNull();
        assertThat(gsonUtil.parseObject(null, TestObject.class)).isNull();
    }

    @Test
    public void testParseObject_nullClass() {
        String json = "{\"name\":\"test\"}";
        assertThat(fastjsonUtil.parseObject(json, (Class<TestObject>) null)).isNull();
        assertThat(jacksonUtil.parseObject(json, (Class<TestObject>) null)).isNull();
        assertThat(gsonUtil.parseObject(json, (Class<TestObject>) null)).isNull();
    }

    @Test
    public void testParseObject_genericType_list() {
        String json = "[{\"name\":\"item1\",\"value\":1},{\"name\":\"item2\",\"value\":2}]";

        List<TestObject> fastjsonList = fastjsonUtil.parseObject(json, List.class);
        List<TestObject> jacksonList = jacksonUtil.parseObject(json, List.class);
        List<TestObject> gsonList = gsonUtil.parseObject(json, List.class);

        assertThat(fastjsonList).hasSize(2);
        assertThat(jacksonList).hasSize(2);
        assertThat(gsonList).hasSize(2);
    }

    @Test
    public void testParseObject_invalidJson_allSerializers() {
        String invalidJson = "{invalid json}";

        assertThatThrownBy(() -> fastjsonUtil.parseObject(invalidJson, TestObject.class))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("deserialize error");

        assertThatThrownBy(() -> jacksonUtil.parseObject(invalidJson, TestObject.class))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("deserialize error");

        assertThatThrownBy(() -> gsonUtil.parseObject(invalidJson, TestObject.class))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("deserialize error");
    }

    @Test
    public void testEmptyList_serialization_allSerializers() {
        List<String> emptyList = new ArrayList<>();

        assertThat(fastjsonUtil.toJSONString(emptyList)).isEqualTo("[]");
        assertThat(jacksonUtil.toJSONString(emptyList)).isEqualTo("[]");
        assertThat(gsonUtil.toJSONString(emptyList)).isEqualTo("[]");
    }

    @Test
    public void testEmptyList_deserialization_allSerializers() {
        String json = "[]";

        List<?> fastjsonList = fastjsonUtil.parseObject(json, List.class);
        List<?> jacksonList = jacksonUtil.parseObject(json, List.class);
        List<?> gsonList = gsonUtil.parseObject(json, List.class);

        assertThat(fastjsonList).isEmpty();
        assertThat(jacksonList).isEmpty();
        assertThat(gsonList).isEmpty();
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
