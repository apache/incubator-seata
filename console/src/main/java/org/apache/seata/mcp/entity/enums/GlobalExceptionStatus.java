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
package org.apache.seata.mcp.entity.enums;

import java.util.ArrayList;
import java.util.List;

/**
 * Status of global transaction.
 *
 */
public enum GlobalExceptionStatus {

    /**
     * The Commit failed.
     */
    // Finally: failed to commit
    CommitFailed(10, "2Phase commit failed"),

    /**
     * The Rollback failed.
     */
    // Finally: failed to rollback
    RollbackFailed(12, "global transaction completed but rollback failed"),

    /**
     * The Timeout rollback failed.
     */
    // Finally: failed to rollback since timeout
    TimeoutRollbackFailed(14, "global transaction was rollbacking due to timeout, but failed");


    private final int code;
    private final String desc;

    GlobalExceptionStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * Gets code.
     *
     * @return the code
     */
    public int getCode() {
        return code;
    }

    /**
     * Get global status.
     *
     * @param code the code
     * @return the global status
     */
    public static GlobalExceptionStatus get(byte code) {
        return get((int) code);
    }

    /**
     * Get global status.
     *
     * @param code the code
     * @return the global status
     */
    public static GlobalExceptionStatus get(int code) {
        GlobalExceptionStatus value = null;
        try {
            value = GlobalExceptionStatus.values()[code];
        } catch (Exception e) {
            throw new IllegalArgumentException("Unknown GlobalStatus[" + code + "]");
        }
        return value;
    }

    /**
     * Get global exception status code
     * @return the exception status code
     */
    public static List<Integer> getAll() {
        GlobalExceptionStatus[] values = GlobalExceptionStatus.values();
        List<Integer> result = new ArrayList<>();
        for(GlobalExceptionStatus value : values) {
            result.add(value.getCode());
        }
        return result;
    }
}
