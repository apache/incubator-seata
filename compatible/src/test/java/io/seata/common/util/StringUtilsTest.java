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

    @Test
    public void testIsNullOrEmpty() {
        // Test null string
        Assertions.assertTrue(StringUtils.isNullOrEmpty(null));

        // Test empty string
        Assertions.assertTrue(StringUtils.isNullOrEmpty(""));

        // Test non-empty string
        Assertions.assertFalse(StringUtils.isNullOrEmpty("hello"));

        // Test string with spaces
        Assertions.assertFalse(StringUtils.isNullOrEmpty(" "));
    }

    @Test
    public void testEquals() {
        // Test both null
        Assertions.assertTrue(StringUtils.equals(null, null));

        // Test one null
        Assertions.assertFalse(StringUtils.equals("test", null));
        Assertions.assertFalse(StringUtils.equals(null, "test"));

        // Test both equal
        Assertions.assertTrue(StringUtils.equals("test", "test"));

        // Test not equal
        Assertions.assertFalse(StringUtils.equals("test1", "test2"));

        // Test case sensitivity
        Assertions.assertFalse(StringUtils.equals("Test", "test"));
    }

    @Test
    public void testEqualsIgnoreCase() {
        // Test both null
        Assertions.assertTrue(StringUtils.equalsIgnoreCase(null, null));

        // Test one null
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("test", null));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase(null, "test"));

        // Test both equal with same case
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("test", "test"));

        // Test both equal with different case
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("Test", "test"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("TEST", "test"));

        // Test not equal
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("test1", "test2"));
    }

    @Test
    public void testInputStreamToBytes() {
        // Test with normal input stream
        byte[] testBytes = new byte[] {1, 2, 3, 4, 5};
        InputStream inputStream = new ByteArrayInputStream(testBytes);
        byte[] result = StringUtils.inputStream2Bytes(inputStream);
        Assertions.assertArrayEquals(testBytes, result);

        // Test with empty input stream
        InputStream emptyStream = new ByteArrayInputStream(new byte[0]);
        byte[] emptyResult = StringUtils.inputStream2Bytes(emptyStream);
        Assertions.assertArrayEquals(new byte[0], emptyResult);
    }

    @Test
    public void testToString() {
        // Test with null object - returns String "null"
        Assertions.assertEquals("null", StringUtils.toString(null));

        // Test with string - adds quotes around it
        Assertions.assertEquals("\"test\"", StringUtils.toString("test"));

        // Test with integer
        Assertions.assertEquals("123", StringUtils.toString(123));

        // Test with boolean
        Assertions.assertEquals("true", StringUtils.toString(true));
    }

    @Test
    public void testTrimToNull() {
        // Test null string
        Assertions.assertNull(StringUtils.trimToNull(null));

        // Test empty string - should return null
        Assertions.assertNull(StringUtils.trimToNull(""));

        // Test string with only spaces - should return null
        Assertions.assertNull(StringUtils.trimToNull("   "));

        // Test string with content
        Assertions.assertEquals("hello", StringUtils.trimToNull("  hello  "));

        // Test string without spaces
        Assertions.assertEquals("hello", StringUtils.trimToNull("hello"));
    }

    @Test
    public void testHump2Line() {
        // Test camelCase to kebab-case (uses hyphen, not underscore)
        Assertions.assertEquals("user-name", StringUtils.hump2Line("userName"));
        Assertions.assertEquals("user-id", StringUtils.hump2Line("userId"));

        // Test with empty string
        Assertions.assertEquals("", StringUtils.hump2Line(""));

        // Test with no uppercase
        Assertions.assertEquals("username", StringUtils.hump2Line("username"));

        // Test with already kebab-case - converts to camelCase
        Assertions.assertEquals("userName", StringUtils.hump2Line("user-name"));
    }

    @Test
    public void testCheckDataSize() {
        // Test with small data - should not throw
        Assertions.assertTrue(StringUtils.checkDataSize("test", "testData", 100, true));

        // Test with null data - should return true
        Assertions.assertTrue(StringUtils.checkDataSize(null, "testData", 100, true));

        // Test with empty data - should return true
        Assertions.assertTrue(StringUtils.checkDataSize("", "testData", 100, true));

        // Test with data exceeding size but not throwing
        Assertions.assertFalse(StringUtils.checkDataSize("test data", "testData", 5, false));
    }

    @Test
    public void testHasLowerCase() {
        // Test with lowercase
        Assertions.assertTrue(StringUtils.hasLowerCase("hello"));
        Assertions.assertTrue(StringUtils.hasLowerCase("Hello"));
        Assertions.assertTrue(StringUtils.hasLowerCase("HELLO world"));

        // Test without lowercase
        Assertions.assertFalse(StringUtils.hasLowerCase("HELLO"));
        Assertions.assertFalse(StringUtils.hasLowerCase("123"));

        // Test with null
        Assertions.assertFalse(StringUtils.hasLowerCase(null));

        // Test with empty string
        Assertions.assertFalse(StringUtils.hasLowerCase(""));
    }

    @Test
    public void testHasUpperCase() {
        // Test with uppercase
        Assertions.assertTrue(StringUtils.hasUpperCase("HELLO"));
        Assertions.assertTrue(StringUtils.hasUpperCase("Hello"));
        Assertions.assertTrue(StringUtils.hasUpperCase("hello WORLD"));

        // Test without uppercase
        Assertions.assertFalse(StringUtils.hasUpperCase("hello"));
        Assertions.assertFalse(StringUtils.hasUpperCase("123"));

        // Test with null
        Assertions.assertFalse(StringUtils.hasUpperCase(null));

        // Test with empty string
        Assertions.assertFalse(StringUtils.hasUpperCase(""));
    }
}
