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
package io.seata.compressor.sevenz;

import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.archivers.sevenz.SevenZOutputFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * the SevenZ Util
 *
 * @author ph3636
 */
public class SevenZUtil {

    private static final int BUFFER_SIZE = 8192;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel();
        try (SevenZOutputFile z7z = new SevenZOutputFile(channel)) {
            SevenZArchiveEntry entry = new SevenZArchiveEntry();
            entry.setName("sevenZip");
            entry.setSize(bytes.length);
            z7z.putArchiveEntry(entry);
            z7z.write(bytes);
            z7z.closeArchiveEntry();
            z7z.finish();
            return channel.array();
        } catch (IOException e) {
            throw new RuntimeException("SevenZ compress error", e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        SeekableInMemoryByteChannel channel = new SeekableInMemoryByteChannel(bytes);
        try (SevenZFile sevenZFile = new SevenZFile(channel)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            while (sevenZFile.getNextEntry() != null) {
                int n;
                while ((n = sevenZFile.read(buffer)) > -1) {
                    out.write(buffer, 0, n);
                }
            }
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("SevenZ decompress error", e);
        }
    }
}
