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
package org.apache.seata.common.code;

import org.apache.seata.common.result.Code;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CodeTest {

    @Test
    public void testGetErrorMsgWithValidCodeReturnsExpectedMsg() {
        // Test case for SUCCESS
        assertEquals("ok", Code.SUCCESS.getMsg());
        // Test case for ERROR
        assertEquals("Server error", Code.ERROR.getMsg());
        // Test case for LOGIN_FAILED
        assertEquals("Login failed", Code.LOGIN_FAILED.getMsg());
    }

    @Test
    public void testGetErrorMsgWithInvalidCodeReturnsNull() {
        // Test case for non-existing code
        assertNull(Code.getErrorMsg("404"));
    }

    @Test
    public void testSetCodeAndMsgUpdatesValuesCorrectly() {
        // Test case to check if setCode and setMsg are working as expected
        Code.SUCCESS.setCode("201");
        Code.SUCCESS.setMsg("Created");
        assertEquals("201", Code.SUCCESS.getCode());
        assertEquals("Created", Code.SUCCESS.getMsg());
    }
}
