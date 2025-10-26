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
package org.apache.seata.compressor.gzip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.zip.GZIPInputStream;

public class GzipUtilTest {

    @Test
    public void testCompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            GzipUtil.compress(null);
        });

        byte[] compress = GzipUtil.compress("aa".getBytes());
        int head = ((int) compress[0] & 0xff) | ((compress[1] << 8) & 0xff00);
        Assertions.assertEquals(GZIPInputStream.GZIP_MAGIC, head);
    }

    @Test
    public void testDecompress() {

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
    public void testCompressEqualDecompress() {

        byte[] compress = GzipUtil.compress("aa".getBytes());

        byte[] decompress = GzipUtil.decompress(compress);

        Assertions.assertEquals("aa", new String(decompress));
    }

    @Test
    public void testCompressEmptyArray() {
        byte[] empty = new byte[0];
        byte[] compressed = GzipUtil.compress(empty);
        byte[] decompressed = GzipUtil.decompress(compressed);
        Assertions.assertArrayEquals(empty, decompressed);
    }

    @Test
    public void testCompressSingleByte() {
        byte[] singleByte = {42};
        byte[] compressed = GzipUtil.compress(singleByte);
        byte[] decompressed = GzipUtil.decompress(compressed);
        Assertions.assertArrayEquals(singleByte, decompressed);
    }

    @Test
    public void testCompressLargeData() {
        byte[] largeData = new byte[1024 * 1024];
        new Random().nextBytes(largeData);
        byte[] compressed = GzipUtil.compress(largeData);
        byte[] decompressed = GzipUtil.decompress(compressed);
        Assertions.assertArrayEquals(largeData, decompressed);
    }

    @Test
    public void testCompressTextData() {
        String text = "The quick brown fox jumps over the lazy dog";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = GzipUtil.compress(original);
        byte[] decompressed = GzipUtil.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressUnicodeData() {
        String text = "测试中文数据压缩解压";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = GzipUtil.compress(original);
        byte[] decompressed = GzipUtil.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressionRatio() {
        String repeatedText = new String(new char[1000]).replace("\0", "a");
        byte[] original = repeatedText.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = GzipUtil.compress(original);
        Assertions.assertTrue(
                compressed.length < original.length,
                "Compressed size should be smaller than original for repetitive data");
    }

    @Test
    public void testDecompressInvalidData() {
        byte[] invalidData = {0x1, 0x2, 0x3, 0x4, 0x5};
        Assertions.assertThrows(RuntimeException.class, () -> {
            GzipUtil.decompress(invalidData);
        });
    }

    @Test
    public void testMultipleCompressionCycles() {
        String text = "Test multiple compression cycles";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        byte[] firstCompressed = GzipUtil.compress(original);
        byte[] firstDecompressed = GzipUtil.decompress(firstCompressed);

        byte[] secondCompressed = GzipUtil.compress(firstDecompressed);
        byte[] secondDecompressed = GzipUtil.decompress(secondCompressed);

        Assertions.assertEquals(text, new String(secondDecompressed, StandardCharsets.UTF_8));
    }
}
