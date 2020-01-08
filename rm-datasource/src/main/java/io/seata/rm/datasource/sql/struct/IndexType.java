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
package io.seata.rm.datasource.sql.struct;

/**
 * The enum Index type.
 *
 * @author sharajava
 */
public enum IndexType {
    /**
     * Primary index type.
     */
    PRIMARY(0),
    /**
     * Normal index type.
     */
    NORMAL(1),
    /**
     * Unique index type.
     */
    UNIQUE(2),
    /**
     * Full text index type.
     */
    FULL_TEXT(3);

    private int i;

    IndexType(int i) {
        this.i = i;
    }

    /**
     * Value int.
     *
     * @return the int
     */
    public int value() {
        return this.i;
    }

    /**
     * Value of index type.
     *
     * @param i the
     * @return the index type
     */
    public static IndexType valueOf(int i) {
        for (IndexType t : values()) {
            if (t.value() == i) {
                return t;
            }
        }
        throw new IllegalArgumentException("Invalid IndexType:" + i);
    }
}