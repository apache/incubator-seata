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

import org.apache.seata.common.ConfigurationKeys;
import org.apache.seata.common.DefaultValues;
import org.apache.seata.config.Configuration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonUtilTest {

    @BeforeEach
    void setUp() {}

    @Test
    public void testToJSONStringBasicObject() {
        TestObject obj = new TestObject("test", 123);
        String json = JsonUtil.toJSONString(obj);

        assertThat(json).isNotNull();
        assertThat(json).contains("\"name\":\"test\"");
        assertThat(json).contains("\"value\":123");
    }

    @Test
    public void testParseObjectBasicObject() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = JsonUtil.parseObject(json, TestObject.class);

        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("test");
        assertThat(obj.getValue()).isEqualTo(123);
    }

    @Test
    public void testToJSONStringAndParseObjectApple() {
        TestObject original = new TestObject("apple", 456);
        String json = JsonUtil.toJSONString(original);
        TestObject restored = JsonUtil.parseObject(json, TestObject.class);

        assertThat(restored.getName()).isEqualTo(original.getName());
        assertThat(restored.getValue()).isEqualTo(original.getValue());
    }

    @Test
    public void testParseObjectNullInputs() {
        TestObject obj1 = JsonUtil.parseObject(null, TestObject.class);
        assertThat(obj1).isNull();

        TestObject obj2 = JsonUtil.parseObject("{\"name\":\"test\"}", null);
        assertThat(obj2).isNull();
    }

    @Test
    public void testParseObjectPrefixLogic() {
        String normalJson = "{\"name\":\"normalTest\",\"value\":888}";
        TestObject obj = JsonUtil.parseObject(normalJson, TestObject.class);
        assertThat(obj).isNotNull();
        assertThat(obj.getName()).isEqualTo("normalTest");
        assertThat(obj.getValue()).isEqualTo(888);
    }

    @Test
    public void testToJSONStringNullObject() {
        String json = JsonUtil.toJSONString(null);
        assertThat(json).isEqualTo("null");
    }

    @Test
    public void testParseObjectComplexObject() {
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

    @Test
    public void testResolveJsonSerializerNamePrefersNewConfig() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getConfig(ConfigurationKeys.JSON_SERIALIZER_TYPE)).thenReturn("gson");

        assertThat(JsonUtil.resolveJsonSerializerName(configuration)).isEqualTo("gson");
    }

    @Test
    public void testResolveJsonSerializerNameFallsBackToDeprecatedConfig() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getConfig(ConfigurationKeys.JSON_SERIALIZER_TYPE)).thenReturn(null);
        when(configuration.getConfig(ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME))
                .thenReturn("fastjson2");

        assertThat(JsonUtil.resolveJsonSerializerName(configuration)).isEqualTo("fastjson2");
    }

    @Test
    public void testResolveJsonSerializerNameReturnsDefaultWhenConfigMissing() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getConfig(ConfigurationKeys.JSON_SERIALIZER_TYPE)).thenReturn(null);
        when(configuration.getConfig(ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME))
                .thenReturn(null);

        assertThat(JsonUtil.resolveJsonSerializerName(configuration))
                .isEqualTo(DefaultValues.BUSINESS_ACTION_CONTEXT_JSON_PARSER);
    }

    @Test
    public void testResolveJsonSerializerNameIgnoresBlankNewConfigAndFallsBackToDeprecatedConfig() {
        Configuration configuration = mock(Configuration.class);
        when(configuration.getConfig(ConfigurationKeys.JSON_SERIALIZER_TYPE)).thenReturn(" ");
        when(configuration.getConfig(ConfigurationKeys.TCC_BUSINESS_ACTION_CONTEXT_JSON_PARSER_NAME))
                .thenReturn("fastjson2");

        assertThat(JsonUtil.resolveJsonSerializerName(configuration)).isEqualTo("fastjson2");
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
