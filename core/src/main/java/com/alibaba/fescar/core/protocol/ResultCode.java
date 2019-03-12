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
package com.alibaba.fescar.core.protocol;

/**
 * The enum Result code.
 */
public enum ResultCode {

    /**
     * Failed result code.
     */
    // Failed
    Failed,

    /**
     * Success result code.
     */
    // Success
    Success;

    /**
     * Get result code.
     *
     * @param ordinal the ordinal
     * @return the result code
     */
    public static ResultCode get(byte ordinal) {
        return get((int)ordinal);
    }

    /**
     * Get result code.
     *
     * @param ordinal the ordinal
     * @return the result code
     */
    public static ResultCode get(int ordinal) {
        for (ResultCode resultCode : ResultCode.values()) {
            if (resultCode.ordinal() == ordinal) {
                return resultCode;
            }
        }
        throw new IllegalArgumentException("Unknown ResultCode[" + ordinal + "]");
    }
}
