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
package org.apache.seata.server.storage.redis;

import org.apache.seata.common.exception.StoreException;
import org.apache.seata.server.BaseSpringBootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

public class LuaParserTest extends BaseSpringBootTest {

    @Test
    public void testLuaResultGettersAndSetters() {
        LuaParser.LuaResult luaResult = new LuaParser.LuaResult();
        luaResult.setSuccess(true);
        luaResult.setStatus("OK");
        luaResult.setData("test-data");

        Assertions.assertTrue(luaResult.getSuccess());
        Assertions.assertEquals("OK", luaResult.getStatus());
        Assertions.assertEquals("test-data", luaResult.getData());
    }

    @Test
    public void testLuaResultToString() {
        LuaParser.LuaResult luaResult = new LuaParser.LuaResult();
        luaResult.setSuccess(true);
        luaResult.setStatus("OK");
        luaResult.setData("test-data");

        String result = luaResult.toString();
        Assertions.assertTrue(result.contains("success=true"));
        Assertions.assertTrue(result.contains("type='OK'"));
        Assertions.assertTrue(result.contains("data='test-data'"));
    }

    @Test
    public void testLuaErrorStatusConstants() {
        Assertions.assertEquals("AnotherRollbackIng", LuaParser.LuaErrorStatus.ANOTHER_ROLLBACKING);
        Assertions.assertEquals("AnotherHoldIng", LuaParser.LuaErrorStatus.ANOTHER_HOLDING);
        Assertions.assertEquals("NotExisted", LuaParser.LuaErrorStatus.XID_NOT_EXISTED);
        Assertions.assertEquals("ChangeStatusFail", LuaParser.LuaErrorStatus.ILLEGAL_CHANGE_STATUS);
    }

    @Test
    public void testGetObjectFromJson() {
        String json = "{\"success\":true,\"status\":\"OK\",\"data\":\"test\"}";
        LuaParser.LuaResult result = LuaParser.getObjectFromJson(json, LuaParser.LuaResult.class);

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.getSuccess());
        Assertions.assertEquals("OK", result.getStatus());
        Assertions.assertEquals("test", result.getData());
    }

    @Test
    public void testGetObjectFromJsonWithInvalidJson() {
        String invalidJson = "{invalid json}";
        Assertions.assertThrows(StoreException.class, () -> {
            LuaParser.getObjectFromJson(invalidJson, LuaParser.LuaResult.class);
        });
    }

    @Test
    public void testGetListFromJson() {
        String json =
                "[{\"success\":true,\"status\":\"OK\",\"data\":\"test1\"},{\"success\":false,\"status\":\"ERROR\",\"data\":\"test2\"}]";
        List<LuaParser.LuaResult> results = LuaParser.getListFromJson(json, LuaParser.LuaResult.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(2, results.size());
    }

    @Test
    public void testGetListFromJsonWithInvalidJson() {
        String invalidJson = "[{invalid json}]";
        Assertions.assertThrows(StoreException.class, () -> {
            LuaParser.getListFromJson(invalidJson, LuaParser.LuaResult.class);
        });
    }

    @Test
    public void testGetListFromJsonWithEmptyArray() {
        String json = "[]";
        List<LuaParser.LuaResult> results = LuaParser.getListFromJson(json, LuaParser.LuaResult.class);

        Assertions.assertNotNull(results);
        Assertions.assertEquals(0, results.size());
    }

    static class TestObject {
        private String name;
        private int value;

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

    @Test
    public void testGetObjectFromJsonWithCustomClass() {
        String json = "{\"name\":\"test\",\"value\":123}";
        TestObject obj = LuaParser.getObjectFromJson(json, TestObject.class);

        Assertions.assertNotNull(obj);
        Assertions.assertEquals("test", obj.getName());
        Assertions.assertEquals(123, obj.getValue());
    }
}
