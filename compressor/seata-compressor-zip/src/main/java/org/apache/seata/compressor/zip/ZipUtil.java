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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * the Zip Util
 *
 */
public class ZipUtil {

    private static final int BUFFER_SIZE = 8192;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            ZipEntry entry = new ZipEntry("zip");
            entry.setSize(bytes.length);
            zip.putNextEntry(entry);
            zip.write(bytes);
            zip.closeEntry();
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Zip compress error", e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(bytes))) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (zip.getNextEntry() != null) {
                int n;
                while ((n = zip.read(buffer)) > -1) {
                    out.write(buffer, 0, n);
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Zip decompress error", e);
        }
    }
}
