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
package org.apache.seata.compressor.bzip2;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.apache.tools.bzip2.CBZip2InputStream;
import org.apache.tools.bzip2.CBZip2OutputStream;

/**
 * the BZip2 Util
 *
 */
public class BZip2Util {

    private static final int BUFFER_SIZE = 8192;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (CBZip2OutputStream bzip2 = new CBZip2OutputStream(bos)) {
            bzip2.write(bytes);
            bzip2.finish();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("BZip2 compress error", e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (CBZip2InputStream bzip2 = new CBZip2InputStream(bis)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while ((n = bzip2.read(buffer)) > -1) {
                out.write(buffer, 0, n);
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("BZip2 decompress error", e);
        }
    }
}
