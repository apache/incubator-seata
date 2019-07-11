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

/**
 * @author Geng Zhang
 * @since 0.7.0
 */
public class CodecUtils {

    /**
     * 一个byte存两个4bit的信息
     *
     * @param b 原始byte
     * @return byte数组{<16,<16}
     */
    public static byte[] parseHigh4Low4Bytes(byte b) {
        return new byte[]{
                (byte) ((b >> 4)), // 右移4位，只取前4bit的值
                (byte) ((b & 0x0f)) // 只取后面4bit的值，前面两位补0
        };
    }

    /**
     * 一个byte存两个4bit的信息
     *
     * @param high4 高4位 <16
     * @param low4  低4位 <16
     * @return 一个byte存两个4bit的信息
     */
    public static byte buildHigh4Low4Bytes(byte high4, byte low4) {
        return (byte) ((high4 << 4) + low4);
    }
}
