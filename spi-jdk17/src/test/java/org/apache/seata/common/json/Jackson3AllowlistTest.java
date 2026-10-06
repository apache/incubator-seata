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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for Jackson3 serializer with allowlist security check
 */
public class Jackson3AllowlistTest {

    private JsonSerializer jsonSerializer;

    @BeforeEach
    void setUp() {
        jsonSerializer = JsonSerializerFactory.getSerializer("jackson3");
    }

    @AfterEach
    void tearDown() {
        JsonAllowlistManager.getInstance().clearUserAllowlist();
    }

    @Test
    public void testParseObject_allowedSeataClass() {

        String json =
                "{\"@type\":\"org.apache.seata.common.json.Jackson3AllowlistTest$AllowedTestClass\",\"name\":\"test\"}";

        AllowedTestClass result = jsonSerializer.parseObject(json, AllowedTestClass.class, false);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
    }

    @Test
    public void testParseObject_allowedJavaClass() {

        String json = "{\"@type\":\"java.util.HashMap\"}";

        Object result = jsonSerializer.parseObject(json, Object.class, false);

        assertThat(result).isNotNull();
    }

    @Test
    public void testParseObject_notAllowedClass() {

        String json = "{\"@type\":\"com.malicious.EvilClass\",\"command\":\"rm -rf /\"}";

        assertThatThrownBy(() -> jsonSerializer.parseObject(json, Object.class, false))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("not in JSON deserialization allowlist")
                .hasMessageContaining("com.malicious.EvilClass");
    }

    @Test
    public void testParseObject_userAllowedClass() {

        JsonAllowlistManager.getInstance().addUserClass("com.example.UserClass");

        String json = "{\"@type\":\"com.example.UserClass\",\"data\":\"test\"}";

        try {
            jsonSerializer.parseObject(json, Object.class, false);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            assertThat(e).isNotInstanceOf(SecurityException.class);
        }
    }

    @Test
    public void testParseObject_userAllowedPrefix() {
        JsonAllowlistManager.getInstance().addUserPrefix("com.mycompany.model.");

        String json = "{\"@type\":\"com.mycompany.model.User\",\"id\":1}";

        try {
            jsonSerializer.parseObject(json, Object.class, false);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {

            assertThat(e).isNotInstanceOf(SecurityException.class);
        }
    }

    @Test
    public void testParseObject_ignoreAutoType_bypasses_check() {

        String json = "{\"@type\":\"com.malicious.EvilClass\",\"command\":\"rm -rf /\"}";

        try {
            jsonSerializer.parseObject(json, Object.class, true);
        } catch (SecurityException e) {
            throw new AssertionError("Should not throw SecurityException when ignoreAutoType=true", e);
        } catch (Exception e) {

            assertThat(e).isNotInstanceOf(SecurityException.class);
        }
    }

    @Test
    public void testParseObject_noAutoType_bypasses_check() {
        String json = "{\"name\":\"test\",\"value\":123}";

        TestObject result = jsonSerializer.parseObject(json, TestObject.class);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("test");
    }

    @Test
    public void testParseObject_multipleAutoTypes() {

        String json = "{\"@type\":\"org.apache.seata.common.json.Jackson3AllowlistTest$ContainerClass\","
                + "\"inner\":{\"@type\":\"org.apache.seata.common.json.Jackson3AllowlistTest$AllowedTestClass\",\"name\":\"nested\"}}";

        ContainerClass result = jsonSerializer.parseObject(json, ContainerClass.class, false);

        assertThat(result).isNotNull();
    }

    @Test
    public void testParseObject_multipleAutoTypes_oneNotAllowed() {

        String json = "{\"@type\":\"org.apache.seata.common.json.Jackson3AllowlistTest$ContainerClass\","
                + "\"inner\":{\"@type\":\"com.malicious.EvilClass\",\"name\":\"evil\"}}";

        assertThatThrownBy(() -> jsonSerializer.parseObject(json, ContainerClass.class, false))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("com.malicious.EvilClass");
    }

    @Test
    public void testParseObject_atTypeInStringValue_notBlocked() {
        String json = "{\"@type\":\"org.apache.seata.common.json.Jackson3AllowlistTest$TestObject\","
                + "\"name\":\"the \\\"@type\\\" field is important\",\"value\":123}";

        TestObject result = jsonSerializer.parseObject(json, TestObject.class, false);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("the \"@type\" field is important");
    }

    @Test
    public void testLoadUserAllowlist_thenParse() {
        JsonAllowlistManager.getInstance().loadUserAllowlist("com.trusted.model.,com.trusted.dto.SpecificDTO");

        String json1 = "{\"@type\":\"com.trusted.model.User\",\"id\":1}";
        try {
            jsonSerializer.parseObject(json1, Object.class, false);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {

        }

        String json2 = "{\"@type\":\"com.trusted.dto.SpecificDTO\",\"data\":\"test\"}";
        try {
            jsonSerializer.parseObject(json2, Object.class, false);
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {

        }

        String json3 = "{\"@type\":\"com.untrusted.EvilClass\",\"data\":\"evil\"}";
        assertThatThrownBy(() -> jsonSerializer.parseObject(json3, Object.class, false))
                .isInstanceOf(SecurityException.class);
    }

    public static class TestObject {
        private String name;
        private int value;

        public TestObject() {}

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

    public static class AllowedTestClass {
        private String name;

        public AllowedTestClass() {}

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    public static class ContainerClass {
        private AllowedTestClass inner;

        public ContainerClass() {}

        public AllowedTestClass getInner() {
            return inner;
        }

        public void setInner(AllowedTestClass inner) {
            this.inner = inner;
        }
    }
}
