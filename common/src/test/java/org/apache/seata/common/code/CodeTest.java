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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class CodeTest {

    static Stream<Arguments> codeMessageProvider() {
        return Stream.of(
                Arguments.of(Code.SUCCESS, "ok"), // Test case for SUCCESS
                Arguments.of(Code.ERROR, "Server error"), // Test case for ERROR
                Arguments.of(Code.LOGIN_FAILED, "Login failed") // Test case for LOGIN_FAILED
                );
    }

    @ParameterizedTest
    @MethodSource("codeMessageProvider")
    public void testGetErrorMsgWithValidCodeReturnsExpectedMsg(Code code, String expectedMsg) {
        assertEquals(expectedMsg, code.getMsg());
    }

    @Test
    public void testGetErrorMsgWithInvalidCodeReturnsNull() {
        // Test case for non-existing code
        assertNull(Code.getErrorMsg("404"));
    }

    static Stream<Arguments> codeSetterProvider() {
        return Stream.of(Arguments.of(Code.SUCCESS, "201", "Created"));
    }

    @ParameterizedTest
    @MethodSource("codeSetterProvider")
    public void testSetCodeAndMsgUpdatesValuesCorrectly(Code code, String newCode, String newMsg) {
        code.setCode(newCode);
        code.setMsg(newMsg);
        assertEquals(newCode, code.getCode());
        assertEquals(newMsg, code.getMsg());
    }
}
