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
package io.seata.compressor.deflater;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

/**
 * @author dongzl
 */
public class DeflaterUtil {

    private DeflaterUtil() {

    }

    private static final int BUFFER_SIZE = 8192;

    public static byte[] compress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        int lenght = 0;
        Deflater deflater = new Deflater();
        deflater.setInput(bytes);
        deflater.finish();
        byte[] outputBytes = new byte[BUFFER_SIZE];
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            while (!deflater.finished()) {
                lenght = deflater.deflate(outputBytes);
                bos.write(outputBytes, 0, lenght);
            }
            deflater.end();
            return bos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Deflater compress error", e);
        }
    }

    public static byte[] decompress(byte[] bytes) {
        if (bytes == null) {
            throw new NullPointerException("bytes is null");
        }
        int length = 0;
        Inflater inflater = new Inflater();
        inflater.setInput(bytes);
        byte[] outputBytes = new byte[BUFFER_SIZE];
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();) {
            while (!inflater.finished()) {
                length = inflater.inflate(outputBytes);
                if (length == 0) {
                    break;
                }
                bos.write(outputBytes, 0, length);
            }
            inflater.end();
            return bos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Deflater decompress error", e);
        }
    }

}
