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
package org.apache.seata.core.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The Result Test
 */
public class ResultTest {
    @Test
    public void testGetResult() {
        // Create a Result with a result value
        Result<String> result = new Result<>("Success", null, null);

        // Test the getResult method
        assertEquals("Success", result.getResult());
    }

    @Test
    public void testGetErrMsg() {
        // Create a Result with an error message
        Result<String> result = new Result<>(null, "Error", null);

        // Test the getErrMsg method
        assertEquals("Error", result.getErrMsg());
    }

    @Test
    public void testGetErrMsgParams() {
        // Create a Result with error message parameters
        Result<String> result = new Result<>(null, null, new Object[]{"param1", "param2"});

        // Test the getErrMsgParams method
        assertArrayEquals(new Object[]{"param1", "param2"}, result.getErrMsgParams());
    }

    @Test
    public void testOk() {
        // Test the ok method
        Result<Boolean> result = Result.ok();

        // Verify that the result is true
        assertTrue(result.getResult());
        assertNull(result.getErrMsg());
        assertNull(result.getErrMsgParams());
    }

    @Test
    public void testBuild() {
        // Create a Result with a result value
        Result<Integer> result = Result.build(100);

        // Test the getResult method
        assertEquals(100, result.getResult().intValue());
        assertNull(result.getErrMsg());
        assertNull(result.getErrMsgParams());
    }

    @Test
    public void testBuildWithErrMsg() {
        // Create a Result with a result value and an error message
        Result<Double> result = Result.build(3.14, "Invalid value");

        // Test the getResult and getErrMsg methods
        assertEquals(3.14, result.getResult(), 0.001);
        assertEquals("Invalid value", result.getErrMsg());
        assertNull(result.getErrMsgParams());
    }

    @Test
    public void testBuildWithParams() {
        // Create a Result with a result value, an error message, and error message parameters
        Result<String> result = Result.buildWithParams("Hello", "Invalid {0} value: {1}", "parameter", 42);

        // Test the getResult, getErrMsg, and getErrMsgParams methods
        assertEquals("Hello", result.getResult());
        assertEquals("Invalid {0} value: {1}", result.getErrMsg());
        assertArrayEquals(new Object[]{"parameter", 42}, result.getErrMsgParams());
    }
}
