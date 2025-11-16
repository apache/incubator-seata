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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The type Result test.
 */
public class ResultTest {

    @Test
    public void testDefaultConstructor() {
        Result<String> result = new Result<>();
        assertEquals(Result.SUCCESS_CODE, result.getCode());
        assertEquals(Result.SUCCESS_MSG, result.getMessage());
    }

    @Test
    public void testConstructorWithParams() {
        Result<String> result = new Result<>("400", "Bad Request");
        assertEquals("400", result.getCode());
        assertEquals("Bad Request", result.getMessage());
    }

    @Test
    public void testIsSuccess() {
        Result<String> successResult = new Result<>(Result.SUCCESS_CODE, Result.SUCCESS_MSG);
        assertTrue(successResult.isSuccess());

        Result<String> failResult = new Result<>(Result.FAIL_CODE, "Failed");
        assertFalse(failResult.isSuccess());

        Result<String> customResult = new Result<>("404", "Not Found");
        assertFalse(customResult.isSuccess());
    }

    @Test
    public void testGettersAndSetters() {
        Result<String> result = new Result<>();

        result.setCode("401");
        assertEquals("401", result.getCode());

        result.setMessage("Unauthorized");
        assertEquals("Unauthorized", result.getMessage());
    }

    @Test
    public void testStaticConstants() {
        assertEquals("200", Result.SUCCESS_CODE);
        assertEquals("success", Result.SUCCESS_MSG);
        assertEquals("500", Result.FAIL_CODE);
    }
}
