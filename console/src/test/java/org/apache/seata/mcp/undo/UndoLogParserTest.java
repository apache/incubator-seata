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
package org.apache.seata.mcp.undo;

import org.apache.seata.common.Constants;
import org.apache.seata.mcp.undo.parser.UndoLogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class UndoLogParserTest {

    private UndoLogParser parser;

    @BeforeEach
    public void setUp() {
        parser = new UndoLogParser();
    }

    @Test
    public void testDecode() {
        // Create test data
        String testJson = "{\"name\":\"test\",\"value\":123}";
        byte[] testBytes = testJson.getBytes(Constants.DEFAULT_CHARSET);

        // Decode the test data
        String result = parser.decode("fastjson", testBytes);

        // Verify the decoding result
        assertEquals(testJson, result, "The decoding result should be the same as the original JSON string");
    }

    @Test
    public void testDecodeWithEmptyBytes() {
        // Test decoding empty byte arrays
        byte[] emptyBytes = new byte[0];
        String result = parser.decode("fastjson", emptyBytes);

        // The result of the validation is an empty string
        assertEquals("", result, "Decoding an array of empty bytes should return an empty string");
    }

    @Test
    public void testDecodeWithNullBytes() {
        // Test decoding null
        assertThrows(
                NullPointerException.class,
                () -> {
                    parser.decode("fastjson", null);
                },
                "decode null should throw NullPointerException");
    }

    @Test
    public void testDefaultCharset() {
        // The validation DEFAULT_CHARSET is UTF-8
        assertEquals(StandardCharsets.UTF_8, Constants.DEFAULT_CHARSET, "The default character set should be UTF-8");
    }
}
