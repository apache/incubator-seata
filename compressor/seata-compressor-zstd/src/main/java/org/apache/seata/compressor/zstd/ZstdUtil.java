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

/**
 * the Zstd Util
 *
 */
public class ZstdUtil {

    public static final int MAX_COMPRESSED_SIZE = 4 * 1024 * 1024;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }

        return Zstd.compress(bytes);
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }

        long size = Zstd.decompressedSize(bytes);
        if (size < 0 || size > MAX_COMPRESSED_SIZE) {
            throw new IllegalArgumentException(
                "Invalid decompressed size: " + size + ", the value of size ranges from 0 to " + MAX_COMPRESSED_SIZE);
        }
        byte[] decompressBytes = new byte[(int)size];
        Zstd.decompress(decompressBytes, bytes);
        return decompressBytes;
    }
}
