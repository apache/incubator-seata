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
import com.github.luben.zstd.ZstdInputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * the Zstd Util
 *
 */
public class ZstdUtil {

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
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                ZstdInputStream zis = new ZstdInputStream(bais);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int len;
            while ((len = zis.read(buffer)) > 0) {
                baos.write(buffer, 0, len);
            }
            return baos.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to decompress zstd data", e);
        }
    }
}
