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
package io.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Unit test for StringUtils class
 */
public class StringUtilsTest {

    @Test
    public void testIsEmpty() {
        // Test null string
        Assertions.assertTrue(StringUtils.isEmpty(null));

        // Test empty string
        Assertions.assertTrue(StringUtils.isEmpty(""));

        // Test non-empty string
        Assertions.assertFalse(StringUtils.isEmpty("hello"));

        // Test string with spaces
        Assertions.assertFalse(StringUtils.isEmpty(" "));
    }

    @Test
    public void testIsNotEmpty() {
        // Test null string
        Assertions.assertFalse(StringUtils.isNotEmpty(null));

        // Test empty string
        Assertions.assertFalse(StringUtils.isNotEmpty(""));

        // Test non-empty string
        Assertions.assertTrue(StringUtils.isNotEmpty("hello"));

        // Test string with spaces
        Assertions.assertTrue(StringUtils.isNotEmpty(" "));
    }

    @Test
    public void testIsBlank() {
        // Test null string
        Assertions.assertTrue(StringUtils.isBlank(null));

        // Test empty string
        Assertions.assertTrue(StringUtils.isBlank(""));

        // Test string with only spaces
        Assertions.assertTrue(StringUtils.isBlank("   "));

        // Test string with tabs and newlines
        Assertions.assertTrue(StringUtils.isBlank("\t\n\r "));

        // Test non-blank string
        Assertions.assertFalse(StringUtils.isBlank("hello"));

        // Test string with content and spaces
        Assertions.assertFalse(StringUtils.isBlank(" hello "));
    }

    @Test
    public void testIsNotBlank() {
        // Test null string
        Assertions.assertFalse(StringUtils.isNotBlank(null));

        // Test empty string
        Assertions.assertFalse(StringUtils.isNotBlank(""));

        // Test string with only spaces
        Assertions.assertFalse(StringUtils.isNotBlank("   "));

        // Test non-blank string
        Assertions.assertTrue(StringUtils.isNotBlank("hello"));

        // Test string with content and spaces
        Assertions.assertTrue(StringUtils.isNotBlank(" hello "));
    }

    @Test
    public void testTrim() {
        // Test null string
        Assertions.assertNull(StringUtils.trim(null));

        // Test empty string
        Assertions.assertEquals("", StringUtils.trim(""));

        // Test string with leading/trailing spaces
        Assertions.assertEquals("hello", StringUtils.trim("  hello  "));

        // Test string without spaces
        Assertions.assertEquals("hello", StringUtils.trim("hello"));

        // Test string with only spaces
        Assertions.assertEquals("", StringUtils.trim("   "));
    }

    @Test
    public void testJoin() {
        // Test join with iterator
        List<String> list = Arrays.asList("a", "b", "c");
        Iterator<String> iterator = list.iterator();
        Assertions.assertEquals("a,b,c", StringUtils.join(iterator, ","));
    }

    @Test
    public void testInputStreamToString() {
        // Test with normal input stream
        String testString = "Hello World";
        InputStream inputStream = new ByteArrayInputStream(testString.getBytes());
        String result = StringUtils.inputStream2String(inputStream);
        Assertions.assertEquals(testString, result);

        // Test with empty input stream
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        String emptyResult = StringUtils.inputStream2String(emptyStream);
        Assertions.assertEquals("", emptyResult);
    }

    @Test
    public void testCompatibilityWithApacheSeata() {
        // Test that the compatible StringUtils delegates to Apache Seata's StringUtils
        String testStr = "  test  ";

        // Compare results with Apache Seata's StringUtils
        boolean isEmpty = org.apache.seata.common.util.StringUtils.isEmpty(testStr);
        boolean isEmptyCompat = StringUtils.isEmpty(testStr);
        Assertions.assertEquals(isEmpty, isEmptyCompat);

        boolean isBlank = org.apache.seata.common.util.StringUtils.isBlank(testStr);
        boolean isBlankCompat = StringUtils.isBlank(testStr);
        Assertions.assertEquals(isBlank, isBlankCompat);

        String trimmed = org.apache.seata.common.util.StringUtils.trim(testStr);
        String trimmedCompat = StringUtils.trim(testStr);
        Assertions.assertEquals(trimmed, trimmedCompat);
    }
}
