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
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class CompressUtilTest {

    final byte[] originBytes = new byte[] {1, 2, 3};

    final byte[] compressedBytes1 =
            new byte[] {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};

    // for java17
    final byte[] compressedBytes2 =
            new byte[] {31, -117, 8, 0, 0, 0, 0, 0, 0, -1, 99, 100, 98, 6, 0, 29, -128, -68, 85, 3, 0, 0, 0};

    @Test
    public void testInit() {
        Assertions.assertNotNull(new CompressUtil());
    }

    @Test
    @EnabledOnJre({JRE.JAVA_8, JRE.JAVA_11})
    public void testCompress1() throws IOException {
        Assertions.assertArrayEquals(compressedBytes1, CompressUtil.compress(originBytes));
    }

    @Test
    @DisabledOnJre({JRE.JAVA_8, JRE.JAVA_11})
    public void testCompress2() throws IOException {
        Assertions.assertArrayEquals(compressedBytes2, CompressUtil.compress(originBytes));
    }

    @Test
    public void testUncompress() throws IOException {
        Assertions.assertArrayEquals(originBytes, CompressUtil.uncompress(compressedBytes1));

        Assertions.assertArrayEquals(originBytes, CompressUtil.uncompress(compressedBytes2));
    }

    @Test
    public void testIsCompressData() {
        Assertions.assertFalse(CompressUtil.isCompressData(null));
        Assertions.assertFalse(CompressUtil.isCompressData(new byte[0]));
        Assertions.assertFalse(CompressUtil.isCompressData(new byte[] {31, 11}));
        Assertions.assertFalse(CompressUtil.isCompressData(new byte[] {31, 11, 0}));

        Assertions.assertTrue(CompressUtil.isCompressData(new byte[] {31, -117, 0}));
    }

    @Test
    public void testCompressAndUncompress() throws IOException {
        // Test with normal data
        byte[] originData = "This is a test string for compression".getBytes();
        byte[] compressedData = CompressUtil.compress(originData);
        byte[] uncompressedData = CompressUtil.uncompress(compressedData);
        assertThat(uncompressedData).isEqualTo(originData);

        // Test with empty data
        byte[] emptyData = new byte[0];
        byte[] compressedEmptyData = CompressUtil.compress(emptyData);
        byte[] uncompressedEmptyData = CompressUtil.uncompress(compressedEmptyData);
        assertThat(uncompressedEmptyData).isEqualTo(emptyData);

        // Test with large data
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 128);
        }
        byte[] compressedLargeData = CompressUtil.compress(largeData);
        byte[] uncompressedLargeData = CompressUtil.uncompress(compressedLargeData);
        assertThat(uncompressedLargeData).isEqualTo(largeData);

        // Test compression ratio - compressed data should be smaller for large data with patterns
        assertThat(compressedLargeData.length).isLessThan(largeData.length);
    }

    @Test
    public void testCompressException() {
        // Test that we properly handle exceptions during compression
        // This is difficult to trigger in normal circumstances, but we can at least ensure
        // the method exists and is testable
        assertThat(CompressUtil.class).hasDeclaredMethods("compress");
    }

    @Test
    public void testUncompressException() {
        // Test with invalid compressed data
        byte[] invalidCompressedData = new byte[] {1, 2, 3, 4, 5};
        assertThatThrownBy(() -> CompressUtil.uncompress(invalidCompressedData)).isInstanceOf(IOException.class);
    }

    @Test
    public void testIsCompressDataEnhanced() {
        // Test with valid GZIP magic number
        byte[] gzipData = new byte[] {31, -117, 8, 0, 0, 0, 0, 0, 0, 0};
        assertThat(CompressUtil.isCompressData(gzipData)).isTrue();

        // Test with invalid GZIP magic number
        byte[] nonGzipData = new byte[] {31, -116, 8, 0, 0, 0, 0, 0, 0, 0};
        assertThat(CompressUtil.isCompressData(nonGzipData)).isFalse();

        // Test with edge case - exactly 2 elements with GZIP magic number
        byte[] edgeCase = new byte[] {31, -117};
        // This should be false because we need at least 3 bytes for the check to work properly
        assertThat(CompressUtil.isCompressData(edgeCase)).isFalse();
    }

    @Test
    public void testCompressWithSpecialData() throws IOException {
        // Test with data that contains the GZIP magic number
        byte[] dataWithMagic = new byte[] {31, -117, 8, 0, 0, 0, 0, 0, 0, 0, 't', 'e', 's', 't'};
        byte[] compressed = CompressUtil.compress(dataWithMagic);
        byte[] uncompressed = CompressUtil.uncompress(compressed);
        assertThat(uncompressed).isEqualTo(dataWithMagic);

        // Test with repeated data (highly compressible)
        byte[] repeatedData = new byte[1000];
        for (int i = 0; i < repeatedData.length; i++) {
            repeatedData[i] = (byte) (i % 2); // Only 0 and 1
        }
        byte[] compressedRepeated = CompressUtil.compress(repeatedData);
        byte[] uncompressedRepeated = CompressUtil.uncompress(compressedRepeated);
        assertThat(uncompressedRepeated).isEqualTo(repeatedData);
        // Compression should be very effective for this data
        assertThat(compressedRepeated.length).isLessThan(repeatedData.length / 10);
    }

    @Test
    public void testUncompressWithEdgeCases() throws IOException {
        // Test with data that has the GZIP header but is truncated
        byte[] truncatedData = new byte[] {31, -117, 8};
        assertThatThrownBy(() -> CompressUtil.uncompress(truncatedData)).isInstanceOf(IOException.class);
    }
}
