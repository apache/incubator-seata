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
package org.apache.seata.common.result;

import static org.apache.seata.common.result.Code.SUCCESS;
import static org.apache.seata.common.result.Code.INTERNAL_SERVER_ERROR;
import static org.apache.seata.common.result.Code.UNAUTHORIZED;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class SingleResultTest {

    @Test
    void testConstructor() {
        SingleResult<String> result = new SingleResult<>("200", "ok", "Data");
        Assertions.assertEquals(SUCCESS.code, result.getCode());
        Assertions.assertEquals(SUCCESS.msg, result.getMessage());
        Assertions.assertEquals("Data", result.getData());
    }

    @Test
    void testFailureWithCodeAndMessage() {
        SingleResult<String> result = SingleResult.failure("500", "Server error");
        Assertions.assertEquals(INTERNAL_SERVER_ERROR.code, result.getCode());
        Assertions.assertEquals(INTERNAL_SERVER_ERROR.msg, result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testFailureWithMessage() {
        SingleResult<String> result = SingleResult.failure("Server error");
        Assertions.assertEquals(INTERNAL_SERVER_ERROR.code, result.getCode());
        Assertions.assertEquals("Server error", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testFailureWithErrorCode() {
        SingleResult<String> result = SingleResult.failure(UNAUTHORIZED);
        Assertions.assertEquals("401", result.getCode());
        Assertions.assertEquals("Login failed", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testSuccess() {
        SingleResult<String> result = SingleResult.successWithData("ok");
        Assertions.assertEquals(SUCCESS.code, result.getCode());
        Assertions.assertEquals(SUCCESS.msg, result.getMessage());
        Assertions.assertEquals("ok", result.getData());
    }

    @Test
    void testSuccessWithoutData() {
        SingleResult<String> result = SingleResult.success();
        Assertions.assertEquals(SUCCESS.code, result.getCode());
        Assertions.assertEquals(SUCCESS.msg, result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testSuccessWithMessage() {
        SingleResult<Void> result = SingleResult.success("ok");
        Assertions.assertEquals(SUCCESS.code, result.getCode());
        Assertions.assertEquals("ok", result.getMessage());
        Assertions.assertNull(result.getData());
    }

    @Test
    void testSuccessWithMessageAndData() {
        SingleResult<String> result = SingleResult.success("ok", "Data");
        Assertions.assertEquals(SUCCESS.code, result.getCode());
        Assertions.assertEquals("ok", result.getMessage());
        Assertions.assertEquals("Data", result.getData());
    }

    @Test
    void testGettersAndSetters() {
        SingleResult<String> result = new SingleResult<>("200", "OK");
        result.setData("NewData");
        Assertions.assertEquals("NewData", result.getData());
    }
}
