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
package org.apache.seata.core.compressor;


public enum CompressorType {

    /**
     * Not compress
     */
    NONE((byte) 0),

    /**
     * The gzip.
     */
    GZIP((byte) 1),

    /**
     * The zip.
     */
    ZIP((byte) 2),

    /**
     * The sevenz.
     */
    SEVENZ((byte) 3),

    /**
     * The bzip2.
     */
    BZIP2((byte) 4),

    /**
     * The lz4.
     */
    LZ4((byte) 5),

    /**
     * The deflater.
     */
    DEFLATER((byte) 6),

    /**
     * The zstd.
     */
    ZSTD((byte) 7);

    private final byte code;

    CompressorType(final byte code) {
        this.code = code;
    }

    /**
     * Gets result code.
     *
     * @param code the code
     * @return the result code
     */
    public static CompressorType getByCode(int code) {
        for (CompressorType b : CompressorType.values()) {
            if (code == b.code) {
                return b;
            }
        }
        throw new IllegalArgumentException("unknown codec:" + code);
    }

    /**
     * Gets result code.
     *
     * @param name the code
     * @return the result code
     */
    public static CompressorType getByName(String name) {
        for (CompressorType b : CompressorType.values()) {
            if (b.name().equalsIgnoreCase(name)) {
                return b;
            }
        }
        throw new IllegalArgumentException("unknown codec:" + name);
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public byte getCode() {
        return code;
    }
}
