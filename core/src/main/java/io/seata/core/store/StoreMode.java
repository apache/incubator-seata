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
package io.seata.core.store;

/**
 * transaction log store mode
 *
 * @author zhangsen
 * @data 2019 /4/2
 */
public enum StoreMode {

    /**
     * file store
     */
    FILE,

    /**
     * database store
     */
    DB;

    /**
     * Valueof store mode.
     *
     * @param mode the mode
     * @return the store mode
     */
    public static StoreMode valueof(String mode) {
        for (StoreMode sm : values()) {
            if (sm.name().equalsIgnoreCase(mode)) {
                return sm;
            }
        }
        throw new IllegalArgumentException("unknown store mode:" + mode);
    }

}
