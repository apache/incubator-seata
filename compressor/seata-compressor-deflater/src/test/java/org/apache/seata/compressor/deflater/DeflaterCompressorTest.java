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
package org.apache.seata.compressor.deflater;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Random;

public class DeflaterCompressorTest {

    @Test
    public void testCompressAndDecompress() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        byte[] bytes = "seata".getBytes();
        bytes = compressor.compress(bytes);
        bytes = compressor.decompress(bytes);
        Assertions.assertEquals(new String(bytes), "seata");
    }

    @Test
    public void testCompressNull() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        Assertions.assertThrows(NullPointerException.class, () -> {
            compressor.compress(null);
        });
    }

    @Test
    public void testDecompressNull() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        Assertions.assertThrows(NullPointerException.class, () -> {
            compressor.decompress(null);
        });
    }

    @Test
    public void testCompressEmptyArray() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        byte[] empty = new byte[0];
        byte[] compressed = compressor.compress(empty);
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertArrayEquals(empty, decompressed);
    }

    @Test
    public void testCompressSingleByte() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        byte[] singleByte = {42};
        byte[] compressed = compressor.compress(singleByte);
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertArrayEquals(singleByte, decompressed);
    }

    @Test
    public void testCompressLargeData() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        byte[] largeData = new byte[1024 * 1024];
        new Random().nextBytes(largeData);
        byte[] compressed = compressor.compress(largeData);
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertArrayEquals(largeData, decompressed);
    }

    @Test
    public void testCompressTextData() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        String text = "The quick brown fox jumps over the lazy dog";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(original);
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressSpecialCharacters() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        String text = "Hello World! @#$%^&*()_+-=[]{}|;':\",./<>?";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(original);
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressUnicodeData() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        String text = "Hello World in Chinese: 你好世界, Japanese: こんにちは世界, Korean: 안녕하세요 세계";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(original);
        byte[] decompressed = compressor.decompress(compressed);
        Assertions.assertEquals(text, new String(decompressed, StandardCharsets.UTF_8));
    }

    @Test
    public void testCompressionRatio() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        String repeatedText = new String(new char[1000]).replace("\0", "a");
        byte[] original = repeatedText.getBytes(StandardCharsets.UTF_8);
        byte[] compressed = compressor.compress(original);
        Assertions.assertTrue(
                compressed.length < original.length,
                "Compressed size should be smaller than original for repetitive data");
    }

    @Test
    public void testDecompressInvalidData() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        byte[] invalidData = {0x1, 0x2, 0x3, 0x4};
        Assertions.assertThrows(RuntimeException.class, () -> {
            compressor.decompress(invalidData);
        });
    }

    @Test
    public void testMultipleCompressionCycles() {
        DeflaterCompressor compressor = new DeflaterCompressor();
        String text = "Test multiple compression cycles";
        byte[] original = text.getBytes(StandardCharsets.UTF_8);

        byte[] firstCompressed = compressor.compress(original);
        byte[] firstDecompressed = compressor.decompress(firstCompressed);

        byte[] secondCompressed = compressor.compress(firstDecompressed);
        byte[] secondDecompressed = compressor.decompress(secondCompressed);

        Assertions.assertEquals(text, new String(secondDecompressed, StandardCharsets.UTF_8));
    }
}
