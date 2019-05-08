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
package io.seata.common.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 *
 * @author lizhao
 */
public class CompressUtil {

    /**
     * compress bytes
     * @param src
     * @return
     * @throws IOException
     */
    public static byte[] compress(final byte[] src) throws IOException {
        byte[] result;
        ByteArrayOutputStream bos = new ByteArrayOutputStream(src.length);
        GZIPOutputStream gos = new GZIPOutputStream(bos);
        try {
            gos.write(src);
            gos.finish();
            result = bos.toByteArray();
        } finally {
            bos.close();
            gos.close();
        }
        return result;
    }

    /**
     * uncompress bytes
     * @param src
     * @return
     * @throws IOException
     */
    public static byte[] uncompress(final byte[] src) throws IOException {
        byte[] result;
        byte[] uncompressData = new byte[src.length];
        ByteArrayInputStream bis = new ByteArrayInputStream(src);
        GZIPInputStream iis = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream(src.length);

        try {
            while (true) {
                int len = iis.read(uncompressData, 0, uncompressData.length);
                if (len <= 0) {
                    break;
                }
                bos.write(uncompressData, 0, len);
            }
            bos.flush();
            result = bos.toByteArray();
        } finally {
            bis.close();
            iis.close();
            bos.close();
        }
        return result;
    }

    /**
     * check magic
     * @param bytes
     * @return
     */
    public static boolean isCompressData(byte[] bytes) {
        if (bytes != null && bytes.length > 2) {
            int header = ((bytes[0] & 0xff)) | (bytes[1] & 0xff) << 8;
            return GZIPInputStream.GZIP_MAGIC == header;
        }
        return false;
    }
}