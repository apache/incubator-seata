/*
 *  Copyright 1999-2018 Alibaba Group Holding Ltd.
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

package com.alibaba.fescar.common.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Blob;
import java.sql.SQLException;

import javax.sql.rowset.serial.SerialBlob;

/**
 * The type String utils.
 */
public class StringUtils {

    private StringUtils() {

    }

    /**
     * Is empty boolean.
     *
     * @param str the str
     * @return the boolean
     */
    public static final boolean isEmpty(String str) {
        return (str == null) || (str.isEmpty());
    }

    /**
     * String 2 blob blob.
     *
     * @param str the str
     * @return the blob
     * @throws SQLException the sql exception
     */
    public static Blob string2blob(String str) throws SQLException {
        if (str == null) {
            return null;
        }
        return new SerialBlob(str.getBytes());
    }

    /**
     * Blob 2 string string.
     *
     * @param blob the blob
     * @return the string
     * @throws SQLException the sql exception
     */
    public static String blob2string(Blob blob) throws SQLException {
        if (blob == null) {
            return null;
        }

        return new String(blob.getBytes((long) 1, (int) blob.length()));
    }

    /**
     * Input stream 2 string string.
     *
     * @param is the is
     * @return the string
     * @throws IOException the io exception
     */
    public static String inputStream2String(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        int i = -1;
        while ((i = is.read()) != -1) {
            baos.write(i);
        }
        return baos.toString();
    }
}
