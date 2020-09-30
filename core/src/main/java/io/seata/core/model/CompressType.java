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
package io.seata.core.model;

import java.util.HashMap;
import java.util.Map;

/**
 * @author chd
 */
public enum CompressType {
    /**
     * the compress type none
     */
    NONE(""),
    /**
     * the compress type gzip
     */
    GZIP("gzip"),
    /**
     * the compress type zip
     */
    ZIP("zip"),
    /**
     * the compress type bzip2
     */
    BZIP2("bzip2"),
    /**
     * the compress type lz4
     */
    LZ4("lz4"),
    /**
     * the compress type 7z
     */
    SEVEN_Z("7z");

    private String type;

    CompressType(String type) {
        this.type = type;
    }

    /**
     * Gets type.
     *
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * mostly, the active CompressType is 2 in a rm
     * NONE and one of other type
     */
    private static final Map<String, CompressType> MAP = new HashMap<>(2, 1.01f);

    /**
     * Get compress type using foreach.
     *
     * @param type the type to lower
     * @return the compress type
     */
    private static CompressType getByType(String type) {
        for (CompressType compressType : values()) {
            if (compressType.type.equals(type)) {
                return compressType;
            }
        }

        return null;
    }

    /**
     * Get compress type.
     *
     * @param type the type
     * @return the compress type
     */
    public static CompressType get(String type) {
        String type2Lower = type.toLowerCase();

        CompressType compressType = MAP.get(type2Lower);

        if (null == compressType) {
            compressType = getByType(type2Lower);
            if (null == compressType) {
                throw new IllegalArgumentException("Unknown compress type [" + type + "]");
            } else {
                MAP.put(type2Lower, compressType);
            }
        }

        return compressType;
    }
}
