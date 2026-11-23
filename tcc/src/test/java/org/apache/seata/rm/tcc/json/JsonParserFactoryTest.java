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
package org.apache.seata.rm.tcc.json;

import org.apache.seata.common.json.JsonUtil;
import org.apache.seata.common.loader.EnhancedServiceNotFoundException;
import org.apache.seata.integration.tx.api.json.JsonParser;
import org.apache.seata.integration.tx.api.json.JsonParserFactory;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JsonParserFactoryTest {

    @Test
    public void testGetInstance() {
        assertNotNull(JsonParserFactory.getInstance("jackson"));
    }

    @Test
    public void testGetInstanceThrowsException() {
        assertThrows(EnhancedServiceNotFoundException.class, () -> JsonParserFactory.getInstance("jsonParser"));
    }

    @Test
    public void testDeprecatedParsersDelegateTextOperationsToJsonUtil() throws IOException {
        TestValue value = new TestValue("value");
        String json = JsonUtil.toJSONString(value);

        assertParserDelegatesToJsonUtil(new FastJsonParser(), value, json);
        assertParserDelegatesToJsonUtil(new GsonJsonParser(), value, json);
        assertParserDelegatesToJsonUtil(new JacksonJsonParser(), value, json);
    }

    private void assertParserDelegatesToJsonUtil(JsonParser parser, TestValue value, String json) throws IOException {
        assertEquals(json, parser.toJSONString(value));
        assertEquals(value, parser.parseObject(json, TestValue.class));
    }

    public static class TestValue {

        private String value;

        public TestValue() {}

        TestValue(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object object) {
            return object instanceof TestValue && value.equals(((TestValue) object).value);
        }
    }
}
