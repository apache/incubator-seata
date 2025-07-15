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
package org.apache.console.mcp.parser;

import com.alibaba.fastjson.serializer.SimplePropertyPreFilter;
import org.apache.seata.common.Constants;
import org.apache.seata.common.executor.Initialize;
import org.apache.seata.mcp.parser.FastjsonUndoLogParser;
import org.apache.seata.mcp.parser.UndoLogParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class FastjsonUndoLogParserTest {

    private FastjsonUndoLogParser parser;

    @BeforeEach
    public void setUp() {
        parser = new FastjsonUndoLogParser();
    }

    @Test
    public void testInit() throws Exception {
        // Initialize the parser
        parser.init();
        
        // Get the private field filter
        Field filterField = FastjsonUndoLogParser.class.getDeclaredField("filter");
        filterField.setAccessible(true);
        SimplePropertyPreFilter filter = (SimplePropertyPreFilter) filterField.get(parser);
        
        // Verify that the filter contains "tableMeta" in the exclusion list
        Set<String> excludes = filter.getExcludes();
        assertTrue(excludes.contains("tableMeta"), "排除列表应该包含'tableMeta'");
    }

    @Test
    public void testGetName() {
        // Verify that the getName method returns the correct constant
        assertEquals("fastjson", parser.getName(), "名称应该是'fastjson'");
        assertEquals(FastjsonUndoLogParser.NAME, parser.getName(), "getName应该返回NAME常量");
    }

    @Test
    public void testGetDefaultContent() {
        // Get the default content
        byte[] defaultContent = parser.getDefaultContent();
        
        // The validation content is an array of bytes encoded using UTF-8 using "{}".
        byte[] expected = "{}".getBytes(Constants.DEFAULT_CHARSET);
        assertArrayEquals(expected, defaultContent, "The default content should be an array of UTF-8 encoded bytes for '{}'");
    }

    @Test
    public void testDecode() {
        // Create test data
        String testJson = "{\"name\":\"test\",\"value\":123}";
        byte[] testBytes = testJson.getBytes(Constants.DEFAULT_CHARSET);
        
        // Decode the test data
        String result = parser.decode(testBytes);
        
        // Verify the decoding result
        assertEquals(testJson, result, "解码结果应该与原始JSON字符串相同");
    }

    @Test
    public void testDecodeWithEmptyBytes() {
        // Test decoding empty byte arrays
        byte[] emptyBytes = new byte[0];
        String result = parser.decode(emptyBytes);
        
        // The result of the validation is an empty string
        assertEquals("", result, "Decoding an array of empty bytes should return an empty string");
    }

    @Test
    public void testDecodeWithNullBytes() {
        // Test decoding null
        assertThrows(NullPointerException.class, () -> {
            parser.decode(null);
        }, "decode null should throw NullPointerException");
    }

    @Test
    public void testParserImplementsInterfaces() {
        // Verify that the correct interface is implemented
        assertTrue(parser instanceof UndoLogParser, "The UndoLogParser interface should be implemented");
        assertTrue(parser instanceof Initialize, "The Initialize interface should be implemented");
    }
    
    @Test
    public void testDefaultCharset() {
        // The validation DEFAULT_CHARSET is UTF-8
        assertEquals(StandardCharsets.UTF_8, Constants.DEFAULT_CHARSET, "默认字符集应该是UTF-8");
    }
} 