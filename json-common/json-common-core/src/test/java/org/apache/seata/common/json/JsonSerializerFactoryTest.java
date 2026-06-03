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

import org.apache.seata.common.json.impl.Fastjson2JsonSerializer;
import org.apache.seata.common.json.impl.FastjsonJsonSerializer;
import org.apache.seata.common.json.impl.GsonJsonSerializer;
import org.apache.seata.common.json.impl.JacksonJsonSerializer;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class JsonSerializerFactoryTest {

    @BeforeEach
    @SuppressWarnings("unchecked")
    void clearInstancesCache() throws Exception {
        Field field = JsonSerializerFactory.class.getDeclaredField("INSTANCES");
        field.setAccessible(true);
        ((Map<String, JsonSerializer>) field.get(null)).clear();
    }

    @Test
    public void testGetSerializer_jackson() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("jackson");
        assertThat(serializer).isNotNull().isInstanceOf(JacksonJsonSerializer.class);
    }

    @Test
    public void testGetSerializer_fastjson() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("fastjson");
        assertThat(serializer).isNotNull().isInstanceOf(FastjsonJsonSerializer.class);
    }

    @Test
    public void testGetSerializer_fastjson2() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("fastjson2");
        assertThat(serializer).isNotNull().isInstanceOf(Fastjson2JsonSerializer.class);
    }

    @Test
    public void testGetSerializer_gson() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("gson");
        assertThat(serializer).isNotNull().isInstanceOf(GsonJsonSerializer.class);
    }

    @Test
    public void testGetSerializer_nullName_returnsDefaultJackson() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer(null);
        assertThat(serializer).isNotNull().isInstanceOf(JacksonJsonSerializer.class);
    }

    @Test
    public void testGetSerializer_unknownName_throwsException() {
        assertThatThrownBy(() -> JsonSerializerFactory.getSerializer("definitely-not-a-real-serializer"))
                .isInstanceOf(EnhancedServiceNotFoundException.class);
    }

    @Test
    public void testGetSerializer_sameNameReturnsCachedInstance() {
        JsonSerializer first = JsonSerializerFactory.getSerializer("jackson");
        JsonSerializer second = JsonSerializerFactory.getSerializer("jackson");
        assertThat(second).isSameAs(first);
    }

    /**
     * In json-common-core tests, the jackson3 SPI lives in the separate json-common-jackson3
     * module which is NOT on the test classpath. The factory must catch the
     * EnhancedServiceNotFoundException for "jackson3" and fall back to the default "jackson"
     * serializer.
     */
    @Test
    public void testGetSerializer_jackson3_fallsBackToJacksonWhenUnavailable() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("jackson3");
        assertThat(serializer).isNotNull().isInstanceOf(JacksonJsonSerializer.class);
    }

    @Test
    public void testGetSerializer_jackson3FallbackIsCached() {
        JsonSerializer first = JsonSerializerFactory.getSerializer("jackson3");
        JsonSerializer second = JsonSerializerFactory.getSerializer("jackson3");
        assertThat(second).isSameAs(first);
        assertThat(first).isInstanceOf(JacksonJsonSerializer.class);
    }

    @Test
    public void testGetSerializer_jackson3FallbackIsUsableForSerialization() {
        JsonSerializer serializer = JsonSerializerFactory.getSerializer("jackson3");
        String json = serializer.toJSONString(new SimplePojo("hello", 7));
        assertThat(json).contains("\"name\":\"hello\"").contains("\"value\":7");

        SimplePojo restored = serializer.parseObject(json, SimplePojo.class);
        assertThat(restored).isNotNull();
        assertThat(restored.getName()).isEqualTo("hello");
        assertThat(restored.getValue()).isEqualTo(7);
    }

    public static class SimplePojo {
        private String name;
        private int value;

        public SimplePojo() {}

        public SimplePojo(String name, int value) {
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
