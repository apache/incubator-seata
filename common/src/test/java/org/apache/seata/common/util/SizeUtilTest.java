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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SizeUtilTest {
    @Test
    void size2Long() {
        assertThatThrownBy(() -> SizeUtil.size2Long(null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> SizeUtil.size2Long("")).isInstanceOf(IllegalArgumentException.class);
        // wrong format
        assertThatThrownBy(() -> SizeUtil.size2Long("2kk")).isInstanceOf(IllegalArgumentException.class);
        // wrong unit
        assertThatThrownBy(() -> SizeUtil.size2Long("2x")).isInstanceOf(IllegalArgumentException.class);

        assertThat(SizeUtil.size2Long("2k")).isEqualTo(2L * 1024);
        assertThat(SizeUtil.size2Long("2m")).isEqualTo(2L * 1024 * 1024);
        assertThat(SizeUtil.size2Long("2G")).isEqualTo(2L * 1024 * 1024 * 1024);
        assertThat(SizeUtil.size2Long("2t")).isEqualTo(2L * 1024 * 1024 * 1024 * 1024);
    }

    @Test
    public void testSize2LongWithValidInputs() {
        // Test all valid units in lowercase
        assertThat(SizeUtil.size2Long("1k")).isEqualTo(1L * 1024);
        assertThat(SizeUtil.size2Long("1m")).isEqualTo(1L * 1024 * 1024);
        assertThat(SizeUtil.size2Long("1g")).isEqualTo(1L * 1024 * 1024 * 1024);
        assertThat(SizeUtil.size2Long("1t")).isEqualTo(1L * 1024 * 1024 * 1024 * 1024);

        // Test all valid units in uppercase
        assertThat(SizeUtil.size2Long("1K")).isEqualTo(1L * 1024);
        assertThat(SizeUtil.size2Long("1M")).isEqualTo(1L * 1024 * 1024);
        assertThat(SizeUtil.size2Long("1G")).isEqualTo(1L * 1024 * 1024 * 1024);
        assertThat(SizeUtil.size2Long("1T")).isEqualTo(1L * 1024 * 1024 * 1024 * 1024);

        // Test larger numbers
        assertThat(SizeUtil.size2Long("1024k")).isEqualTo(1024L * 1024);
        assertThat(SizeUtil.size2Long("512m")).isEqualTo(512L * 1024 * 1024);
        assertThat(SizeUtil.size2Long("2g")).isEqualTo(2L * 1024 * 1024 * 1024);
        assertThat(SizeUtil.size2Long("1t")).isEqualTo(1L * 1024 * 1024 * 1024 * 1024);

        // Test single digit numbers
        assertThat(SizeUtil.size2Long("0k")).isEqualTo(0L);
        assertThat(SizeUtil.size2Long("5m")).isEqualTo(5L * 1024 * 1024);
    }

    @Test
    public void testSize2LongWithInvalidInputs() {
        // Test null input
        assertThatThrownBy(() -> SizeUtil.size2Long(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert 'null' to byte length");

        // Test empty string
        assertThatThrownBy(() -> SizeUtil.size2Long(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert '' to byte length");

        // Test single character
        assertThatThrownBy(() -> SizeUtil.size2Long("k"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert 'k' to byte length");

        // Test invalid number format
        assertThatThrownBy(() -> SizeUtil.size2Long("ak"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert 'ak' to byte length");

        // Test invalid unit
        assertThatThrownBy(() -> SizeUtil.size2Long("1x"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert '1x' to byte length");

        // Test negative numbers
        assertThat(SizeUtil.size2Long("-1k")).isEqualTo(-1L * 1024);

        // Test zero
        assertThat(SizeUtil.size2Long("0g")).isEqualTo(0L);

        // Test invalid format with multiple characters
        assertThatThrownBy(() -> SizeUtil.size2Long("12kk"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert '12kk' to byte length");

        // Test decimal numbers (should be truncated)
        assertThatThrownBy(() -> SizeUtil.size2Long("1.5k"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert '1.5k' to byte length");
    }

    @Test
    public void testSize2LongWithBoundaryConditions() {
        // Test maximum values that don't overflow
        assertThat(SizeUtil.size2Long("8k")).isEqualTo(8L * 1024);

        // Test large valid values
        assertThat(SizeUtil.size2Long("1000000k")).isEqualTo(1000000L * 1024);

        // Test with spaces (should be invalid)
        assertThatThrownBy(() -> SizeUtil.size2Long("1 k"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("could not convert '1 k' to byte length");
    }

    @Test
    public void testSize2LongWithSpecialCases() {
        // Test that we handle long numbers properly
        String largeNumber = Long.toString(Long.MAX_VALUE / (1024 * 1024 * 1024));
        // This should work without overflow
        assertThat(SizeUtil.size2Long(largeNumber + "g")).isEqualTo(Long.parseLong(largeNumber) * 1024 * 1024 * 1024);

        // Test very small numbers
        assertThat(SizeUtil.size2Long("1k")).isEqualTo(1024L);
    }
}
