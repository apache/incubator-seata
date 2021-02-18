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
 * @author chd
 */
public class SizeUtil {
    private static final long RADIX = 1024;
    /**
     * case size to byte length
     * example:
     *   2k => 2 * 1024
     *   2m => 2 * 1024 * 1024
     *   2g => 2 * 1024 * 1024 * 1024
     *   2t => 2 * 1024 * 1024 * 1024 * 1024
     * @param size the string size with unit
     * @return the byte length
     */
    public static long size2Long(String size) {
        if (null == size || size.length() <= 1) {
            throw new IllegalArgumentException("could not convert '" + size + "' to byte length");
        }

        String size2Lower = size.toLowerCase();
        char unit = size2Lower.charAt(size.length() - 1);
        long number;
        try {
            number = NumberUtils.toLong(size2Lower.substring(0, size.length() - 1));
        } catch (NumberFormatException | NullPointerException ex) {
            throw new IllegalArgumentException("could not convert '" + size + "' to byte length");
        }

        switch (unit) {
            case 'k':
                return number * RADIX;
            case 'm':
                return number * RADIX * RADIX;
            case 'g':
                return number * RADIX * RADIX * RADIX;
            case 't':
                return number * RADIX * RADIX * RADIX * RADIX;
            default:
                throw new IllegalArgumentException("could not convert '" + size + "' to byte length");
        }
    }
}
