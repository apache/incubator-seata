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
package org.apache.seata.common.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class NumberUtilsTest {

    @Test
    public void testToInReturnDefaultValueWithNull() {
        Assertions.assertEquals(10, NumberUtils.toInt(null, 10));
    }

    @Test
    public void testToInReturnDefaultValueWithFormatIsInvalid() {
        Assertions.assertEquals(10, NumberUtils.toInt("nine", 10));
    }

    @Test
    public void testToInReturnParsedValue() {
        Assertions.assertEquals(10, NumberUtils.toInt("10", 9));
    }

    @Test
    public void testToLongWithDefaultValue() {
        // Test with null input
        Assertions.assertEquals(Long.valueOf(10L), NumberUtils.toLong(null, 10L));

        // Test with invalid format
        Assertions.assertEquals(Long.valueOf(10L), NumberUtils.toLong("invalid", 10L));

        // Test with valid format
        Assertions.assertEquals(Long.valueOf(20L), NumberUtils.toLong("20", 10L));
    }

    @Test
    public void testToLongWithoutDefaultValue() {
        // Test with null input
        Assertions.assertNull(NumberUtils.toLong(null));

        // Test with blank input
        Assertions.assertNull(NumberUtils.toLong(""));
        Assertions.assertNull(NumberUtils.toLong("   "));

        // Test with valid format
        Assertions.assertEquals(Long.valueOf(20L), NumberUtils.toLong("20"));
    }

    @Test
    public void testToLongWithInvalidFormatThrowsException() {
        // Test that invalid format throws NumberFormatException
        Assertions.assertThrows(NumberFormatException.class, () -> {
            NumberUtils.toLong("invalid");
        });
    }
}
