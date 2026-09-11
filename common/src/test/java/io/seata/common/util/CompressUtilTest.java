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
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

public class CompressUtilTest {

    final byte[] originBytes = new byte[]{1, 2, 3};

    final byte[] compressedBytes1 = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, 0,
            99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};

    // for java17
    final byte[] compressedBytes2 = new byte[]{31, -117, 8, 0, 0, 0, 0, 0, 0, -1,
            99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};


    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11})
    public void testCompress1() throws IOException {
        Assertions.assertArrayEquals(compressedBytes1,
                CompressUtil.compress(originBytes));
    }

    @Test
    @DisabledOnJre({JRE.JAVA_8, JRE.JAVA_11})
    public void testCompress2() throws IOException {
        Assertions.assertArrayEquals(compressedBytes2,
                CompressUtil.compress(originBytes));
    }

    @Test
    public void testUncompress() throws IOException {
        Assertions.assertArrayEquals(originBytes,
                CompressUtil.uncompress(compressedBytes1));

        Assertions.assertArrayEquals(originBytes,
                CompressUtil.uncompress(compressedBytes2));
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
