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

import java.util.ArrayList;
import java.util.List;

import com.github.luben.zstd.Zstd;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.apache.seata.compressor.zstd.ZstdUtil.MAX_COMPRESSED_SIZE;

/**
 * the Zstd Util test
 */
public class ZstdUtilTest {

    @Test
    public void test_compress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZstdUtil.compress(null);
        });
    }

    @Test
    public void test_decompress() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            ZstdUtil.decompress(null);
        });
    }

    @Test
    public void test_decompress_with_len_illegal() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            //https://github.com/facebook/zstd/blob/dev/doc/zstd_compression_format.md#zstandard-frames
            List<Byte> bytes = new ArrayList<>();
            byte[] magic = new byte[] {(byte)0x28, (byte)0xB5, (byte)0x2F, (byte)0xFD};
            byte[] frameHeaderDescriptor = new byte[magic.length + 1];
            System.arraycopy(magic, 0, frameHeaderDescriptor, 0, magic.length);
            frameHeaderDescriptor[magic.length] = (byte)0xA0;
            //4*1024*1024 + 1
            byte[] frameContentSize = new byte[] {(byte)0x00, (byte)0x40, (byte)0x00, (byte)0x01};
            byte[] frameContent = new byte[frameHeaderDescriptor.length + frameContentSize.length];
            System.arraycopy(frameHeaderDescriptor, 0, frameContent, 0, frameHeaderDescriptor.length);
            System.arraycopy(frameContentSize, 0, frameContent, frameHeaderDescriptor.length, frameContentSize.length);
            ZstdUtil.decompress(frameContent);

        });
    }

    @Test
    public void test_decompress_with_len() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            byte[] data = new byte[MAX_COMPRESSED_SIZE + 1];
            for (int i = 0; i < data.length; i++) {
                data[i] = (byte)('A' + i % 26);
            }
            byte[] compressedData = Zstd.compress(data);
            ZstdUtil.decompress(compressedData);
        });
        int len = MAX_COMPRESSED_SIZE / 2;
        byte[] data = new byte[len];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)('A' + i % 26);
        }
        byte[] compressedData = Zstd.compress(data);
        byte[] decompressedData = ZstdUtil.decompress(compressedData);
        Assertions.assertEquals(len, decompressedData.length);
    }
}
