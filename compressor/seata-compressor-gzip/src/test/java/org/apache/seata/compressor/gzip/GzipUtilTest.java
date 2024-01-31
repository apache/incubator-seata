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

import java.util.zip.GZIPInputStream;


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

}
