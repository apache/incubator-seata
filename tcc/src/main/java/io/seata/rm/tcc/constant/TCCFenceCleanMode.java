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
package io.seata.rm.tcc.constant;

import io.seata.common.util.StringUtils;

/**
 * TCC Fence clean mode
 *
 * @author kaka2code
 */
public enum TCCFenceCleanMode {

    /**
     * Close auto clean task
     */
    Close,
    /**
     * Clean by days
     */
    Day,
    /**
     * Clean by hours
     */
    Hour,
    /**
     * Clean by minutes
     */
    Minute;

    /**
     * Valueof TCC Fence Clean Mode
     *
     * @param cleanModel the Clean Mode
     * @return the Clean Mode
     */
    public static TCCFenceCleanMode valueof(String cleanModel) {
        for (TCCFenceCleanMode mode : values()) {
            if (StringUtils.equalsIgnoreCase(mode.name(), cleanModel)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown TCC Fence Clean Mode:" + cleanModel);
    }
}
