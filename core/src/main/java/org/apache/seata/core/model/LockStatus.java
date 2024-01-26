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
package org.apache.seata.core.model;


import org.apache.seata.common.exception.ShouldNeverHappenException;

/**
 * Status of lock.
 *
 */
public enum LockStatus {

    /**
     * The Locked.
     * description: Locked
     */
    Locked(0),

    /**
     * The Rollbacking.
     * description: Rollbacking
     */
    Rollbacking(1);

    private final int code;

    LockStatus(int code) {
        this.code = code;
    }

    /**
     * Get lock status.
     *
     * @param code the code
     * @return the lock status
     */
    public static LockStatus get(byte code) {
        return get((int)code);
    }

    /**
     * Get lock status.
     *
     * @param code the code
     * @return the lock status
     */
    public static LockStatus get(int code) {
        LockStatus value;
        try {
            value = LockStatus.values()[code];
        } catch (Exception e) {
            throw new ShouldNeverHappenException("Unknown LockStatus[" + code + "]");
        }
        return value;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

}
