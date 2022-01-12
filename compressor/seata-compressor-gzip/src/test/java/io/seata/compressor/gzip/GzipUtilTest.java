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
package io.seata.compressor.gzip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * @author jsbxyyx
 */
public class GzipUtilTest {

    @Test
    public void test_compress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.compress(null);
        });

        byte[] compress = GzipUtil.compress("aa".getBytes());
        int head = ((int) compress[0] & 0xff) | ((compress[1] << 8) & 0xff00);
        Assertions.assertEquals(GZIPInputStream.GZIP_MAGIC, head);
    }

    @Test
    public void test_decompress() {

        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.decompress(null);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            GzipUtil.decompress(new byte[0]);
        });

        Assertions.assertThrows(RuntimeException.class, () -> {
            byte[] bytes = {0x1, 0x2};
            GzipUtil.decompress(bytes);
        });

    }

    @Test
    public void test_compressEqualDecompress() {

        byte[] compress = GzipUtil.compress("aa".getBytes());

        byte[] decompress = GzipUtil.decompress(compress);

        Assertions.assertEquals("aa", new String(decompress));
    }

    @Test
    public void testCompress() throws IOException {
        GzipCompressor gzipCompressor = new GzipCompressor();
        byte[] bytes = new byte[]{99, 111, 109, 112, 114, 101, 115, 115, 101, 100,31, -117, 8, 0, 0, 0, 0, 0, 0, 0,
                99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};
        Assertions.assertArrayEquals(bytes,gzipCompressor.compress(new byte[]{1, 2, 3}));
    }

    @Test
    public void testUncompress() throws IOException {
        byte[] bytes = new byte[]{99, 111, 109, 112, 114, 101, 115, 115, 101, 100,31, -117, 8, 0, 0, 0, 0, 0, 0, 0,
                99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};

        Assertions.assertArrayEquals(new byte[]{1, 2, 3},new GzipCompressor().decompress(bytes));
    }

//    @Test
//    public void testIsCompressData() {
//        GzipCompressor gzipCompressor = new GzipCompressor();
//        Assertions.assertFalse(gzipCompressor.isCompressData(null));
//        Assertions.assertFalse(gzipCompressor.isCompressData(new byte[0]));
//        Assertions.assertFalse(gzipCompressor.isCompressData(new byte[]{31, 11}));
//        Assertions.assertFalse(
//                gzipCompressor.isCompressData(new byte[]{99, 111, 109, 112, 114, 101, 115, 115, 101, 101,31, 11, 0}));
//
//        Assertions.assertTrue(
//                gzipCompressor.isCompressData(new byte[]{99, 111, 109, 112, 114, 101, 115, 115, 101, 100,31, -117, 0}));
//    }

}
