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
package org.apache.seata.common.util;

import java.sql.Blob;

import javax.sql.rowset.serial.SerialBlob;

import org.apache.seata.common.Constants;
import org.apache.seata.common.exception.ShouldNeverHappenException;

/**
 * The type Blob utils.
 *
 */
public class BlobUtils {

    private BlobUtils() {

    }

    /**
     * String 2 blob blob.
     *
     * @param str the str
     * @return the blob
     */
    public static Blob string2blob(String str) {
        if (str == null) {
            return null;
        }

        try {
            return new SerialBlob(str.getBytes(Constants.DEFAULT_CHARSET));
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Blob 2 string string.
     *
     * @param blob the blob
     * @return the string
     */
    public static String blob2string(Blob blob) {
        if (blob == null) {
            return null;
        }

        try {
            return new String(blob.getBytes(1, (int) blob.length()), Constants.DEFAULT_CHARSET);
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Byte array to blob
     *
     * @param bytes the byte array
     * @return the blob
     */
    public static Blob bytes2Blob(byte[] bytes) {
        if (bytes == null) {
            return null;
        }

        try {
            return new SerialBlob(bytes);
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }

    /**
     * Blob to byte array.
     *
     * @param blob the blob
     * @return the byte array
     */
    public static byte[] blob2Bytes(Blob blob) {
        if (blob == null) {
            return null;
        }

        try {
            return blob.getBytes(1, (int) blob.length());
        } catch (Exception e) {
            throw new ShouldNeverHappenException(e);
        }
    }
}
