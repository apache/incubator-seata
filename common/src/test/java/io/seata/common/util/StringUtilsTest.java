/*
 *  Copyright 1999-2019 Seata.io Group.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package io.seata.common.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import io.seata.common.Constants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The type String utils test.
 *
 * @author Otis.z
 * @author Geng Zhang
 * @date 2019 /2/20
 */
public class StringUtilsTest {

    /**
     * Test is empty.
     */
    @Test
    public void testIsNullOrEmpty() {

        assertThat(StringUtils.isNullOrEmpty(null)).isTrue();
        assertThat(StringUtils.isNullOrEmpty("abc")).isFalse();
        assertThat(StringUtils.isNullOrEmpty("")).isTrue();
        assertThat(StringUtils.isNullOrEmpty(" ")).isFalse();
    }

    @Test
    public void testInputStream2String() throws IOException {
        assertNull(StringUtils.inputStream2String(null));
        String data = "abc\n"
                + ":\"klsdf\n"
                + "2ks,x:\".,-3sd˚ø≤ø¬≥";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(Constants.DEFAULT_CHARSET));
        assertThat(StringUtils.inputStream2String(inputStream)).isEqualTo(data);
    }

    @Test
    void inputStream2Bytes() {
        assertNull(StringUtils.inputStream2Bytes(null));
        String data = "abc\n"
                + ":\"klsdf\n"
                + "2ks,x:\".,-3sd˚ø≤ø¬≥";
        byte[] bs = data.getBytes(Constants.DEFAULT_CHARSET);
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data.getBytes(Constants.DEFAULT_CHARSET));
        assertThat(StringUtils.inputStream2Bytes(inputStream)).isEqualTo(bs);
    }

    @Test
    void testEquals() {
        Assertions.assertTrue(StringUtils.equals("1", "1"));
        Assertions.assertFalse(StringUtils.equals("1", "2"));
        Assertions.assertFalse(StringUtils.equals(null, "1"));
        Assertions.assertFalse(StringUtils.equals("1", null));
        Assertions.assertFalse(StringUtils.equals("", null));
        Assertions.assertFalse(StringUtils.equals(null, ""));
    }

    @Test
    void testEqualsIgnoreCase() {
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("a", "a"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("a", "A"));
        Assertions.assertTrue(StringUtils.equalsIgnoreCase("A", "a"));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("1", "2"));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase(null, "1"));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("1", null));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase("", null));
        Assertions.assertFalse(StringUtils.equalsIgnoreCase(null, ""));
    }
}
