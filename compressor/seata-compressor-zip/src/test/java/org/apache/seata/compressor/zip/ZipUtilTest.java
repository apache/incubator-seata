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
package org.apache.seata.compressor.zip;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * the Zip Util test
 *
 */
public class ZipUtilTest {

    @Test
    public void testCompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZipUtil.compress(null);
        });
    }

    @Test
    public void testDecompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZipUtil.decompress(null);
        });
    }

    @Test
    public void testCompressEqualDecompress() {
        byte[] compress = ZipUtil.compress("aa".getBytes());
        byte[] decompress = ZipUtil.decompress(compress);
        Assertions.assertEquals("aa", new String(decompress));
    }

    @Test
    public void testCompressEmptyArray() {
        byte[] empty = new byte[0];
        byte[] compressed = ZipUtil.compress(empty);
        byte[] decompressed = ZipUtil.decompress(compressed);
        Assertions.assertArrayEquals(empty, decompressed);
    }

    @Test
    public void testCompressSingleByte() {
        byte[] singleByte = {42};
        byte[] compressed = ZipUtil.compress(singleByte);
        byte[] decompressed = ZipUtil.decompress(compressed);
        Assertions.assertArrayEquals(singleByte, decompressed);
    }

    @Test
    public void testCompressLargeData() {
        byte[] largeData = new byte[1024 * 1024];
        new Random().nextBytes(largeData);
        byte[] compressed = ZipUtil.compress(largeData);
        byte[] decompressed = ZipUtil.decompress(compressed);
        Assertions.assertArrayEquals(largeData, decompressed);
    }

    @Test
    public void testCompressTextCata() {
        String text = "The quick brown fox jumps over the lazy dog";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = ZipUtil.compress(original);
        byte[] decompressed = ZipUtil.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressUnicodeData() {
        String text = "测试中文数据压缩解压";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = ZipUtil.compress(original);
        byte[] decompressed = ZipUtil.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressionRatio() {
        String repeatedText = new String(new char[1000]).replace("\0", "a");
        byte[] original = repeatedText.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = ZipUtil.compress(original);
        Assertions.assertTrue(
                compressed.length < original.length,
                "Compressed size should be smaller than original for repetitive data");
    }

    @Test
    public void testDecompressInvalidData() {
        byte[] invalidData = {0x1, 0x2, 0x3, 0x4, 0x5};
        byte[] result = ZipUtil.decompress(invalidData);
        Assertions.assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testDecompressEmptyArray() {
        byte[] result = ZipUtil.decompress(new byte[0]);
        Assertions.assertArrayEquals(new byte[0], result);
    }

    @Test
    public void testMultipleCompressionCycles() {
        String text = "Test multiple compression cycles";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        byte[] firstCompressed = ZipUtil.compress(original);
        byte[] firstDecompressed = ZipUtil.decompress(firstCompressed);

        byte[] secondCompressed = ZipUtil.compress(firstDecompressed);
        byte[] secondDecompressed = ZipUtil.decompress(secondCompressed);

        Assertions.assertEquals(text, new String(secondDecompressed, StandardCharsets.UTF_8));
    }
}
