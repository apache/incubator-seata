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
package org.apache.seata.compressor.zstd;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * the Zstd Util test
 */
public class ZstdUtilTest {

    private final int MAX_COMPRESSED_SIZE = 4 * 1024 * 1024; // 4MB

    @Test
    public void testCompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZstdUtil.compress(null);
        });
    }

    @Test
    public void testDecompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZstdUtil.decompress(null);
        });
    }

    @Test
    public void testCompressEqualDecompress() {
        byte[] compress = ZstdUtil.compress("aa".getBytes());
        byte[] decompress = ZstdUtil.decompress(compress);
        Assertions.assertEquals("aa", new String(decompress));
    }

    @Test
    public void testDecompressWithLenIllegal() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            // https://github.com/facebook/zstd/blob/dev/doc/zstd_compression_format.md#zstandard-frames
            List<Byte> bytes = new ArrayList<>();
            byte[] magic = new byte[] {(byte) 0x28, (byte) 0xB5, (byte) 0x2F, (byte) 0xFD};
            byte[] frameHeaderDescriptor = new byte[magic.length + 1];
            System.arraycopy(magic, 0, frameHeaderDescriptor, 0, magic.length);
            frameHeaderDescriptor[magic.length] = (byte) 0xA0;
            // 4*1024*1024 + 1
            byte[] frameContentSize = new byte[] {(byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x01};
            byte[] frameContent = new byte[frameHeaderDescriptor.length + frameContentSize.length];
            System.arraycopy(frameHeaderDescriptor, 0, frameContent, 0, frameHeaderDescriptor.length);
            System.arraycopy(frameContentSize, 0, frameContent, frameHeaderDescriptor.length, frameContentSize.length);
            ZstdUtil.decompress(frameContent);
        });
    }

    @Test
    public void testDecompressWithLen() {
        Assertions.assertDoesNotThrow(() -> {
            byte[] data = new byte[MAX_COMPRESSED_SIZE + 1];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte) ('A' + i % 26);
            }
            byte[] compressedData = Zstd.compress(data);
            ZstdUtil.decompress(compressedData);
        });
        int len = MAX_COMPRESSED_SIZE / 2;
        byte[] data = new byte[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) ('A' + i % 26);
        }
        byte[] compressedData = Zstd.compress(data);
        byte[] decompressedData = ZstdUtil.decompress(compressedData);
        Assertions.assertEquals(len, decompressedData.length);
    }

    @Test
    public void testDecompressWithFakeFrameContentSizeOOM() {
        // Construct a fake zstd header with the frame content size set to 1GB, while the actual content is only 4MB.
        byte[] magic = new byte[] {(byte) 0x28, (byte) 0xB5, (byte) 0x2F, (byte) 0xFD};
        byte[] frameHeaderDescriptor = new byte[magic.length + 1];
        System.arraycopy(magic, 0, frameHeaderDescriptor, 0, magic.length);
        frameHeaderDescriptor[magic.length] = (byte) 0xA0;
        // frame content size: 1GB = 0x40000000
        byte[] frameContentSize = new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x40};
        // The actual content is only 4MB.
        byte[] fakeContent = new byte[4 * 1024 * 1024];
        for (int i = 0; i < fakeContent.length; i++) {
            fakeContent[i] = (byte) ('A' + i % 26);
        }
        byte[] frameContent = new byte[frameHeaderDescriptor.length + frameContentSize.length + fakeContent.length];
        System.arraycopy(frameHeaderDescriptor, 0, frameContent, 0, frameHeaderDescriptor.length);
        System.arraycopy(frameContentSize, 0, frameContent, frameHeaderDescriptor.length, frameContentSize.length);
        System.arraycopy(
                fakeContent,
                0,
                frameContent,
                frameHeaderDescriptor.length + frameContentSize.length,
                fakeContent.length);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ZstdUtil.decompress(frameContent));
        Assertions.assertTrue(Zstd.decompressedSize(frameContent) > MAX_COMPRESSED_SIZE);
    }
}
