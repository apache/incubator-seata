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

import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CompressUtilTest {

    @Test
    public void testCompress() throws IOException {
        byte[] bytes = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0,
                99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};

        Assertions.assertArrayEquals(bytes,
                CompressUtil.compress(new byte[]{1, 2, 3}));
    }

    @Test
    public void testUncompress() throws IOException {
        byte[] bytes = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0,
                99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};

        Assertions.assertArrayEquals(new byte[]{1, 2, 3},
                CompressUtil.uncompress(bytes));
    }

    @Test
    public void testIsCompressData() {
        Assertions.assertFalse(CompressUtil.isCompressData(null));
        Assertions.assertFalse(CompressUtil.isCompressData(new byte[0]));
        Assertions.assertFalse(CompressUtil.isCompressData(new byte[]{31, 11}));
        Assertions.assertFalse(
                CompressUtil.isCompressData(new byte[]{31, 11, 0}));

        Assertions.assertTrue(
                CompressUtil.isCompressData(new byte[]{31, -117, 0}));
    }
}
