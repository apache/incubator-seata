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
package io.seata.compressor.zstd;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * the Zstd Compressor test
 *
 * @author chd
 */
public class ZstdCompressorTest {

    @Test
    public void testCompressAndDecompress() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100000; i++) {
            sb.append(UUID.randomUUID().toString().replace("-", ""));
        }

        byte[] bytes = sb.toString().getBytes(StandardCharsets.UTF_8);

        ZstdCompressor compressor = new ZstdCompressor();
        long start = 0;
        for (int i = 0; i < 1010; i ++) {
            if (i == 10) {
                start = System.currentTimeMillis();
            }

            bytes = compressor.compress(bytes);
            bytes = compressor.decompress(bytes);
        }
        System.out.println("bytes size=" + bytes.length + "; usage=" + (System.currentTimeMillis() - start));
        bytes = compressor.compress(bytes);
        System.out.println("compressed size=" + bytes.length);
    }
}
